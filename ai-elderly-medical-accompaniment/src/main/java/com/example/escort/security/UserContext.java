package com.example.escort.security;

/**
 * 用户上下文（ThreadLocal）
 * 存储当前请求的用户ID与角色，LoginInterceptor 中 set，afterCompletion 中 remove。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();
    /** 窗口期标记：ESCORT角色查询档案时，由Service层设置。true=当前陪诊师有该老人的ACCEPTED/IN_SERVICE订单 */
    private static final ThreadLocal<Boolean> PHONE_FULL_ACCESS = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUserRole(String role) {
        USER_ROLE.set(role);
    }

    public static String getUserRole() {
        return USER_ROLE.get();
    }

    /** Service层设置：当前ESCORT在窗口期内可看老人电话全量 */
    public static void setPhoneFullAccess(boolean access) {
        PHONE_FULL_ACCESS.set(access);
    }

    /** 序列化器读取：窗口期内返回true→PHONE全量 */
    public static boolean isPhoneFullAccess() {
        return Boolean.TRUE.equals(PHONE_FULL_ACCESS.get());
    }

    /**
     * 清除 ThreadLocal，防止线程池复用导致上下文泄漏。
     * 必须在拦截器 afterCompletion 中调用。
     */
    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
        PHONE_FULL_ACCESS.remove();
    }
}