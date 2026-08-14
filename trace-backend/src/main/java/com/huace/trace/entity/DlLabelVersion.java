package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_label_version")
public class DlLabelVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String versionNo;
    private String foodName;
    private String barcode;
    private String ingredients;
    private String spec;
    private String netContent;
    private String foodImages;
    private String nutritionImage;
    private String foodCategory;
    private String shelfLife;
    private String productionDateLabel;
    private String expiryDateLabel;
    private String licenseNo;
    private String standardCode;
    private String qualityGrade;
    private String storageCondition;
    private String gmoFood;
    private String irradiatedFood;
    private String quantityLabel;
    private String batchNoLabel;
    private String allergens;
    private String consumptionMethod;
    private String introVideo;
    private String certificates;
    /** JSON字符串：自定义扩展字段 */
    private String customFields;
    /** JSON字符串：生产信息数组 */
    private String productionInfo;
    private String versionDesc;
    private String status;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 非数据库字段：二维码Base64 */
    @TableField(exist = false)
    private String qrCode;
    /** 非数据库字段：扫码访问URL */
    @TableField(exist = false)
    private String scanUrl;
}
