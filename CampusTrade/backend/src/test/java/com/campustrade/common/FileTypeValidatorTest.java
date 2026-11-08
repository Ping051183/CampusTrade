package com.campustrade.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileTypeValidator 单元测试：魔数识别与扩展名比对。
 */
@DisplayName("文件类型校验")
class FileTypeValidatorTest {

    private static byte[] withHeader(int... bytes) {
        byte[] data = new byte[FileTypeValidator.HEADER_LENGTH];
        for (int i = 0; i < bytes.length && i < data.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return data;
    }

    @Test
    @DisplayName("识别 JPEG / PNG / GIF / BMP / WEBP")
    void detectImageTypes() {
        assertEquals("jpg", FileTypeValidator.detectImageExtension(withHeader(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("png", FileTypeValidator.detectImageExtension(
                withHeader(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals("gif", FileTypeValidator.detectImageExtension(withHeader('G', 'I', 'F', '8', '9', 'a')));
        assertEquals("bmp", FileTypeValidator.detectImageExtension(withHeader('B', 'M', 0x36, 0x00)));
        assertEquals("webp", FileTypeValidator.detectImageExtension(
                withHeader('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')));
    }

    @Test
    @DisplayName("HTML 改名成 jpg 会被识别为不支持")
    void htmlRenamedToJpg() {
        byte[] header = withHeader('<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E', ' ', 'h', 't');
        assertNull(FileTypeValidator.detectImageExtension(header));
        assertFalse(FileTypeValidator.matchesDeclared("jpg", null));
    }

    @Test
    @DisplayName("真实类型与扩展名不一致时拒绝")
    void extensionMismatch() {
        assertFalse(FileTypeValidator.matchesDeclared("png", "jpg"));
        assertTrue(FileTypeValidator.matchesDeclared("png", "png"));
    }

    @Test
    @DisplayName("jpeg 与 jpg 视为同一种类型，大小写不敏感")
    void normalizeAlias() {
        assertTrue(FileTypeValidator.matchesDeclared("JPEG", "jpg"));
        assertTrue(FileTypeValidator.matchesDeclared(".JPG", "jpg"));
        assertEquals("jpg", FileTypeValidator.normalizeExtension(".jpeg"));
    }
}