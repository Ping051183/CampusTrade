package com.campustrade.common;

import java.util.Set;

/**
 * 文件类型校验：不信任扩展名，按文件头魔数判断真实类型。
 * <p>
 * 只校验扩展名的话，把 a.html / a.jsp 改名为 a.jpg 就能上传成功，
 * 后续若有解析或 CDN 误判，就是路径上的 XSS / 任意文件落地风险。
 */
public final class FileTypeValidator {

    public static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    /** 读取用于识别的文件头长度 */
    public static final int HEADER_LENGTH = 12;

    private FileTypeValidator() {
    }

    /**
     * 按文件头魔数识别图片真实类型
     *
     * @param header 文件头字节（最少 12 字节）
     * @return 规范扩展名（jpg / png / gif / bmp / webp），无法识别返回 null
     */
    public static String detectImageExtension(byte[] header) {
        if (header == null || header.length < 4) {
            return null;
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47
                && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
            return "png";
        }
        // GIF: 47 49 46 38
        if (header.length >= 6 && (header[0] & 0xFF) == 'G' && (header[1] & 0xFF) == 'I'
                && (header[2] & 0xFF) == 'F' && (header[3] & 0xFF) == '8') {
            return "gif";
        }
        // BMP: 42 4D
        if ((header[0] & 0xFF) == 'B' && (header[1] & 0xFF) == 'M') {
            return "bmp";
        }
        // WEBP: RIFF....WEBP
        if (header.length >= 12
                && (header[0] & 0xFF) == 'R' && (header[1] & 0xFF) == 'I'
                && (header[2] & 0xFF) == 'F' && (header[3] & 0xFF) == 'F'
                && (header[8] & 0xFF) == 'W' && (header[9] & 0xFF) == 'E'
                && (header[10] & 0xFF) == 'B' && (header[11] & 0xFF) == 'P') {
            return "webp";
        }
        return null;
    }

    /**
     * 规范化扩展名：统一小写，jpeg 归一为 jpg
     */
    public static String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String ext = extension.startsWith(".") ? extension.substring(1) : extension;
        ext = ext.toLowerCase();
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    /**
     * 声明扩展名与真实类型是否一致
     */
    public static boolean matchesDeclared(String declaredExtension, String detectedExtension) {
        if (detectedExtension == null) {
            return false;
        }
        return normalizeExtension(declaredExtension).equals(normalizeExtension(detectedExtension));
    }
}