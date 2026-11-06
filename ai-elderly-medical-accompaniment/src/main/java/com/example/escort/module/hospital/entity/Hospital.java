package com.example.escort.module.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("hospital")
public class Hospital {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private String city;
    private String level;
    private String phone;
    private String departments;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}