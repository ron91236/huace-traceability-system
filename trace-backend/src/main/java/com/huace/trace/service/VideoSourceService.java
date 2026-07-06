package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.VideoSource;
import com.huace.trace.mapper.VideoSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoSourceService {

    private final VideoSourceMapper videoSourceMapper;

    public List<VideoSource> listByEnterprise(Long enterpriseId, Long baseId, Long batchId) {
        LambdaQueryWrapper<VideoSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoSource::getEnterpriseId, enterpriseId)
               .eq(VideoSource::getStatus, 1);
        if (baseId != null) wrapper.and(w -> w.eq(VideoSource::getBaseId, baseId).or().isNull(VideoSource::getBaseId));
        if (batchId != null) wrapper.and(w -> w.eq(VideoSource::getBatchId, batchId).or().isNull(VideoSource::getBatchId));
        wrapper.orderByAsc(VideoSource::getSortOrder);
        return videoSourceMapper.selectList(wrapper);
    }

    public VideoSource createVideoSource(VideoSource source) {
        videoSourceMapper.insert(source);
        return source;
    }

    public VideoSource updateVideoSource(Long id, VideoSource source) {
        source.setId(id);
        videoSourceMapper.updateById(source);
        return videoSourceMapper.selectById(id);
    }

    public void deleteVideoSource(Long id) {
        videoSourceMapper.deleteById(id);
    }

    public VideoSource getById(Long id) {
        return videoSourceMapper.selectById(id);
    }

    /**
     * C端溯源页面精简数据
     */
    public List<Map<String, Object>> listForTrace(Long enterpriseId, Long baseId, Long batchId) {
        if (enterpriseId == null) return Collections.emptyList();
        List<VideoSource> sources = listByEnterprise(enterpriseId, baseId, batchId);
        return sources.stream().map(vs -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", vs.getId());
            m.put("cameraName", vs.getCameraName());
            m.put("streamUrl", vs.getStreamUrl());
            m.put("streamType", vs.getStreamType());
            m.put("coverImage", vs.getCoverImage());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 管理端查询全部企业视频源
     */
    public List<VideoSource> listAll() {
        return videoSourceMapper.selectList(
                new LambdaQueryWrapper<VideoSource>().orderByDesc(VideoSource::getCreatedAt));
    }
}
