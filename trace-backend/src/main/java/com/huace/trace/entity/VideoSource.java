package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_source")
public class VideoSource extends BaseEntity {
    private Long enterpriseId;
    private Long baseId;
    private Long batchId;
    private String cameraName;
    private String streamUrl;
    private String streamType;
    private String coverImage;
    private String platform;
    private String deviceId;
    private Integer status;
    private Integer sortOrder;
}
