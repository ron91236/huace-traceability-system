package com.huace.trace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ip2region 归属地解析回归测试（使用 resources/ip2region/ip2region.xdb 真实数据）。
 */
class IpRegionServiceTest {

    private final IpRegionService service = new IpRegionService();

    @Test
    void 解析阿里DNS地址_返回省份城市() {
        String[] region = service.resolve("223.5.5.5");
        assertNotNull(region, "223.5.5.5 应能解析出归属地");
        assertEquals(2, region.length);
        assertFalse(region[0].isEmpty(), "省份不应为空");
    }

    @Test
    void 解析本地回环地址_返回空() {
        assertNull(service.resolve("127.0.0.1"));
    }

    @Test
    void 解析内网地址_返回空() {
        assertNull(service.resolve("192.168.1.1"));
    }

    @Test
    void 非法IP_返回空() {
        assertNull(service.resolve("999.999.1.1"));
    }

    @Test
    void 空输入_返回空() {
        assertNull(service.resolve(null));
        assertNull(service.resolve(""));
        assertNull(service.resolve("   "));
    }

    @Test
    void 公网IP多次解析_结果一致() {
        String[] first = service.resolve("114.114.114.114");
        String[] second = service.resolve("114.114.114.114");
        if (first == null) {
            assertNull(second);
        } else {
            assertArrayEquals(first, second);
        }
    }
}
