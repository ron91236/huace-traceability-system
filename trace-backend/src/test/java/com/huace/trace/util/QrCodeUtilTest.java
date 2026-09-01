package com.huace.trace.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 二维码生成工具回归测试。
 */
class QrCodeUtilTest {

    @Test
    void 生成Base64_返回PNG格式前缀() {
        String result = QrCodeUtil.generateBase64("https://trace.cti-pit.com/trace/00000626", 200, 200);
        assertTrue(result.startsWith("data:image/png;base64,"), "应返回 data URL");
        byte[] png = Base64.getDecoder().decode(result.substring("data:image/png;base64,".length()));
        assertTrue(png.length > 100, "PNG 内容不应为空");
        // PNG magic header
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }

    @Test
    void 生成字节_非空且为PNG() {
        byte[] png = QrCodeUtil.generateBytes("测试内容", 150, 150);
        assertNotNull(png);
        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
    }

    @Test
    void 不同内容_生成结果不同() {
        byte[] a = QrCodeUtil.generateBytes("A", 100, 100);
        byte[] b = QrCodeUtil.generateBytes("B", 100, 100);
        assertFalse(java.util.Arrays.equals(a, b), "不同内容生成的二维码不应相同");
    }
}
