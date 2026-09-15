package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.*;
import com.campustrade.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员Controller
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员后台管理接口")
public class AdminController {

    private final UserService userService;
    private final GoodsService goodsService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final NoticeService noticeService;
    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final ReportMapper reportMapper;

    /**
     * 数据统计
     */
    @GetMapping("/dashboard")
    @Operation(summary = "数据统计")
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 用户统计
        dashboard.put("userCount", userMapper.countTotal());

        // 商品统计
        dashboard.put("goodsOnSale", goodsMapper.countOnSale());
        dashboard.put("goodsPending", goodsMapper.countPending());

        // 订单统计
        dashboard.put("orderToday", orderMapper.countToday());
        dashboard.put("amountToday", orderMapper.sumTodayAmount());
        dashboard.put("orderTotal", orderMapper.countTotal());
        dashboard.put("amountTotal", orderMapper.sumTotalAmount());

        return Result.success(dashboard);
    }

    // ========== 用户管理 ==========

    /**
     * 分页查询用户列表
     */
    @GetMapping("/user/list")
    @Operation(summary = "用户列表")
    public Result<PageResult<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getSchool, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(pageParam, wrapper);
        List<UserVO> voList = userPage.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(Collectors.toList());

        PageResult<UserVO> result = new PageResult<>();
        result.setTotal(userPage.getTotal());
        result.setPage(userPage.getCurrent());
        result.setPageSize(userPage.getSize());
        result.setPages(userPage.getPages());
        result.setRecords(voList);
        return Result.success(result);
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/user/status/{id}")
    @Operation(summary = "修改用户状态")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                          @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success("操作成功", null);
    }

    // ========== 商品管理 ==========

    /**
     * 分页查询所有商品（管理员）
     */
    @GetMapping("/goods/list")
    @Operation(summary = "商品列表（管理员）")
    public Result<PageResult<Goods>> getGoodsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Page<Goods> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        wrapper.orderByDesc(Goods::getCreateTime);
        return Result.success(PageResult.from(goodsMapper.selectPage(pageParam, wrapper)));
    }

    /**
     * 商品审核
     */
    @PutMapping("/goods/audit/{id}")
    @Operation(summary = "商品审核")
    public Result<Void> auditGoods(@PathVariable Long id,
                                    @RequestParam Integer status,
                                    @RequestParam(required = false) String rejectReason) {
        goodsService.auditGoods(id, status, rejectReason);
        return Result.success("审核完成", null);
    }

    /**
     * 删除商品（管理员）
     */
    @DeleteMapping("/goods/delete/{id}")
    @Operation(summary = "删除商品（管理员）")
    public Result<Void> deleteGoods(@PathVariable Long id) {
        goodsService.removeById(id);
        return Result.success("删除成功", null);
    }

    // ========== 举报管理 ==========

    /**
     * 分页查询举报列表
     */
    @GetMapping("/report/list")
    @Operation(summary = "举报列表")
    public Result<PageResult<Report>> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Page<Report> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);
        return Result.success(PageResult.from(reportMapper.selectPage(pageParam, wrapper)));
    }

    /**
     * 处理举报
     */
    @PutMapping("/report/handle/{id}")
    @Operation(summary = "处理举报")
    public Result<Void> handleReport(@PathVariable Long id,
                                      @RequestParam Integer status,
                                      @RequestParam String result) {
        Long handlerId = SecurityUtil.getCurrentUserId();
        reportService.handleReport(id, status, result, handlerId);
        return Result.success("处理完成", null);
    }

    // ========== 公告管理 ==========

    /**
     * 发布公告
     */
    @PostMapping("/notice/add")
    @Operation(summary = "发布公告")
    public Result<Void> addNotice(@RequestBody Notice notice) {
        noticeService.save(notice);
        return Result.success("发布成功", null);
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/notice/delete/{id}")
    @Operation(summary = "删除公告")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success("删除成功", null);
    }
}
