package com.example.escort.module.order.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态枚举
 *
 * 状态流转：PENDING_DISPATCH → DISPATCHED → ACCEPTED → IN_SERVICE
 *            → SERVICE_FINISHED → SETTLED → COMPLETED
 * 旁路终态：CANCELLED
 *
 * 6个进行态 + 2个终态（COMPLETED、CANCELLED）
 */
@Getter
public enum OrderStatus {

    PENDING_DISPATCH("待派单"),
    DISPATCHED("已派单待接单"),
    ACCEPTED("已接单待服务"),
    IN_SERVICE("服务中"),
    SERVICE_FINISHED("服务结束待结算"),
    SETTLED("已结算待评价"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /** 每个状态允许流转到的目标状态集合 */
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new java.util.HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(PENDING_DISPATCH, EnumSet.of(DISPATCHED, CANCELLED));   // T2派单, T9取消
        ALLOWED_TRANSITIONS.put(DISPATCHED, EnumSet.of(ACCEPTED, PENDING_DISPATCH, CANCELLED)); // T3接单, T4拒单, T9取消
        ALLOWED_TRANSITIONS.put(ACCEPTED, EnumSet.of(IN_SERVICE, CANCELLED));          // T5开始服务, T10取消
        ALLOWED_TRANSITIONS.put(IN_SERVICE, EnumSet.of(SERVICE_FINISHED));              // T6结束服务
        ALLOWED_TRANSITIONS.put(SERVICE_FINISHED, EnumSet.of(SETTLED));                 // T7结算
        ALLOWED_TRANSITIONS.put(SETTLED, EnumSet.of(COMPLETED));                        // T8评价
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(OrderStatus.class));          // 终态
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));          // 终态
    }

    /**
     * 校验状态流转是否合法
     * @param from 当前状态
     * @param to   目标状态
     * @throws IllegalStateException 非法流转
     */
    public static void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalStateException(
                    String.format("订单当前状态(%s:%s)不允许流转到(%s:%s)", from.name(), from.getDescription(), to.name(), to.getDescription()));
        }
    }
}