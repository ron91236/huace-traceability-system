package com.huace.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * config_json 读取口的回归测试。钉住四件事：
 * 1. 长图骨架只在 renderMode=long-image 时才解析出来，其余模式一律空
 * 2. 排序按 sortOrder 升序，缺 sortOrder 的行用它在数组里的位置兜底
 * 3. 非法/缺失 config_json 按"没有配置"处理，绝不抛解析异常打断请求
 * 4. 管理端保存校验只拒真正会坏事的输入：缺 key、重复 key、超 10 个、名称空或超长
 */
class TemplateConfigReaderTest {

    // 用真实 ObjectMapper：mock 的 readValue 恒返回 null，会让"解析失败按空处理"这类用例假通过
    private final TemplateConfigReader reader = new TemplateConfigReader(new ObjectMapper());

    private static final String TWO_MODULES =
            "{\"renderMode\":\"long-image\",\"longModules\":["
            + "{\"key\":\"m2\",\"name\":\"产品信息\",\"sortOrder\":1},"
            + "{\"key\":\"m1\",\"name\":\"有机种植\",\"sortOrder\":0}]}";

    @Test
    void 长图骨架按sortOrder升序返回() {
        List<TemplateConfigReader.LongModuleDef> defs = reader.longModulesOf(TWO_MODULES);

        assertEquals(2, defs.size());
        assertEquals("m1", defs.get(0).key());
        assertEquals("有机种植", defs.get(0).name());
        assertEquals("m2", defs.get(1).key());
    }

    @Test
    void 非长图模板读不出长图骨架() {
        assertEquals(List.of(), reader.longModulesOf("{\"renderMode\":\"visual\",\"longModules\":"
                + "[{\"key\":\"m1\",\"name\":\"不该出现\",\"sortOrder\":0}]}"));
        assertEquals(List.of(), reader.longModulesOf("{\"pages\":[]}"));
        assertEquals(List.of(), reader.longModulesOf(null));
        assertEquals(List.of(), reader.longModulesOf("  "));
    }

    @Test
    void 非法config_json按空骨架处理而不是抛解析异常() {
        // 线上 config_json 是自由文本列，历史脏数据不该让模板列表 500
        assertEquals(List.of(), reader.longModulesOf("{不是合法JSON"));
        assertNull(reader.renderModeOf("{不是合法JSON"));
    }

    @Test
    void 没有key的行被丢弃() {
        // 骨架行靠 key 与企业页图关联，无 key 的行两侧都认不出来，留着只会渲染出一个点不开的空胶囊
        List<TemplateConfigReader.LongModuleDef> defs = reader.longModulesOf(
                "{\"renderMode\":\"long-image\",\"longModules\":["
                + "{\"name\":\"忘了带key\"},{\"key\":\"m9\",\"name\":\"有key\"}]}");

        assertEquals(1, defs.size());
        assertEquals("m9", defs.get(0).key());
    }

    @Test
    void 缺sortOrder时用数组位置兜底() {
        List<TemplateConfigReader.LongModuleDef> defs = reader.longModulesOf(
                "{\"renderMode\":\"long-image\",\"longModules\":["
                + "{\"key\":\"a\",\"name\":\"甲\"},{\"key\":\"b\",\"name\":\"乙\"}]}");

        assertEquals(List.of("a", "b"), defs.stream().map(TemplateConfigReader.LongModuleDef::key).toList());
        assertEquals(List.of(0, 1), defs.stream().map(TemplateConfigReader.LongModuleDef::sortOrder).toList());
    }

    @Test
    void 校验放过空骨架与可视化模板() {
        // 新增长图模板时空骨架是合法初始态（spec §12：longModules 为空 → 企业端提示"平台尚未配置模块"）
        assertDoesNotThrow(() -> reader.validate("{\"renderMode\":\"long-image\",\"longModules\":[]}"));
        assertDoesNotThrow(() -> reader.validate("{\"renderMode\":\"long-image\"}"));
        assertDoesNotThrow(() -> reader.validate("{\"renderMode\":\"visual\",\"longModules\":["
                + "{\"key\":\"a\"},{\"key\":\"a\"}]}"));
        assertDoesNotThrow(() -> reader.validate(null));
    }

