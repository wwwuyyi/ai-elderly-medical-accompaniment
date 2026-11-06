package com.example.escort.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.common.AesUtil;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.admin.service.AdminService;
import com.example.escort.module.admin.vo.DashboardVO;
import com.example.escort.module.admin.vo.EscortAuditDetailVO;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.escort.mapper.EscortProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.entity.Evaluation;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.order.mapper.EvaluationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final EscortOrderMapper orderMapper;
    private final EscortProfileMapper escortMapper;
    private final SysUserMapper userMapper;
    private final EvaluationMapper evalMapper;
    private final AesUtil aesUtil;

    @Override
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        vo.setTodayOrders(orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>().ge(EscortOrder::getCreateTime, today)));
        vo.setPendingAuditCount(escortMapper.selectCount(new LambdaQueryWrapper<EscortProfile>().eq(EscortProfile::getAuditStatus, "PENDING")));
        vo.setTotalUsers(userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0)));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Long cnt = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                    .ge(EscortOrder::getCreateTime, day.atStartOfDay())
                    .lt(EscortOrder::getCreateTime, day.plusDays(1).atStartOfDay()));
            trend.add(Map.of("date", day.toString(), "count", cnt));
        }
        vo.setOrderTrend7Days(trend);
        return vo;
    }

    @Override
    public IPage<EscortProfile> auditPage(int current, int size, String status) {
        return escortMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<EscortProfile>()
                        .eq(status != null && !status.isEmpty(), EscortProfile::getAuditStatus, status)
                        .orderByAsc(EscortProfile::getCreateTime));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminId, Long escortUserId, String status, String remark) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status))
            throw new BizException(ResultCode.BAD_REQUEST, "审核状态仅支持APPROVED或REJECTED");
        if ("REJECTED".equals(status) && (remark == null || remark.trim().isEmpty()))
            throw new BizException(ResultCode.BAD_REQUEST, "驳回时审核备注不能为空");
        int rows = escortMapper.update(null, new LambdaUpdateWrapper<EscortProfile>()
                .eq(EscortProfile::getUserId, escortUserId)
                .eq(EscortProfile::getAuditStatus, "PENDING")
                .set(EscortProfile::getAuditStatus, status)
                .set(EscortProfile::getAuditRemark, remark)
                .set(EscortProfile::getAuditTime, LocalDateTime.now())
                .set(EscortProfile::getAuditorId, adminId));
        if (rows == 0) throw new BizException(ResultCode.CONFLICT, "该申请不是待审核状态");
    }

    @Override
    public EscortAuditDetailVO getAuditDetail(Long escortUserId) {
        EscortProfile ep = escortMapper.selectOne(
                new LambdaQueryWrapper<EscortProfile>().eq(EscortProfile::getUserId, escortUserId));
        if (ep == null) throw new BizException(ResultCode.NOT_FOUND, "陪诊师资质信息不存在");
        EscortAuditDetailVO vo = new EscortAuditDetailVO();
        vo.setId(ep.getId()); vo.setUserId(ep.getUserId());
        vo.setRealName(ep.getRealName()); vo.setGender(ep.getGender());
        vo.setBirthDate(ep.getBirthDate());
        // AES解密身份证号——仅审核场景返回明文，该VO不缓存不落地
        vo.setIdCard(aesUtil.decrypt(ep.getIdCard()));
        vo.setPhone(ep.getPhone());
        vo.setServiceCity(ep.getServiceCity()); vo.setServiceArea(ep.getServiceArea());
        vo.setIntro(ep.getIntro()); vo.setMaterials(ep.getMaterials());
        vo.setAuditStatus(ep.getAuditStatus()); vo.setAuditRemark(ep.getAuditRemark());
        vo.setAuditTime(ep.getAuditTime()); vo.setAuditorId(ep.getAuditorId());
        vo.setAvgRating(ep.getAvgRating()); vo.setTotalOrders(ep.getTotalOrders());
        vo.setCreateTime(ep.getCreateTime());
        return vo;
    }

    @Override
    public IPage<SysUser> userPage(int current, int size, String keyword) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.and(wr -> wr.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        w.orderByDesc(SysUser::getCreateTime);
        return userMapper.selectPage(new Page<>(current, size), w);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId, String status) {
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status))
            throw new BizException(ResultCode.BAD_REQUEST, "状态仅支持ENABLED或DISABLED");
        SysUser u = userMapper.selectById(userId);
        if (u != null && "ADMIN".equals(u.getRole()) && "DISABLED".equals(status))
            throw new BizException(ResultCode.BAD_REQUEST, "禁止禁用管理员账号");
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId).set(SysUser::getStatus, status));
    }

    @Override
    public IPage<EscortOrder> orderPage(int current, int size, String status, String keyword) {
        LambdaQueryWrapper<EscortOrder> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) w.eq(EscortOrder::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) w.like(EscortOrder::getOrderNo, keyword);
        w.orderByDesc(EscortOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(current, size), w);
    }

    @Override
    public IPage<Evaluation> evaluationPage(int current, int size) {
        return evalMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Evaluation>().orderByDesc(Evaluation::getCreateTime));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void hideEvaluation(Long evalId, String reason) {
        evalMapper.update(null, new LambdaUpdateWrapper<Evaluation>()
                .eq(Evaluation::getId, evalId).set(Evaluation::getIsHidden, 1).set(Evaluation::getHideReason, reason));
    }
}