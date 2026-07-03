package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Long roleId;
    private Long enterpriseId;
    private String userType;
    private Integer status;
    private Long parentUserId;
    private String accountLevel;
}
