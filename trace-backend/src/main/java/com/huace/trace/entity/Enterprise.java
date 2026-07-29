package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise")
public class Enterprise extends BaseEntity {
    private Long parentId;
    private String accountLevel;
    private String name;
    private String nature;
    private String industry;
    private String contact;
    private String phone;
    private String accountType;
    private String loginAccount;
    private String loginPasswordHash;
    @TableField(exist = false)
    private String loginPassword;
    private Integer status;
    private String creditCode;
    private String email;
    private String licenseImage;
    private String enterpriseImage;
    private String mainType;
    private Integer showOuterPackage;
    private Integer totalStaff;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String honors;
    private String zipcode;
    private String standardSystem;
    private String introduction;
    private String qualifications;
    private String sealImage;
    private String promoVideo;
    private String assignedTemplateIds;
}
