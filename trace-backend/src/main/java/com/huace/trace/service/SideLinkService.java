package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.EnterpriseSideLink;
import com.huace.trace.mapper.EnterpriseSideLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业侧边栏外链：公众号、微博这类按钮的文案、图标与跳转目标。
 *
 * <p>挂企业而非模板（spec 决定 4）——它是企业固有资产，换模板不该丢配置，
 * 所以菜单也始终可见，接口不设模板授权守卫。
 * 一条链接要么有 url（点击直接跳），要么只有 image（点击弹二维码），两者都空是死按钮，写入就拒。
 */
@Service
@RequiredArgsConstructor
public class SideLinkService {

    /** 与表字段 VARCHAR(50) 一致 */
    public static final int NAME_MAX = 50;

    private final EnterpriseSideLinkMapper linkMapper;

    /** 企业端列表，含停用项，按 sort_order 升序 */
    public List<EnterpriseSideLink> list(Long enterpriseId) {
        return linkMapper.selectList(new LambdaQueryWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getEnterpriseId, enterpriseId)
                .orderByAsc(EnterpriseSideLink::getSortOrder)
                .orderByAsc(EnterpriseSideLink::getId));
    }

    /** 消费者取数：只要启用项，按 sort_order 升序 */
    public List<EnterpriseSideLink> resolveForConsumer(Long enterpriseId) {
        if (enterpriseId == null) return List.of();
        return linkMapper.selectList(new LambdaQueryWrapper<EnterpriseSideLink>()
                .eq(EnterpriseSideLink::getEnterpriseId, enterpriseId)
                .eq(EnterpriseSideLink::getStatus, 1)
                .orderByAsc(EnterpriseSideLink::getSortOrder));
    }

    /** 新建：企业归属与排序由服务端决定，前端不传 */
    @Transactional
    public void create(EnterpriseSideLink form, Long enterpriseId) {
        validate(form);
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
        validate(form);
        form.setId(existing.getId());
        form.setEnterpriseId(enterpriseId);
        form.setSortOrder(existing.getSortOrder());
        // 前端只 PUT 改动字段时 status 是 null，直接落库等于把按钮停用，而接口照样回 200
        if (form.getStatus() == null) form.setStatus(existing.getStatus());
        linkMapper.updateById(form);
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

    private void validate(EnterpriseSideLink form) {
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            throw new BusinessException("按钮名称不能为空");
        }
        String name = form.getName().trim();
        if (name.length() > NAME_MAX) throw new BusinessException("按钮名称不能超过 " + NAME_MAX + " 字");
        form.setName(name);
        String url = form.getUrl() == null ? "" : form.getUrl().trim();
        String image = form.getImage() == null ? "" : form.getImage().trim();
        if (url.isEmpty() && image.isEmpty()) {
            throw new BusinessException("跳转链接与跳转图片至少填一项");
        }
        // 只认 http/https：这个值最终会成为消费者点击时 window.location.href 的目标，
        // 存进 javascript: 或 data: 就等于把 XSS 写进了库里（图片走本系统上传，不在这条线上）
        if (!url.isEmpty() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException("跳转链接必须以 http:// 或 https:// 开头");
        }
        // 空串统一落成 NULL：库里留空串会让"有没有跳转目标"变成两种判法
        form.setUrl(url.isEmpty() ? null : url);
        form.setImage(image.isEmpty() ? null : image);
    }
}
