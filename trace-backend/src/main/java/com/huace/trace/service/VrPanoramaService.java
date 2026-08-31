package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VrPanoramaService {

    private final VrSceneMapper vrSceneMapper;
    private final VrHotspotMapper vrHotspotMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseBaseMapper baseMapper;

    /**
     * 管理端 - 场景列表（含热点）
     */
    public List<VrScene> listScenes(Long enterpriseId) {
        LambdaQueryWrapper<VrScene> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(VrScene::getEnterpriseId, enterpriseId);
        w.orderByAsc(VrScene::getSortOrder).orderByAsc(VrScene::getId);
        List<VrScene> scenes = vrSceneMapper.selectList(w);
        fillScenes(scenes);
        return scenes;
    }

    /**
     * C端 - 获取企业的VR导览数据（按基地过滤可选）
     */
    public List<VrScene> listForTrace(Long enterpriseId, Long baseId) {
        if (enterpriseId == null) return List.of();
        LambdaQueryWrapper<VrScene> w = new LambdaQueryWrapper<>();
        w.eq(VrScene::getEnterpriseId, enterpriseId);
        if (baseId != null) w.eq(VrScene::getBaseId, baseId);
        w.orderByAsc(VrScene::getSortOrder).orderByAsc(VrScene::getId);
        List<VrScene> scenes = vrSceneMapper.selectList(w);
        fillScenes(scenes);
        return scenes;
    }

    @Transactional
    public VrScene createScene(VrScene scene) {
        if (scene.getPanoramaUrl() == null || scene.getPanoramaUrl().isBlank()) {
            throw new BusinessException("全景图不能为空");
        }
        if (scene.getHfov() == null) scene.setHfov(120);
        if (scene.getVfov() == null) scene.setVfov(90);
        if (scene.getSortOrder() == null) scene.setSortOrder(0);
        if (scene.getIsDefault() == null) scene.setIsDefault(0);
        vrSceneMapper.insert(scene);
        fillScene(scene);
        return scene;
    }

    @Transactional
    public VrScene updateScene(Long id, VrScene scene) {
        VrScene existing = vrSceneMapper.selectById(id);
        if (existing == null) throw new BusinessException("场景不存在");
        scene.setId(id);
        if (scene.getEnterpriseId() == null) scene.setEnterpriseId(existing.getEnterpriseId());
        vrSceneMapper.updateById(scene);
        VrScene updated = vrSceneMapper.selectById(id);
        fillScene(updated);
        return updated;
    }

    @Transactional
    public void deleteScene(Long id) {
        // 级联删除热点
        vrHotspotMapper.delete(new LambdaQueryWrapper<VrHotspot>().eq(VrHotspot::getSceneId, id));
        // 删除引用此场景作为目标的热点
        vrHotspotMapper.delete(new LambdaQueryWrapper<VrHotspot>().eq(VrHotspot::getTargetSceneId, id));
        vrSceneMapper.deleteById(id);
    }

    public VrHotspot createHotspot(VrHotspot hotspot) {
        if (hotspot.getSceneId() == null) throw new BusinessException("场景ID不能为空");
        VrScene scene = vrSceneMapper.selectById(hotspot.getSceneId());
        if (scene == null) throw new BusinessException("场景不存在");
        if (hotspot.getType() == null) hotspot.setType("info");
        if (hotspot.getHYaw() == null) hotspot.setHYaw(java.math.BigDecimal.ZERO);
        if (hotspot.getVPitch() == null) hotspot.setVPitch(java.math.BigDecimal.ZERO);
        if (hotspot.getSortOrder() == null) hotspot.setSortOrder(0);
        vrHotspotMapper.insert(hotspot);
        return hotspot;
    }

    public VrHotspot updateHotspot(Long id, VrHotspot hotspot) {
        VrHotspot existing = vrHotspotMapper.selectById(id);
        if (existing == null) throw new BusinessException("热点不存在");
        hotspot.setId(id);
        vrHotspotMapper.updateById(hotspot);
        return vrHotspotMapper.selectById(id);
    }

    public void deleteHotspot(Long id) {
        vrHotspotMapper.deleteById(id);
    }

    private void fillScenes(List<VrScene> scenes) {
        if (scenes.isEmpty()) return;
        List<Long> sceneIds = scenes.stream().map(VrScene::getId).collect(java.util.stream.Collectors.toList());
        Map<Long, List<VrHotspot>> hotspotMap = vrHotspotMapper.selectList(
                new LambdaQueryWrapper<VrHotspot>()
                        .in(VrHotspot::getSceneId, sceneIds)
                        .orderByAsc(VrHotspot::getSortOrder))
                .stream().collect(java.util.stream.Collectors.groupingBy(VrHotspot::getSceneId));
        List<Long> entIds = scenes.stream().map(VrScene::getEnterpriseId)
                .filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, Enterprise> entMap = entIds.isEmpty() ? java.util.Collections.emptyMap()
                : enterpriseMapper.selectBatchIds(entIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Enterprise::getId, e -> e));
        List<Long> baseIds = scenes.stream().map(VrScene::getBaseId)
                .filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, EnterpriseBase> baseMap = baseIds.isEmpty() ? java.util.Collections.emptyMap()
                : baseMapper.selectBatchIds(baseIds).stream()
                        .collect(java.util.stream.Collectors.toMap(EnterpriseBase::getId, b -> b));
        for (VrScene scene : scenes) {
            scene.setHotspots(hotspotMap.getOrDefault(scene.getId(), new ArrayList<>()));
            Enterprise e = entMap.get(scene.getEnterpriseId());
            if (e != null) scene.setEnterpriseName(e.getName());
            EnterpriseBase b = baseMap.get(scene.getBaseId());
            if (b != null) scene.setBaseName(b.getName());
        }
    }

    private void fillScene(VrScene scene) {
        // 填充热点
        List<VrHotspot> hotspots = vrHotspotMapper.selectList(
                new LambdaQueryWrapper<VrHotspot>()
                        .eq(VrHotspot::getSceneId, scene.getId())
                        .orderByAsc(VrHotspot::getSortOrder));
        scene.setHotspots(hotspots != null ? hotspots : new ArrayList<>());
        // 填充企业名称
        if (scene.getEnterpriseId() != null) {
            Enterprise e = enterpriseMapper.selectById(scene.getEnterpriseId());
            if (e != null) scene.setEnterpriseName(e.getName());
        }
        // 填充基地名称
        if (scene.getBaseId() != null) {
            EnterpriseBase b = baseMapper.selectById(scene.getBaseId());
            if (b != null) scene.setBaseName(b.getName());
        }
    }
}
