package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.TraceLongModuleMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 长图溯源内容：模块骨架归模板（管理端写），整页图归企业（本轮唯一由企业写的数据）。
 *
 * <p>行坐标是 (template_id, enterprise_id, module_key)，且「企业未填」用**无行**表示，
 * 所以本类不提供"改名/排序/删除模块"的能力——那些动作的主语是模板，不是企业。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LongModuleService {

    /** 单模块最多拼接的整页图数量 */
    public static final int MAX_PAGES = 30;

    private final TraceLongModuleMapper moduleMapper;
    private final ObjectMapper objectMapper;
    private final PdfConvertService pdfConvertService;
    private final CacheManager cacheManager;
    /** 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序，测试手工构造时追加即可 */
    private final TemplateConfigReader configReader;

    /** 上传目录，用于把 /uploads/ 相对路径还原成磁盘路径做体积校验 */
    @Setter(AccessLevel.PACKAGE)
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 企业端列表：模板骨架 ⊕ 该企业已存的页图。
     * 骨架里有但没填的模块要带着空 pages 出现（企业要看清"该填哪几栏"），
     * 骨架外还留在表里的历史行被忽略（管理端删过模块，spec §12 决定不做级联删除）。
     */
    public List<Map<String, Object>> list(TraceTemplate template, Long enterpriseId) {
        Map<String, List<String>> stored = storedPages(template, enterpriseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TemplateConfigReader.LongModuleDef d : skeleton(template)) {
            Map<String, Object> item = new HashMap<>();
            item.put("key", d.key());
            item.put("name", d.name());
            item.put("sortOrder", d.sortOrder());
            item.put("pages", stored.getOrDefault(d.key(), List.of()));
            result.add(item);
        }
        return result;
    }

    /** 消费端取数：按骨架顺序输出企业已填的模块，空模块直接跳过 */
    public List<Map<String, Object>> resolveForTrace(TraceTemplate template, Long enterpriseId) {
        if (enterpriseId == null) return List.of();
        Map<String, List<String>> stored = storedPages(template, enterpriseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TemplateConfigReader.LongModuleDef d : skeleton(template)) {
            List<String> pages = stored.get(d.key());
            if (pages == null || pages.isEmpty()) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("key", d.key());
            item.put("name", d.name());
            item.put("pages", pages);
            result.add(item);
        }
        return result;
    }

    /**
     * 保存某模块的整页图。pages 为空即"企业未填"，按 spec §12 用无行表示，所以这里删行而不是写空数组。
     *
     * <p>跨企业写入没有独立的归属判断方法：定位行的那条查询本身就同时绑了 template_id 与
     * enterprise_id，别人的行根本 select 不到，只会走"新增"分支插一条属于自己的行。
     */
    @Transactional
    public void savePages(TraceTemplate template, String moduleKey, List<String> pages, Long enterpriseId) {
        TemplateConfigReader.LongModuleDef def = skeleton(template).stream()
                .filter(d -> d.key().equals(moduleKey))
                .findFirst()
                // 骨架里没有这个 key 就别写：那会落出一行任何一侧都取不出来的孤儿数据
                .orElseThrow(() -> new BusinessException("模块不存在"));

        List<String> clean = pages == null ? List.of()
                : pages.stream().filter(p -> p != null && !p.isBlank()).toList();

        // 校验一律排在查库之前：坏请求不该先摸一次数据库
        if (clean.size() > MAX_PAGES) throw new BusinessException("单个模块最多拼接 " + MAX_PAGES + " 页");

        TraceLongModule existing = moduleMapper.selectOne(new LambdaQueryWrapper<TraceLongModule>()
                .eq(TraceLongModule::getTemplateId, template.getId())
                .eq(TraceLongModule::getEnterpriseId, enterpriseId)
                .eq(TraceLongModule::getModuleKey, def.key()));

        if (clean.isEmpty()) {
            if (existing != null) moduleMapper.deleteById(existing.getId());
            evictTracePage();
            return;
        }

        String json = writePages(clean);
        if (existing == null) {
            TraceLongModule row = new TraceLongModule();
            row.setTemplateId(template.getId());
            row.setEnterpriseId(enterpriseId);
            row.setModuleKey(def.key());
            row.setPages(json);
            moduleMapper.insert(row);
        } else {
            // 只 SET pages：name/sort_order/status 归模板骨架，写回去就是第二份真相；
            // 而 updateById 的 NOT_NULL 策略会把 null 字段跳过，反而不能保证清掉旧值，
            // 所以这里维持显式 SET 的写法。
            moduleMapper.update(null, new LambdaUpdateWrapper<TraceLongModule>()
                    .eq(TraceLongModule::getId, existing.getId())
                    .set(TraceLongModule::getPages, json));
        }
        evictTracePage();
    }

    /**
     * 上传后的 PDF 转整页图。只接受本系统 /uploads/ 下的文件，
     * 避免把该接口变成任意 URL 的抓取入口；还原成磁盘路径后要求它是上传目录内的普通文件，
     * 挡掉 /uploads/../../ 这类穿越和目录（企业账号登录态不代表可信路径）。
     */
    public List<String> convertPdf(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            throw new BusinessException("只能转换本系统上传的文件，请重新上传");
        }
        Path path = resolveUploadFile(url);
        if (path == null) throw new BusinessException("文件不存在或不合法，请重新上传");
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败，请重新上传");
        }
        if (size > PdfConvertService.MAX_PDF_BYTES) {
            throw new BusinessException("PDF 超过 " + PdfConvertService.MAX_PDF_BYTES / 1024 / 1024 + "MB，请压缩后重新上传");
        }
        List<String> pages = pdfConvertService.convertPdfToImages(url, MAX_PAGES);
        // 加密/损坏的 PDF 到这里是空结果，直接回 200 前端既没东西可展示也没东西可报错
        if (pages.isEmpty()) throw new BusinessException("PDF 无法解析，请确认文件未加密且格式正确");
        return pages;
    }

    /** 模板定义的模块骨架；顺序与内容都由 TemplateConfigReader 从 config_json 读出 */
    private List<TemplateConfigReader.LongModuleDef> skeleton(TraceTemplate template) {
        if (template == null) return List.of();
        return configReader.longModulesOf(template.getConfigJson());
    }

    /** 该企业该模板已存的页图，按 module_key 归档 */
    private Map<String, List<String>> storedPages(TraceTemplate template, Long enterpriseId) {
        if (template == null || template.getId() == null || enterpriseId == null) return Map.of();
        Map<String, List<String>> out = new HashMap<>();
        for (TraceLongModule m : moduleMapper.selectList(new LambdaQueryWrapper<TraceLongModule>()
                .eq(TraceLongModule::getTemplateId, template.getId())
                .eq(TraceLongModule::getEnterpriseId, enterpriseId))) {
            if (m.getModuleKey() == null || m.getModuleKey().isBlank()) continue;
            out.put(m.getModuleKey(), parsePages(m.getId(), m.getPages()));
        }
        return out;
    }

    private String writePages(List<String> pages) {
        try {
            return objectMapper.writeValueAsString(pages);
        } catch (Exception e) {
            log.warn("长图页图序列化失败: {}", e.getMessage());
            throw new BusinessException("长图数据格式错误，请重新提交");
        }
    }

    /**
     * 与 PdfConvertService#loadPdfBytes 的目录收口是同一段逻辑的两次落地：
     * 两道门各自校验自己的输入，保证方不同（这里是有登录态的企业端，那里是免登录的公开接口），
     * 所以改动必须同步。
     */
    private Path resolveUploadFile(String url) {
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize().toRealPath();
            Path lexical = root.resolve(url.substring("/uploads/".length())).normalize();
            if (!lexical.startsWith(root)) return null;
            Path real = lexical.toRealPath();
            return real.startsWith(root) && Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS) ? real : null;
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException 收的是 InvalidPathException（路径里有 NUL 之类非法字符）：
            // 它不是 IOException，漏接就一路冒到全局兜底，把"系统内部错误: <用户输入>"回显给用户，
            // 而免登录那扇门对同样的输入只返回空 —— 两扇门必须一个说法。
            // file.upload-dir 配错也是从这里现形的，不写日志用户只会看到一句"请重新上传"。
            log.warn("上传目录内路径无法解析: {} ({})", url, e.getMessage());
            return null;
        }
    }

    /** 解析 pages：坏数据按无内容跳过，日志带模块 id 才能定位是哪行数据坏了 */
    private List<String> parsePages(Long moduleId, String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> pages = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return pages.stream().filter(p -> p != null && !p.isBlank()).toList();
        } catch (Exception e) {
            log.warn("长图模块 id={} 的 pages 解析失败: {}", moduleId, json, e);
            return List.of();
        }
    }

    /** 长图内容变更会影响溯源页，直接清缓存（避免与 TracePageService 循环依赖） */
    private void evictTracePage() {
        try {
            Cache cache = cacheManager.getCache("tracePage");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除溯源页缓存失败", e);
        }
    }
}
