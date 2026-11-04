package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.EnterpriseSideLink;
import com.huace.trace.mapper.EnterpriseSideLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 企业侧边栏外链：公众号、微博这类按钮的文案、图标与跳转目标。
 *
 * <p>挂企业而非模板（spec 决定 4）——它是企业固有资产，换模板不该丢配置，
 * 所以菜单也始终可见，接口不设模板授权守卫。
 * 一条链接要么有 url（点击直接跳），要么只有资料（点击弹层逐页翻看），两者都空是死按钮，写入就拒。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SideLinkService {

    /** 与表字段 VARCHAR(50) 一致 */
    public static final int NAME_MAX = 50;
    /** 与表字段 VARCHAR(64) 一致：本系统上传的 /uploads/ 路径约 45 字，留不下也别说得含糊 */
    public static final int ICON_MAX = 64;
    /** 单个按钮的资料条目上限：再多就不是"翻几页证书"而是加载瀑布 */
    public static final int MAX_ITEMS = 10;
    /** 单个 PDF 的页数上限，沿用长图 LongModuleService.MAX_PAGES */
    public static final int MAX_PAGES_PER_PDF = 30;
    /** 单个按钮展开后的总页数上限：10 个 30 页的 PDF 会变成 300 张图 */
    public static final int MAX_TOTAL_PAGES = 60;
    /** 只认本系统上传目录：这个值最终是消费者免登录页面上 &lt;img&gt; 的 src */
    private static final String UPLOAD_PREFIX = "/uploads/";

    private final EnterpriseSideLinkMapper linkMapper;
    private final ObjectMapper objectMapper;
    private final PdfConvertService pdfConvertService;

    /** 企业端列表，含停用项，按 sort_order 升序。刻意不填 pages：企业要看的是自己传的原始条目 */
    public List<EnterpriseSideLink> list(Long enterpriseId) {
        return linkMapper.selectList(new LambdaQueryWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getEnterpriseId, enterpriseId)
                .orderByAsc(EnterpriseSideLink::getSortOrder)
                .orderByAsc(EnterpriseSideLink::getId));
    }

    /** 消费者取数：只要启用项，按 sort_order 升序，并把资料展开成可逐页滑动的图片地址 */
    public List<EnterpriseSideLink> resolveForConsumer(Long enterpriseId) {
        if (enterpriseId == null) return List.of();
        List<EnterpriseSideLink> links = linkMapper.selectList(new LambdaQueryWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getEnterpriseId, enterpriseId)
                .eq(EnterpriseSideLink::getStatus, 1)
                .orderByAsc(EnterpriseSideLink::getSortOrder));
        links.forEach(link -> link.setPages(expandPages(link.getImage())));
        return links;
    }

    /** 新建：企业归属与排序由服务端决定，前端不传 */
    @Transactional
    public void create(EnterpriseSideLink form, Long enterpriseId) {
        List<String> items = validate(form);
        warmUpPdfs(items);
        Long count = linkMapper.selectCount(new LambdaQueryWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getEnterpriseId, enterpriseId));
        form.setId(null);
        // 无条件覆盖，不是"请求体没填才补"：否则企业可以把按钮塞到别家名下
        form.setEnterpriseId(enterpriseId);
        form.setSortOrder(count == null ? 0 : count.intValue());
        if (form.getStatus() == null) form.setStatus(1);
        linkMapper.insert(form);
    }

    /** 更新：文案/图标/跳转/启停；归属企业与排序由服务端守住 */
    @Transactional
    public void update(Long id, EnterpriseSideLink form, Long enterpriseId) {
        EnterpriseSideLink existing = owned(id, enterpriseId);
        List<String> items = validate(form);
        warmUpPdfs(items);
        // 前端只 PUT 改动字段时 status 是 null，直接落库等于把按钮停用，而接口照样回 200
        if (form.getStatus() == null) form.setStatus(existing.getStatus());
        // 逐列显式 SET 而不是 updateById：NOT_NULL 策略会跳过 null 字段，于是"清空图标"
        // "清空跳转地址"这类操作永远不生效，界面刷新后值又回来，而接口一直是 200
        linkMapper.update(null, new LambdaUpdateWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getId, existing.getId())
                .set(EnterpriseSideLink::getName, form.getName())
                .set(EnterpriseSideLink::getIcon, form.getIcon())
                .set(EnterpriseSideLink::getUrl, form.getUrl())
                .set(EnterpriseSideLink::getImage, form.getImage())
                .set(EnterpriseSideLink::getStatus, form.getStatus()));
    }

    @Transactional
    public void delete(Long id, Long enterpriseId) {
        EnterpriseSideLink existing = owned(id, enterpriseId);
        linkMapper.deleteById(existing.getId());
    }

    /** 拖拽排序：传入完整 id 顺序，空列表、重复或任一不属于本企业都整体拒绝，不做部分生效 */
    @Transactional
    public void sort(List<Long> ids, Long enterpriseId) {
        // 空 ids 是畸形请求而不是合法 no-op：前端把 JSON 键写错时 controller 转下来的就是 null，
        // 静默 return 会让"排序不再保存"在整条链路上全绿
        if (ids == null || ids.isEmpty()) throw new BusinessException("排序列表不能为空");
        // 重复 id 会让同一行拿到两个不同 index，sort_order 直接撞车
        if (ids.stream().distinct().count() != ids.size()) {
            throw new BusinessException("排序列表不能有重复链接");
        }
        List<EnterpriseSideLink> owned = new ArrayList<>();
        for (Long id : ids) owned.add(owned(id, enterpriseId));
        for (int i = 0; i < owned.size(); i++) {
            EnterpriseSideLink patch = new EnterpriseSideLink();
            patch.setId(owned.get(i).getId());
            patch.setSortOrder(i);
            // 只带 sortOrder：url/status 不在 SET 子句里，拖一次顺序不会顺带改跳转目标或停用按钮
            linkMapper.updateById(patch);
        }
    }

    private EnterpriseSideLink owned(Long id, Long enterpriseId) {
        EnterpriseSideLink existing = linkMapper.selectById(id);
        if (existing == null) throw new BusinessException("链接不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        return existing;
    }

    /** 校验并归一化，返回资料条目供 PDF 预热使用 */
    private List<String> validate(EnterpriseSideLink form) {
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            throw new BusinessException("按钮名称不能为空");
        }
        String name = form.getName().trim();
        if (name.length() > NAME_MAX) throw new BusinessException("按钮名称不能超过 " + NAME_MAX + " 字");
        form.setName(name);
        String url = form.getUrl() == null ? "" : form.getUrl().trim();
        List<String> items = normalizeItems(form.getImage());
        if (url.isEmpty() && items.isEmpty()) {
            throw new BusinessException("跳转链接与跳转资料至少填一项");
        }
        // 只认 http/https：这个值最终会成为消费者点击时 window.location.href 的目标，
        // 存进 javascript: 或 data: 就等于把 XSS 写进了库里（资料走本系统上传，不在这条线上）
        if (!url.isEmpty() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException("跳转链接必须以 http:// 或 https:// 开头");
        }
        // 空值统一落成 NULL：库里留空串或 "[]" 会让"有没有资料"变成两种判法
        form.setUrl(url.isEmpty() ? null : url);
        form.setImage(items.isEmpty() ? null : writeItems(items));

        // 图标可空（消费者页只在它看起来像图片地址时才渲染），同样只留一种"没有"的表示
        String icon = form.getIcon() == null ? "" : form.getIcon().trim();
        if (icon.length() > ICON_MAX) throw new BusinessException("图标地址不能超过 " + ICON_MAX + " 字");
        form.setIcon(icon.isEmpty() ? null : icon);
        return items;
    }

    /**
     * 写入侧把资料归一成条目数组。库里只留 JSON 数组这一种表示；
     * 单个裸路径是本次改动之前的形态（也包含升级当天仍开着旧企业端页面的提交），
     * 按单元素数组收下而不是拒掉——否则"只改个按钮文案"这种操作会全部失败。
     */
    private List<String> normalizeItems(String image) {
        String raw = image == null ? "" : image.trim();
        if (raw.isEmpty()) return List.of();
        List<String> items;
        if (raw.startsWith("[")) {
            try {
                items = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                throw new BusinessException("跳转资料必须是资料地址数组，请重新提交");
            }
        } else if (raw.startsWith(UPLOAD_PREFIX)) {
            items = List.of(raw);
        } else {
            throw new BusinessException("跳转资料必须是资料地址数组，请重新提交");
        }
        List<String> clean = new ArrayList<>();
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            String path = item.trim();
            if (!path.startsWith(UPLOAD_PREFIX)) {
                throw new BusinessException("跳转资料只能使用本系统上传的文件，地址需以 " + UPLOAD_PREFIX + " 开头");
            }
            clean.add(path);
        }
        if (clean.size() > MAX_ITEMS) {
            throw new BusinessException("单个按钮最多挂 " + MAX_ITEMS + " 份资料，当前 " + clean.size() + " 份");
        }
        return clean;
    }

    /** 读侧兼容：库里可能是 JSON 数组，也可能是本次改动之前的裸路径；坏值按"没配资料"处理 */
    private List<String> readItems(String image) {
        String raw = image == null ? "" : image.trim();
        if (raw.isEmpty()) return List.of();
        if (!raw.startsWith("[")) return raw.startsWith(UPLOAD_PREFIX) ? List.of(raw) : List.of();
        try {
            List<String> items = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            return items.stream().filter(i -> i != null && i.startsWith(UPLOAD_PREFIX)).toList();
        } catch (Exception e) {
            log.warn("侧栏资料不是合法数组，按未配处理: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeItems(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new BusinessException("跳转资料格式错误，请重新提交");
        }
    }

    /**
     * 保存时就把 PDF 转成页图：转不动的文件不进库，转换结果同时落进 uploads/pdf-images/ 缓存，
     * 消费者第一次点开是列目录而不是现转。转换按 PDF 内容哈希幂等，写库失败留下的缓存目录
     * 下次保存会命中同一个，不会越积越多。
     */
    private void warmUpPdfs(List<String> items) {
        int total = 0;
        for (String item : items) {
            if (!isPdf(item)) continue;
            List<String> pages;
            try {
                pages = pdfConvertService.convertPdfToImages(item, MAX_PAGES_PER_PDF);
            } catch (BusinessException e) {
                throw new BusinessException(fileName(item) + "：" + e.getMessage());
            }
            // 文件不存在、不是 PDF、超过体积上限时 convert 返回空列表而不抛异常，
            // 放过它就会存下一个消费者点开必然空白的条目
            if (pages.isEmpty()) {
                throw new BusinessException(fileName(item) + " 无法解析，请确认文件未加密且格式正确");
            }
            total += pages.size();
            if (total > MAX_TOTAL_PAGES) {
                throw new BusinessException("资料展开后共 " + total + " 页，超过 " + MAX_TOTAL_PAGES
                        + " 页上限，请减少 PDF 或拆分后上传");
            }
        }
    }

    /** 原始条目 → 消费者能直接逐页渲染的图片地址：图片原样，PDF 换成页图 */
    private List<String> expandPages(String image) {
        List<String> pages = new ArrayList<>();
        for (String item : readItems(image)) {
            if (!isPdf(item)) {
                pages.add(item);
                continue;
            }
            try {
                pages.addAll(pdfConvertService.convertPdfToImages(item, MAX_PAGES_PER_PDF));
            } catch (Exception e) {
                // 一条转不动不该让整个按钮点不开：跳过它，其余资料照常显示
                log.warn("侧栏资料 PDF 展开失败，跳过该条: {} ({})", item, e.getMessage());
            }
        }
        return pages;
    }

    /** .PDF 也是 PDF：判定大小写不敏感，漏判就是消费者点开一片空白 */
    private static boolean isPdf(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }
}
