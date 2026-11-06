package com.example.escort.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.module.order.dto.*;
import com.example.escort.module.order.vo.EstimateVO;
import com.example.escort.module.order.vo.OrderVO;
import com.example.escort.module.order.vo.StatusLogVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /** 费用试算 */
    EstimateVO estimate(OrderEstimateDTO dto);

    /** 下单（幂等+模拟支付） */
    OrderVO create(Long familyId, OrderCreateDTO dto);

    /** 分页（按角色） */
    IPage<OrderVO> page(Long userId, String role, int current, int size, String status);

    /** 订单详情（按角色脱敏） */
    OrderVO getById(Long userId, String role, Long orderId);

    /** 取消订单（T9/T10） */
    void cancel(Long userId, String role, Long orderId, OrderCancelDTO dto);

    /** 管理员派单 */
    void dispatch(Long adminId, Long orderId, Long escortId);

    /** 陪诊师接单 */
    void accept(Long escortId, Long orderId);

    /** 陪诊师拒单 */
    void reject(Long escortId, Long orderId);

    /** 开始服务（T5：预约时间前后30分钟窗口） */
    void start(Long escortId, Long orderId);

    /** 结束服务 */
    void finish(Long escortId, Long orderId);

    /** 结算（管理员，模拟账目） */
    void settle(Long adminId, Long orderId);

    /** 评价（家属） */
    void evaluate(Long familyId, Long orderId, EvaluateDTO dto);

    /** 状态流转日志 */
    List<StatusLogVO> statusLog(Long userId, String role, Long orderId);
}