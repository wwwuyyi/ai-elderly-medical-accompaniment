package com.example.escort.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充处理器
 *
 * 使实体类中既有的 @TableField(fill = ...) 声明真正生效：
 * - INSERT：填充 createTime（FieldFill.INSERT）与 updateTime（FieldFill.INSERT_UPDATE）
 * - UPDATE：刷新 updateTime（FieldFill.INSERT_UPDATE）
 *
 * 说明：
 * 1. updateTime 虽为 INSERT_UPDATE，但插入时同样必须填充，否则 create_time/update_time
 *    非空约束会抛出 DataIntegrityViolationException（BUG-E）。
 * 2. 字段名严格使用实体属性名 createTime / updateTime，与 ElderlyProfile、EscortOrder、
 *    AiTask 等实体保持一致。
 * 3. 本类位于 com.example.escort.config 包下并标注 @Component，可被 Spring 自动扫描。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // createTime：FieldFill.INSERT
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        // updateTime：FieldFill.INSERT_UPDATE，插入时同样必须填充
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * 更新时自动刷新
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // updateTime：FieldFill.INSERT_UPDATE，每次更新刷新
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}