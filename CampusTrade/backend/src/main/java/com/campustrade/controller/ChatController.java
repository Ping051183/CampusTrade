package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.SendMessageDTO;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.ChatService;
import com.campustrade.vo.ChatMessageVO;
import com.campustrade.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 聊天Controller
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "聊天管理", description = "买家卖家聊天接口")
public class ChatController {

    private final ChatService chatService;

    @Value("${file.upload.path:${user.home}/campus-trade/uploads}")
    private String uploadPath;

    /**
     * 上传聊天图片
     */
    @PostMapping("/upload/image")
    @Operation(summary = "上传聊天图片")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR);
        }

        // 生成文件名
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 创建目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 返回访问URL
        String fileUrl = "/uploads/" + filename;
        return Result.success("上传成功", fileUrl);
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png") || lowerName.endsWith(".gif")
                || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public Result<ChatMessageVO> sendMessage(@Valid @RequestBody SendMessageDTO sendMessageDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        ChatMessageVO message = chatService.sendMessage(userId, sendMessageDTO);
        return Result.success("发送成功", message);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public Result<List<ConversationVO>> getConversations() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(chatService.getConversations(userId));
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/messages")
    @Operation(summary = "获取聊天记录")
    public Result<List<ChatMessageVO>> getMessages(
            @RequestParam Long otherUserId,
            @RequestParam(required = false) Long goodsId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(chatService.getMessages(userId, otherUserId, goodsId));
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read")
    @Operation(summary = "标记消息为已读")
    public Result<Void> markAsRead(
            @RequestParam Long senderId,
            @RequestParam(required = false) Long goodsId) {
        Long userId = SecurityUtil.getCurrentUserId();
        chatService.markAsRead(userId, senderId, goodsId);
        return Result.success();
    }

    /**
     * 获取未读消息总数
     * 未登录用户返回 0
     */
    @GetMapping("/unread/count")
    @Operation(summary = "获取未读消息数")
    public Result<Integer> getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.success(0);
        }
        return Result.success(chatService.getUnreadCount(userId));
    }
}
