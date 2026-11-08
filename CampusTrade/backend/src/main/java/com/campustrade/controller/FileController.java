package com.campustrade.controller;

import com.campustrade.common.FileTypeValidator;
import com.campustrade.common.Result;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传Controller
 * <p>
 * 上传校验分三层：大小 → 扩展名白名单 → 文件头魔数。
 * 前两层只能拦住手滑，第三层才拦得住"把 a.html 改名成 a.jpg"这种。
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件管理", description = "文件上传接口")
public class FileController {

    @Value("${file.upload.path:${user.home}/campus-trade/uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}")
    private long maxSize;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. 空文件与大小
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 2. 扩展名白名单
        String originalFilename = file.getOriginalFilename();
        String declaredExtension = extractExtension(originalFilename);
        if (!FileTypeValidator.IMAGE_EXTENSIONS.contains(FileTypeValidator.normalizeExtension(declaredExtension))) {
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR);
        }

        // 3. 文件头魔数：扩展名可以随便改，前 12 个字节改不了
        String detectedExtension = readAndDetect(file);
        if (!FileTypeValidator.matchesDeclared(declaredExtension, detectedExtension)) {
            log.warn("上传文件类型与扩展名不符：name={} declared={} detected={}",
                    originalFilename, declaredExtension, detectedExtension);
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR);
        }

        // 4. 落盘：文件名由服务端生成（UUID），不使用用户提交的文件名
        String filename = UUID.randomUUID().toString().replace("-", "")
                + "." + FileTypeValidator.normalizeExtension(detectedExtension);
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            log.error("文件落盘失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        String fileUrl = "/uploads/" + filename;
        return Result.success("上传成功", fileUrl);
    }

    /**
     * 读取文件头并识别真实图片类型
     */
    private String readAndDetect(MultipartFile file) {
        byte[] header = new byte[FileTypeValidator.HEADER_LENGTH];
        try (InputStream in = file.getInputStream()) {
            int read = 0;
            while (read < header.length) {
                int n = in.read(header, read, header.length - read);
                if (n == -1) {
                    break;
                }
                read += n;
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
        return FileTypeValidator.detectImageExtension(header);
    }

    /**
     * 提取扩展名（不含点），没有扩展名返回空串
     */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1);
    }
}