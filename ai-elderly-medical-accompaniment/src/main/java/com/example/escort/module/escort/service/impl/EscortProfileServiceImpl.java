package com.example.escort.module.escort.service.impl;

import com.example.escort.common.AesUtil;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.escort.dto.EscortApplyDTO;
import com.example.escort.module.escort.dto.EscortProfileDTO;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.escort.mapper.EscortProfileMapper;
import com.example.escort.module.escort.service.EscortProfileService;
import com.example.escort.module.escort.vo.EscortProfileVO;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.order.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 陪诊师服务实现
 *
 * 声明：身份证号为模拟数据，AES加密存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscortProfileServiceImpl implements EscortProfileService {

    private final EscortProfileMapper escortProfileMapper;
    private final EscortOrderMapper orderMapper;
    private final SettlementMapper settlementMapper;
    private final AesUtil aesUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, EscortApplyDTO dto) {
        // 校验是否已提交过申请
        EscortProfile exist = escortProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, userId));
        if (exist != null) {
            throw new BizException(ResultCode.CONFLICT, "您已提交过资质申请，请勿重复提交");
        }

        EscortProfile profile = new EscortProfile();
        profile.setUserId(userId);
        profile.setRealName(dto.getRealName());
        profile.setGender(dto.getGender());
        profile.setBirthDate(dto.getBirthDate());
        profile.setIdCard(aesUtil.encrypt(dto.getIdCard())); // AES加密
        profile.setPhone(dto.getPhone());
        profile.setServiceCity(dto.getServiceCity());
        profile.setServiceArea(dto.getServiceArea());
        profile.setIntro(dto.getIntro());
        profile.setMaterials(dto.getMaterials());
        profile.setAuditStatus("PENDING");
        escortProfileMapper.insert(profile);

        log.info("陪诊师资质申请已提交: userId={}", userId);
    }

    @Override
    public EscortProfileVO getApplyStatus(Long userId) {
        EscortProfile profile = escortProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "尚未提交资质申请");
        }
        return toVO(profile);
    }

    @Override
    public EscortProfileVO getProfile(Long userId) {
        EscortProfile profile = escortProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "陪诊师信息不存在");
        }
        return toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, EscortProfileDTO dto) {
        EscortProfile profile = escortProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "陪诊师信息不存在");
        }
        if (dto.getServiceCity() != null) profile.setServiceCity(dto.getServiceCity());
        if (dto.getServiceArea() != null) profile.setServiceArea(dto.getServiceArea());
        if (dto.getIntro() != null) profile.setIntro(dto.getIntro());
        escortProfileMapper.updateById(profile);
    }

    private EscortProfileVO toVO(EscortProfile entity) {
        EscortProfileVO vo = new EscortProfileVO();
        vo.setId(entity.getId());
        vo.setRealName(entity.getRealName());
        vo.setGender(entity.getGender());
        if (entity.getBirthDate() != null) {
            vo.setBirthDate(entity.getBirthDate().toString());
        }
        vo.setPhone(entity.getPhone());
        vo.setServiceCity(entity.getServiceCity());
        vo.setServiceArea(entity.getServiceArea());
        vo.setIntro(entity.getIntro());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setAuditTime(entity.getAuditTime());
        vo.setAvgRating(entity.getAvgRating());
        vo.setTotalOrders(entity.getTotalOrders());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    public List<EscortOrder> getOrderPool(Long userId) {
        return orderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortOrder>()
                .eq(EscortOrder::getEscortId, userId).eq(EscortOrder::getStatus, "DISPATCHED")
                .orderByDesc(EscortOrder::getCreateTime));
    }

    @Override
    public Map<String, Object> getStatistics(Long userId) {
        EscortProfile ep = escortProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortProfile>()
                        .eq(EscortProfile::getUserId, userId));
        Map<String, Object> map = new HashMap<>();
        map.put("totalOrders", ep != null ? ep.getTotalOrders() : 0);
        map.put("avgRating", ep != null ? ep.getAvgRating() : BigDecimal.valueOf(5.00));
        return map;
    }
}