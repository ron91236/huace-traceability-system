package com.huace.trace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
