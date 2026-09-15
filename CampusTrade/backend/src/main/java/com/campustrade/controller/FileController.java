package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传Controller
 */
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件管理", description = "文件上传接口")
public class FileController {

    @Value("${file.upload.path:${user.home}/campus-trade/uploads}")
    private String uploadPath;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片")
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
}