    @Test
    void 校验拒绝重复key() {
        BusinessException e = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"long-image\",\"longModules\":["
                + "{\"key\":\"a\",\"name\":\"甲\",\"sortOrder\":0},"
                + "{\"key\":\"a\",\"name\":\"乙\",\"sortOrder\":1}]}"));
        assertTrue(e.getMessage().contains("重复"), e.getMessage());
    }

    @Test
    void 校验拒绝超过10个模块() {
        StringBuilder sb = new StringBuilder("{\"renderMode\":\"long-image\",\"longModules\":[");
        for (int i = 0; i < 11; i++) {
            sb.append(i == 0 ? "" : ",")
              .append("{\"key\":\"m").append(i).append("\",\"name\":\"模块").append(i).append("\",\"sortOrder\":").append(i).append("}");
        }
        sb.append("]}");

        BusinessException e = assertThrows(BusinessException.class, () -> reader.validate(sb.toString()));
        assertTrue(e.getMessage().contains("10"), e.getMessage());
    }

    @Test
    void 校验拒绝空名称与超长名称() {
        BusinessException blank = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"long-image\",\"longModules\":[{\"key\":\"a\",\"name\":\"  \",\"sortOrder\":0}]}"));
        assertTrue(blank.getMessage().contains("名称"), blank.getMessage());

        String name = "名".repeat(51);
        BusinessException tooLong = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"long-image\",\"longModules\":[{\"key\":\"a\",\"name\":\"" + name + "\",\"sortOrder\":0}]}"));
        assertTrue(tooLong.getMessage().contains("50"), tooLong.getMessage());
    }

    @Test
    void 校验拒绝缺key的骨架行() {
        // longModulesOf 对缺 key 的行是"丢弃"（取数不能因为一行脏数据整体失败），
        // validate 必须是"拒绝"（管理端是唯一写入方，写进去就该当场知道写坏了）
        BusinessException e = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"long-image\",\"longModules\":[{\"name\":\"没有key\"}]}"));
        assertTrue(e.getMessage().contains("标识"), e.getMessage());
    }

    // ==================== 落地页骨架 ====================

    @Test
    void 落地页骨架按sortOrder升序且带上启用标记() {
        List<TemplateConfigReader.LandingBlockDef> blocks = reader.landingBlocksOf(
                "{\"renderMode\":\"landing\",\"landingBlocks\":["
                + "{\"key\":\"video\",\"name\":\"宣传视频\",\"sortOrder\":2,\"enabled\":true},"
                + "{\"key\":\"richtext\",\"name\":\"内容\",\"sortOrder\":0,\"enabled\":true},"
                + "{\"key\":\"antiFake\",\"name\":\"防伪查询\",\"sortOrder\":1,\"enabled\":false}]}");

        assertEquals(List.of("richtext", "antiFake", "video"),
                blocks.stream().map(TemplateConfigReader.LandingBlockDef::key).toList());
        assertTrue(blocks.get(2).enabled());
        assertFalse(blocks.get(1).enabled());
    }

    @Test
    void 非落地页模板读不出落地页骨架() {
        // 与 longModulesOf 同一条口径：renderMode 不对就整体返回空，别把老模板里的残留键当成配置
        assertEquals(List.of(), reader.landingBlocksOf(
                "{\"renderMode\":\"visual\",\"landingBlocks\":[{\"key\":\"richtext\",\"enabled\":true}]}"));
        assertEquals(List.of(), reader.landingBlocksOf(null));
        assertEquals(List.of(), reader.landingBlocksOf("{坏JSON"));
    }

    @Test
    void 枚举外的区块key被丢弃() {
        // 区块 key 是固定枚举（spec §5），拼错的 key 到了消费者端没人能渲染
        List<TemplateConfigReader.LandingBlockDef> blocks = reader.landingBlocksOf(
                "{\"renderMode\":\"landing\",\"landingBlocks\":["
                + "{\"key\":\"rich-text\",\"name\":\"拼错了\",\"sortOrder\":0,\"enabled\":true},"
                + "{\"key\":\"promoImages\",\"name\":\"宣传图片\",\"sortOrder\":1,\"enabled\":true}]}");

        assertEquals(1, blocks.size());
        assertEquals("promoImages", blocks.get(0).key());
    }

    @Test
    void sideMenu与gates读出配置并带默认值() {
        Map<String, Object> menu = reader.sideMenuOf(
                "{\"renderMode\":\"landing\",\"sideMenu\":{\"enabled\":true,\"display\":\"inline\"}}");
        assertEquals(true, menu.get("enabled"));
        assertEquals("inline", menu.get("display"));

        Map<String, Object> absent = reader.sideMenuOf("{\"renderMode\":\"landing\"}");
        // 缺 sideMenu 时不能返回 null 让上层到处判空：默认不启用、悬浮、显示溯源详情、显示底部入口
        assertEquals(false, absent.get("enabled"));
        assertEquals("float", absent.get("display"));
        assertEquals(true, absent.get("showTraceDetail"));
        assertEquals(true, absent.get("showBottomEntry"));

        assertTrue(reader.gatesOf("{\"renderMode\":\"landing\"}").isEmpty());
        assertEquals(true, reader.gatesOf(
                "{\"renderMode\":\"landing\",\"gates\":{\"requireAntiFakeBeforeJump\":true}}")
                .get("requireAntiFakeBeforeJump"));
    }

    @Test
    void 校验放过合法落地页骨架() {
        assertDoesNotThrow(() -> reader.validate(
                "{\"renderMode\":\"landing\",\"landingBlocks\":[{\"key\":\"richtext\",\"name\":\"内容\",\"sortOrder\":0,\"enabled\":true}],"
                        + "\"sideMenu\":{\"enabled\":true,\"display\":\"float\"}}"));
        // 未配置区块也是合法初始态，跟长图空骨架同理
        assertDoesNotThrow(() -> reader.validate("{\"renderMode\":\"landing\"}"));
    }

    @Test
    void 校验拒绝未知blockKey与重复key() {
        BusinessException unknown = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"landing\",\"landingBlocks\":[{\"key\":\"vr\",\"name\":\"全景\",\"sortOrder\":0,\"enabled\":true}]}"));
        assertTrue(unknown.getMessage().contains("区块"), unknown.getMessage());

        BusinessException dup = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"landing\",\"landingBlocks\":["
                        + "{\"key\":\"video\",\"name\":\"视频\",\"sortOrder\":0,\"enabled\":true},"
                        + "{\"key\":\"video\",\"name\":\"视频2\",\"sortOrder\":1,\"enabled\":true}]}"));
        assertTrue(dup.getMessage().contains("重复"), dup.getMessage());
    }

    @Test
    void 校验拒绝非法display取值() {
        // display 只认 float/inline：写错的值到了前端就是"既不悬浮也不直显"的第三种状态。
        // 这条同时钉住"校验必须读原始输入"——sideMenuOf 会把非法值收敛成默认值，拿它来校验就永远不红
        BusinessException e = assertThrows(BusinessException.class, () -> reader.validate(
                "{\"renderMode\":\"landing\",\"sideMenu\":{\"enabled\":true,\"display\":\"popup\"}}"));
        // 报错文案用中文说"侧边栏显示方式"，与其它校验同一口径；断言跟着文案走，别去猜英文字段名
        assertTrue(e.getMessage().contains("显示方式"), e.getMessage());
    }
}
