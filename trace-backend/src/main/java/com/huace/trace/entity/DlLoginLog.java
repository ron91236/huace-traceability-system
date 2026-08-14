package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_login_log")
public class DlLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    /** 企业名称（非持久化，管理员全局查看时填充） */
    @TableField(exist = false)
    private String enterpriseName;
    private String username;
    private String loginType;
    private String country;
    private String province;
    private String city;
    private LocalDateTime loginTime;
}
