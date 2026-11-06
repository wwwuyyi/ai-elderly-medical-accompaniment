package com.example.escort.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数校验失败 */
    BAD_REQUEST(400, "参数校验失败"),

    /** 未登录或Token失效 */
    UNAUTHORIZED(401, "未登录或Token已失效"),

    /** 角色权限不足 */
    FORBIDDEN(403, "角色权限不足"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 业务状态冲突（含订单非法流转、乐观锁冲突） */
    CONFLICT(409, "业务状态冲突"),

    /** 触发重试上限或限流 */
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    /** 系统内部异常 */
    INTERNAL_ERROR(500, "系统内部异常"),

    /** 外部AI服务调用失败 */
    AI_SERVICE_ERROR(502, "外部AI服务调用失败"),

    /** 外部AI服务降级中 */
    AI_SERVICE_DEGRADED(503, "AI服务暂不可用，请稍后重试");

    private final int code;
    private final String message;
}