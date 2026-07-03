package com.huace.trace.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB 码包明细文档（亿级码包外部存储）
 */
@Data
@Document(collection = "code_package_item")
public class CodePackageItemMongo {

    @Id
    private String id;

    /** 对应 MySQL 码包明细 ID */
    private Long itemId;

    /** 所属码包 */
    @Indexed
    private Long packageId;

    /** 流水号 - 扫码查询主键 */
    @Indexed(unique = true)
    private String serialNo;

    /** 防伪码 */
    private String antiFakeCode;

    /** 溯源网址 */
    private String url;

    /** 绑定状态: UNBOUND / BOUND / WASTE */
    @Indexed
    private String bindStatus;

    /** 绑定企业 */
    @Indexed
    private Long enterpriseId;

    private Long goodsId;
    private Long certId;
    private Long batchId;

    /** 溯源模板标识 */
    private String traceTemplate;

    private Long orderCodeId;

    private LocalDateTime bindTime;

    /** 扫码验证次数 */
    private Integer scanCount;

    /** 冗余企业/商品/批次名称，减少 JOIN 查询 */
    private String enterpriseName;
    private String goodsName;
    private String batchName;

    private LocalDateTime createdAt;
}
