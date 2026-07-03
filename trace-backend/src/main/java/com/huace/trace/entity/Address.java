package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("address")
public class Address {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String contact;
    private String phone;
    private String address;
    private String zipcode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
