package com.example.escort.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.escort.mapper.EscortProfileMapper;
import com.example.escort.module.hospital.entity.Hospital;
import com.example.escort.module.hospital.mapper.HospitalMapper;
import com.example.escort.module.order.dto.*;
import com.example.escort.module.order.entity.*;
import com.example.escort.module.order.enums.OrderStatus;
import com.example.escort.module.order.mapper.*;
import com.example.escort.module.order.service.OrderService;
import com.example.escort.module.order.util.OrderNoGenerator;
import com.example.escort.module.order.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * 核心机制：
 * 1. 状态机校验（OrderStatus.validateTransition）
 * 2. 乐观锁并发控制（条件UPDATE + 影响行数校验）
 * 3. 订单号+插入同一重试循环（DuplicateKeyException重试3次）
 * 4. DB唯一索引幂等（uk_request_id，重启不丢失）
 * 5. 模拟支付（下单即PAID，退款均为模拟账目）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final EscortOrderMapper orderMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final SettlementMapper settlementMapper;
    private final EvaluationMapper evaluationMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final ElderlyProfileMapper elderlyMapper;
    private final EscortProfileMapper escortProfileMapper;
    private final HospitalMapper hospitalMapper;
    private final SysUserMapper userMapper;

    @Value("${order.price.base-per-hour:50}")
    private BigDecimal basePricePerHour;
    @Value("${order.price.exam-per-hour:80}")
    private BigDecimal examPricePerHour;
    @Value("${order.cancel.refund-rate:0.8}")
    private BigDecimal cancelRefundRate;
    @Value("${settlement.platform-rate:0.20}")
    private BigDecimal platformRate;

    @Override
    public EstimateVO estimate(OrderEstimateDTO dto) {
        BigDecimal unitPrice = "BASIC".equals(dto.getServiceType()) ? basePricePerHour : examPricePerHour;
        BigDecimal billed = dto.getDuration().compareTo(BigDecimal.ONE) < 0
                ? BigDecimal.ONE : dto.getDuration().setScale(0, RoundingMode.UP);
        BigDecimal total = unitPrice.multiply(billed).setScale(2, RoundingMode.HALF_UP);
        EstimateVO vo = new EstimateVO();
        vo.setServiceType(dto.getServiceType()); vo.setUnitPrice(unitPrice);
        vo.setDuration(billed); vo.setTotalAmount(total);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long familyId, OrderCreateDTO dto) {
        // 校验老人档案归属
        ElderlyProfile elderly = elderlyMapper.selectById(dto.getElderlyId());
        if (elderly == null || !elderly.getFamilyId().equals(familyId))
            throw new BizException(ResultCode.BAD_REQUEST, "老人档案不存在或不属于当前账号");
        // 校验金额
        OrderEstimateDTO estDTO = new OrderEstimateDTO();
        estDTO.setServiceType(dto.getServiceType()); estDTO.setDuration(dto.getDuration());
        if (dto.getAmount().compareTo(estimate(estDTO).getTotalAmount()) != 0)
            throw new BizException(ResultCode.BAD_REQUEST, "金额与试算结果不一致");

        EscortOrder order = buildOrder(familyId, dto);
        // 订单号+insert同一循环（最多3次）
        for (int i = 0; i < 3; i++) {
            try {
                order.setOrderNo(orderNoGenerator.generate());
                orderMapper.insert(order);
                break;
            } catch (DuplicateKeyException e) {
                String msg = e.getMessage();
                if (msg != null && msg.contains("uk_request_id")) {
                    EscortOrder exist = orderMapper.selectOne(
                            new LambdaQueryWrapper<EscortOrder>().eq(EscortOrder::getRequestId, dto.getRequestId()));
                    if (exist != null) return toVO(exist);
                }
                if (i == 2) throw new BizException(ResultCode.TOO_MANY_REQUESTS, "订单创建失败，请稍后重试");
                log.warn("订单号冲突重试 {}/3", i + 1);
            }
        }
        writeStatusLog(order.getId(), order.getOrderNo(), null, "PENDING_DISPATCH", familyId, "FAMILY", "用户下单");
        log.info("订单创建: orderNo={}, familyId={}", order.getOrderNo(), familyId);
        return toVO(order);
    }

    @Override
    public IPage<OrderVO> page(Long userId, String role, int current, int size, String status) {
        Page<EscortOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<EscortOrder> w = new LambdaQueryWrapper<>();
        if ("FAMILY".equals(role)) w.eq(EscortOrder::getFamilyId, userId);
        else if ("ESCORT".equals(role)) w.eq(EscortOrder::getEscortId, userId);
        if (status != null && !status.isEmpty()) w.eq(EscortOrder::getStatus, status);
        w.orderByDesc(EscortOrder::getCreateTime);
        return orderMapper.selectPage(page, w).convert(this::toVO);
    }

    @Override
    public OrderVO getById(Long userId, String role, Long orderId) {
        EscortOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        checkOwnership(userId, role, order);
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String role, Long orderId, OrderCancelDTO dto) {
        EscortOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        if ("FAMILY".equals(role) && !order.getFamilyId().equals(userId))
            throw new BizException(ResultCode.FORBIDDEN);
        OrderStatus cur = OrderStatus.valueOf(order.getStatus());
        try { OrderStatus.validateTransition(cur, OrderStatus.CANCELLED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        if (cur == OrderStatus.ACCEPTED) {
            if (dto.getCancelReason() == null || dto.getCancelReason().trim().isEmpty())
                throw new BizException(ResultCode.BAD_REQUEST, "取消原因不能为空");
            if (ChronoUnit.HOURS.between(LocalDateTime.now(), order.getAppointmentTime()) < 2)
                throw new BizException(ResultCode.CONFLICT, "距预约不足2小时不可取消");
        }
        if (cur == OrderStatus.PENDING_DISPATCH || cur == OrderStatus.DISPATCHED) {
            order.setRefundAmount(order.getAmount()); order.setPayStatus("REFUNDED");
        } else if (cur == OrderStatus.ACCEPTED) {
            order.setRefundAmount(order.getAmount().multiply(cancelRefundRate).setScale(2, RoundingMode.HALF_UP));
            order.setPayStatus("PARTIAL_REFUNDED");
        }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur.name())
                        .eq(EscortOrder::getVersion, order.getVersion())
                        .set(EscortOrder::getStatus, "CANCELLED")
                        .set(EscortOrder::getPayStatus, order.getPayStatus())
                        .set(EscortOrder::getRefundAmount, order.getRefundAmount())
                        .set(EscortOrder::getCancelReason, dto.getCancelReason())
                        .setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, order.getOrderNo(), cur.name(), "CANCELLED", userId, role, dto.getCancelReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(Long adminId, Long orderId, Long escortId) {
        EscortOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        EscortProfile ep = escortProfileMapper.selectOne(
                new LambdaQueryWrapper<EscortProfile>().eq(EscortProfile::getUserId, escortId));
        if (ep == null || !"APPROVED".equals(ep.getAuditStatus()))
            throw new BizException(ResultCode.BAD_REQUEST, "陪诊师未通过审核");
        SysUser eu = userMapper.selectById(escortId);
        if (eu == null || "DISABLED".equals(eu.getStatus()))
            throw new BizException(ResultCode.BAD_REQUEST, "陪诊师账号已禁用");
        String cur = order.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.DISPATCHED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, order.getVersion())
                        .set(EscortOrder::getEscortId, escortId).set(EscortOrder::getStatus, "DISPATCHED")
                        .setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, order.getOrderNo(), cur, "DISPATCHED", adminId, "ADMIN", "派单给:" + escortId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long escortId, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null || !escortId.equals(o.getEscortId()))
            throw new BizException(ResultCode.FORBIDDEN, "订单未指派给您");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.ACCEPTED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getStatus, "ACCEPTED").setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, o.getOrderNo(), cur, "ACCEPTED", escortId, "ESCORT", "接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long escortId, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null || !escortId.equals(o.getEscortId()))
            throw new BizException(ResultCode.FORBIDDEN, "订单未指派给您");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.PENDING_DISPATCH); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getEscortId, null).set(EscortOrder::getStatus, "PENDING_DISPATCH")
                        .setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, o.getOrderNo(), cur, "PENDING_DISPATCH", escortId, "ESCORT", "拒单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long escortId, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null || !escortId.equals(o.getEscortId()))
            throw new BizException(ResultCode.FORBIDDEN, "订单未指派给您");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.IN_SERVICE); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        if (Math.abs(ChronoUnit.MINUTES.between(LocalDateTime.now(), o.getAppointmentTime())) > 30)
            throw new BizException(ResultCode.CONFLICT, "不在服务时间窗口（预约±30分钟）");
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getStatus, "IN_SERVICE").setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, o.getOrderNo(), cur, "IN_SERVICE", escortId, "ESCORT", "开始服务");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long escortId, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null || !escortId.equals(o.getEscortId()))
            throw new BizException(ResultCode.FORBIDDEN, "订单未指派给您");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.SERVICE_FINISHED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getStatus, "SERVICE_FINISHED").setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        writeStatusLog(orderId, o.getOrderNo(), cur, "SERVICE_FINISHED", escortId, "ESCORT", "服务结束");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Long adminId, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null) throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        if (o.getEscortId() == null) throw new BizException(ResultCode.BAD_REQUEST, "未分配陪诊师");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.SETTLED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        BigDecimal pf = o.getAmount().multiply(platformRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ei = o.getAmount().subtract(pf);
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getStatus, "SETTLED").setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        Settlement s = new Settlement();
        s.setOrderId(orderId); s.setOrderNo(o.getOrderNo()); s.setEscortId(o.getEscortId());
        s.setOrderAmount(o.getAmount()); s.setPlatformRate(platformRate);
        s.setPlatformFee(pf); s.setEscortIncome(ei); s.setSettleStatus("SETTLED"); s.setOperatorId(adminId);
        settlementMapper.insert(s);
        writeStatusLog(orderId, o.getOrderNo(), cur, "SETTLED", adminId, "ADMIN", "结算完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long familyId, Long orderId, EvaluateDTO dto) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null || !familyId.equals(o.getFamilyId()))
            throw new BizException(ResultCode.FORBIDDEN, "订单不属于您");
        String cur = o.getStatus();
        try { OrderStatus.validateTransition(OrderStatus.valueOf(cur), OrderStatus.COMPLETED); }
        catch (IllegalStateException e) { throw new BizException(ResultCode.CONFLICT, e.getMessage()); }
        Evaluation ev = new Evaluation();
        ev.setOrderId(orderId); ev.setOrderNo(o.getOrderNo());
        ev.setFamilyId(familyId); ev.setEscortId(o.getEscortId());
        ev.setScore(dto.getScore()); ev.setContent(dto.getContent());
        try { evaluationMapper.insert(ev); }
        catch (DuplicateKeyException e) { throw new BizException(ResultCode.CONFLICT, "该订单已评价"); }
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<EscortOrder>()
                        .eq(EscortOrder::getId, orderId).eq(EscortOrder::getStatus, cur)
                        .eq(EscortOrder::getVersion, o.getVersion())
                        .set(EscortOrder::getStatus, "COMPLETED").setSql("version = version + 1"));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
        updateEscortRating(o.getEscortId());
        writeStatusLog(orderId, o.getOrderNo(), cur, "COMPLETED", familyId, "FAMILY", "评价:" + dto.getScore() + "分");
    }

    @Override
    public List<StatusLogVO> statusLog(Long userId, String role, Long orderId) {
        EscortOrder order = orderMapper.selectById(orderId);
        if (order != null) checkOwnership(userId, role, order);
        return statusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreateTime))
                .stream().map(log -> {
                    StatusLogVO vo = new StatusLogVO();
                    vo.setId(log.getId()); vo.setFromStatus(log.getFromStatus());
                    vo.setToStatus(log.getToStatus()); vo.setOperatorRole(log.getOperatorRole());
                    vo.setRemark(log.getRemark()); vo.setCreateTime(log.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());
    }

    // ---- private helpers ----

    private EscortOrder buildOrder(Long familyId, OrderCreateDTO dto) {
        EscortOrder o = new EscortOrder();
        o.setRequestId(dto.getRequestId()); o.setFamilyId(familyId);
        o.setElderlyId(dto.getElderlyId()); o.setHospitalId(dto.getHospitalId());
        o.setDepartment(dto.getDepartment()); o.setServiceType(dto.getServiceType());
        o.setAppointmentTime(dto.getAppointmentTime()); o.setDuration(dto.getDuration());
        o.setAmount(dto.getAmount()); o.setStatus("PENDING_DISPATCH"); o.setVersion(0);
        o.setPayStatus("PAID"); o.setPayTime(LocalDateTime.now());
        return o;
    }

    private void checkOwnership(Long userId, String role, EscortOrder order) {
        if ("FAMILY".equals(role) && !order.getFamilyId().equals(userId))
            throw new BizException(ResultCode.FORBIDDEN);
        if ("ESCORT".equals(role) && (order.getEscortId() == null || !order.getEscortId().equals(userId)))
            throw new BizException(ResultCode.FORBIDDEN);
    }

    private void writeStatusLog(Long orderId, String orderNo, String from, String to,
                                Long opId, String opRole, String remark) {
        OrderStatusLog e = new OrderStatusLog();
        e.setOrderId(orderId); e.setOrderNo(orderNo);
        e.setFromStatus(from); e.setToStatus(to);
        e.setOperatorId(opId); e.setOperatorRole(opRole); e.setRemark(remark);
        statusLogMapper.insert(e);
    }

    private void updateEscortRating(Long escortId) {
        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getEscortId, escortId).eq(Evaluation::getIsHidden, 0));
        if (evals.isEmpty()) return;
        BigDecimal avg = evals.stream().map(e -> BigDecimal.valueOf(e.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
        escortProfileMapper.update(null,
                new LambdaUpdateWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, escortId)
                        .set(EscortProfile::getAvgRating, avg)
                        .setSql("total_orders = total_orders + 1"));
    }

    private OrderVO toVO(EscortOrder o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId()); vo.setOrderNo(o.getOrderNo());
        vo.setFamilyId(o.getFamilyId()); vo.setElderlyId(o.getElderlyId());
        vo.setEscortId(o.getEscortId());
        vo.setDepartment(o.getDepartment()); vo.setServiceType(o.getServiceType());
        vo.setAppointmentTime(o.getAppointmentTime()); vo.setDuration(o.getDuration());
        vo.setAmount(o.getAmount()); vo.setStatus(o.getStatus());
        vo.setPayStatus(o.getPayStatus()); vo.setRefundAmount(o.getRefundAmount());
        vo.setCancelReason(o.getCancelReason()); vo.setCreateTime(o.getCreateTime());
        if (o.getElderlyId() != null) {
            ElderlyProfile e = elderlyMapper.selectById(o.getElderlyId());
            if (e != null) vo.setElderlyName(e.getName());
        }
        if (o.getEscortId() != null) {
            SysUser u = userMapper.selectById(o.getEscortId());
            if (u != null) vo.setEscortName(u.getNickname());
        }
        if (o.getHospitalId() != null) {
            Hospital h = hospitalMapper.selectById(o.getHospitalId());
            if (h != null) vo.setHospitalName(h.getName());
        }
        return vo;
    }
}