package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hgz")
public class Hgz extends BaseEntity {
    private String code;
    private Long enterpriseId;
    private Long batchId;
    private Long goodsId;
    /** 主体类型: 1=生产者 2=收购者 */
    private Integer userType;
    private String productName;
    private String number;
    private String placeOfOrigin;
    private String promiseUser;
    private String contact;
    private LocalDate useTime;
    private String signature;
    private String promiseList;
    private String basisList;
    private Integer isShowEnterprise;
    /** 状态: 1=有效 0=作废 */
    private Integer status;
    private String qrUrl;
    private String queryUrl;

    /** 非数据库字段：解析后的承诺事项 */
    @TableField(exist = false)
    private List<Map<String, Object>> promiseItems;
    /** 非数据库字段：解析后的承诺依据 */
    @TableField(exist = false)
    private List<Map<String, Object>> basisItems;
    /** 非数据库字段：二维码Base64 */
    @TableField(exist = false)
    private String qrCode;
    /** 非数据库字段：关联批次名称 */
    @TableField(exist = false)
    private String batchName;
}
