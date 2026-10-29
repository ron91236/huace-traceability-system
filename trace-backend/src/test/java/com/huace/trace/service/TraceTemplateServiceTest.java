package com.huace.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CodePackageItemMapper;
import com.huace.trace.mapper.OrderCodeMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 模板保存的骨架校验。钉住三件事：
 * 1. 坏骨架（重复 key / 超 10 个）在落库之前就被拒，接口不返回 200
 * 2. 列表页状态开关那种只带 status 的局部更新不能触发校验，否则停用模板必然报错
 * 3. 可视化模板的保存路径完全不受影响
 */
@ExtendWith(MockitoExtension.class)
class TraceTemplateServiceTest {

    @Mock private TraceTemplateMapper templateMapper;
    @Mock private OrderCodeMapper orderCodeMapper;
    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private TracePageService tracePageService;

    private TraceTemplateService service;

    @BeforeEach
    void setUp() {
        // 必须在 @BeforeEach 里构造：字段初始化器跑在 @Mock 注入之前，会拿到 null mapper。
        // 参数顺序与 @RequiredArgsConstructor 的字段声明顺序一致，新依赖加在最后。
        service = new TraceTemplateService(templateMapper, orderCodeMapper, codePackageItemMapper,
                new ObjectMapper(), tracePageService, new TemplateConfigReader(new ObjectMapper()));
    }

    private static final String DUP_KEY =
            "{\"renderMode\":\"long-image\",\"longModules\":["
            + "{\"key\":\"a\",\"name\":\"甲\",\"sortOrder\":0},"
            + "{\"key\":\"a\",\"name\":\"乙\",\"sortOrder\":1}]}";

    private static final String ELEVEN_MODULES;

    static {
        StringBuilder sb = new StringBuilder("{\"renderMode\":\"long-image\",\"longModules\":[");
        for (int i = 0; i < 11; i++) {
            sb.append(i == 0 ? "" : ",")
              .append("{\"key\":\"m").append(i).append("\",\"name\":\"模块").append(i).append("\",\"sortOrder\":").append(i).append("}");
        }
        ELEVEN_MODULES = sb.append("]}").toString();
    }

    private TraceTemplate existingLongImage() {
        TraceTemplate existing = new TraceTemplate();
        existing.setId(20L);
        existing.setTemplateKey("long-image");
        return existing;
    }

    @Test
    void 新建长图模板时重复key被拒且不落库() {
        TraceTemplate t = new TraceTemplate();
        t.setConfigJson(DUP_KEY);

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(t));
        assertTrue(e.getMessage().contains("重复"), e.getMessage());
        verify(templateMapper, never()).insert(any());
        // 校验必须在清缓存之前：坏数据没落库就不该有"让所有消费者页失效"的副作用
        verify(tracePageService, never()).evictAllCache();
    }

    @Test
    void 更新长图模板时超过10个模块被拒() {
        when(templateMapper.selectById(20L)).thenReturn(existingLongImage());

        TraceTemplate patch = new TraceTemplate();
        patch.setConfigJson(ELEVEN_MODULES);

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(20L, patch));
        assertTrue(e.getMessage().contains("10"), e.getMessage());
        verify(templateMapper, never()).updateById(any());
    }

    @Test
    void 只改状态的局部更新不触发骨架校验() {
        // TemplateList.vue 的状态开关只 PUT {status}，configJson 是 null。
        // 拿 null 去校验会让"停用模板"这个动作在带长图骨架的模板上必然失败。
        when(templateMapper.selectById(20L)).thenReturn(existingLongImage());

        TraceTemplate patch = new TraceTemplate();
        patch.setStatus(0);

        service.update(20L, patch);

        verify(templateMapper).updateById(patch);
        assertEquals("long-image", patch.getTemplateKey(), "更新不允许改 templateKey");
    }

    @Test
    void 可视化模板照常保存() {
        TraceTemplate t = new TraceTemplate();
        t.setTemplateName("标准绿");
        t.setConfigJson("{\"renderMode\":\"visual\",\"pages\":[]}");

        service.create(t);

        verify(templateMapper).insert(t);
        assertTrue(t.getTemplateKey().startsWith("tpl_"), t.getTemplateKey());
    }
}
