package com.huace.trace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.dto.HgzPublicVO;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 缓存序列化往返回归测试：
 * Map<String,Object> 中的标量值会以 @class 类型信息写入缓存，
 * 若 BasicPolymorphicTypeValidator 白名单遗漏某类型，缓存命中时返回 500。
 */
class RedisTypingTest {

    @Test
    void mapWithScalarValuesSurvivesRoundTrip() throws Exception {
        ObjectMapper mapper = new RedisConfig().redisObjectMapper();

        Map<String, Object> src = new HashMap<>();
        src.put("longVal", 123L);
        src.put("intVal", 42);
        src.put("strVal", "hello");
        src.put("boolVal", true);
        src.put("doubleVal", 1.5d);
        src.put("dateVal", Date.valueOf("2026-08-18"));
        src.put("tsVal", new Timestamp(1788234836000L));
        src.put("localDateVal", LocalDate.of(2026, 8, 18));
        src.put("listVal", Arrays.asList(1L, 2L, 3L));

        String json = mapper.writeValueAsString(src);
        Object back = mapper.readValue(json, Object.class);

        assertTrue(back instanceof Map, "应反序列化为 Map");
        Map<?, ?> map = (Map<?, ?>) back;
        assertEquals(123L, map.get("longVal"));
        assertEquals(42, map.get("intVal"));
        assertEquals("hello", map.get("strVal"));
        assertEquals(true, map.get("boolVal"));
        assertEquals(1.5d, map.get("doubleVal"));
        // java.sql.Date 的 equals 按毫秒比较，跨时区序列化往返毫秒可能偏移，按日期字符串断言
        assertEquals("2026-08-18", map.get("dateVal").toString());
        assertEquals(src.get("tsVal"), map.get("tsVal"));
        assertEquals(LocalDate.of(2026, 8, 18), map.get("localDateVal"));
        assertEquals(Arrays.asList(1L, 2L, 3L), map.get("listVal"));
    }

    @Test
    void unknownDangerousTypeIsRejected() {
        ObjectMapper mapper = new RedisConfig().redisObjectMapper();
        // 白名单外的类型必须被拒绝，防止缓存被篡改后反序列化执行任意类
        String evil = "{\"@class\":\"java.lang.ProcessBuilder\",\"command\":[\"rm\",\"-rf\",\"/\"]}";
        assertThrows(Exception.class, () -> mapper.readValue(evil, Object.class));
    }

    @Test
    void hgzPublicVoServesRedisCacheRoundTrip() throws Exception {
        ObjectMapper mapper = new RedisConfig().redisObjectMapper();

        HgzPublicVO vo = new HgzPublicVO();
        vo.setCode("HGZ20260902-ABCD");
        vo.setUserType(1);
        vo.setProductName("阳光玫瑰葡萄");
        vo.setNumber("500公斤");
        vo.setPlaceOfOrigin("江苏省苏州市吴中区");
        vo.setPromiseUser("某某农业科技有限公司");
        vo.setContact("13800000000");
        vo.setUseTime(LocalDate.of(2026, 9, 2));
        vo.setIsShowEnterprise(1);
        vo.setStatus(1);
        vo.setQrUrl("https://trace.cti-pit.com/hgz/HGZ20260902-ABCD");
        vo.setQueryUrl("https://trace.cti-pit.com/trace/batch/123");
        vo.setBatchId(123L);
        vo.setGoodsId(45L);
        vo.setBatchName("2026秋葡萄一号批次");
        vo.setEnterpriseName("某某农业科技有限公司");
        Map<String, Object> promise = new HashMap<>();
        promise.put("title", "不使用禁用的农药兽药、停用兽药及非法添加物");
        promise.put("isSelect", true);
        Map<String, Object> basis = new HashMap<>();
        basis.put("title", "委托检测合格");
        basis.put("isSelect", true);
        basis.put("image", "/uploads/2026/09/02/report.png");
        vo.setPromiseList(Arrays.asList(promise));
        vo.setBasisList(Arrays.asList(basis));

        String json = mapper.writeValueAsString(vo);
        Object back = mapper.readValue(json, Object.class);

        assertTrue(back instanceof HgzPublicVO, "应反序列化为 HgzPublicVO");
        HgzPublicVO r = (HgzPublicVO) back;
        assertEquals(vo.getCode(), r.getCode());
        assertEquals(vo.getProductName(), r.getProductName());
        assertEquals(LocalDate.of(2026, 9, 2), r.getUseTime());
        assertEquals(vo.getQrUrl(), r.getQrUrl());
        assertEquals(1, r.getPromiseList().size());
        assertEquals("不使用禁用的农药兽药、停用兽药及非法添加物", r.getPromiseList().get(0).get("title"));
        assertEquals(Boolean.TRUE, r.getBasisList().get(0).get("isSelect"));
        assertEquals("/uploads/2026/09/02/report.png", r.getBasisList().get(0).get("image"));
    }
}
