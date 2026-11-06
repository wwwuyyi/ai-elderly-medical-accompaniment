package com.example.escort.security;

import java.lang.annotation.*;

/**
 * 角色权限注解
 * 标注在Controller方法上，由 RequireRoleAspect 切面校验当前用户角色。
 * 角色值：FAMILY / ESCORT / ADMIN
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RequireRole {

    /**
     * 允许访问的角色列表，满足其一即可。
     */
    String[] value();
}