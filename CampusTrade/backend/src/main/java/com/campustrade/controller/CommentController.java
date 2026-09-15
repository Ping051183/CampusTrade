package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.common.Result;
import com.campustrade.entity.Comment;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论Controller
 */
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "商品评论接口")
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取商品评论列表
     */
    @GetMapping("/list/{goodsId}")
    @Operation(summary = "获取商品评论列表")
    public Result<List<Comment>> getComments(@PathVariable Long goodsId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getGoodsId, goodsId)
                .orderByDesc(Comment::getCreateTime);
        return Result.success(commentService.list(wrapper));
    }

    /**
     * 添加评论
     */
    @PostMapping("/add")
    @Operation(summary = "添加评论")
    public Result<Void> addComment(@RequestBody Comment comment) {
        Long userId = SecurityUtil.getCurrentUserId();
        comment.setUserId(userId);
        commentService.addComment(comment);
        return Result.success("评论成功", null);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        commentService.deleteComment(id, userId);
        return Result.success("删除成功", null);
    }
}
