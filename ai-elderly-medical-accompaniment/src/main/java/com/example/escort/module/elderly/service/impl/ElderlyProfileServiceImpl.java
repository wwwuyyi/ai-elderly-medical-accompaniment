package com.example.escort.module.elderly.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.common.AesUtil;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.elderly.dto.ElderlyProfileDTO;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.elderly.service.ElderlyProfileService;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.elderly.vo.ElderlyProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 老人档案服务实现
 *
 * 声明：身份证号、住址为模拟数据，AES加密存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElderlyProfileServiceImpl implements ElderlyProfileService {

    private final ElderlyProfileMapper elderlyProfileMapper;
    private final EscortOrderMapper orderMapper;
    private final AesUtil aesUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long familyId, ElderlyProfileDTO dto) {
        ElderlyProfile profile = new ElderlyProfile();
        profile.setFamilyId(familyId);
        copyDtoToEntity(dto, profile);
        // 身份证号AES加密存储
        profile.setIdCard(aesUtil.encrypt(dto.getIdCard()));
        // 住址AES加密存储
        if (dto.getAddress() != null) {
            profile.setAddress(aesUtil.encrypt(dto.getAddress()));
        }
        elderlyProfileMapper.insert(profile);
        log.info("老人档案创建成功: familyId={}, elderlyName={}", familyId, dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long familyId, Long id, ElderlyProfileDTO dto) {
        ElderlyProfile profile = elderlyProfileMapper.selectById(id);
        if (profile == null || !profile.getFamilyId().equals(familyId)) {
            throw new BizException(ResultCode.NOT_FOUND, "老人档案不存在");
        }
        copyDtoToEntity(dto, profile);
        if (dto.getIdCard() != null) {
            profile.setIdCard(aesUtil.encrypt(dto.getIdCard()));
        }
        if (dto.getAddress() != null) {
            profile.setAddress(aesUtil.encrypt(dto.getAddress()));
        }
        elderlyProfileMapper.updateById(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long familyId, Long id) {
        ElderlyProfile profile = elderlyProfileMapper.selectById(id);
        if (profile == null || !profile.getFamilyId().equals(familyId)) {
            throw new BizException(ResultCode.NOT_FOUND, "老人档案不存在");
        }
        // 检查是否存在进行中订单（非终态）
        Long activeCount = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EscortOrder>()
                        .eq(EscortOrder::getElderlyId, id)
                        .notIn(EscortOrder::getStatus, "COMPLETED", "CANCELLED"));
        if (activeCount > 0) {
            throw new BizException(ResultCode.CONFLICT, "该老人存在进行中的陪诊订单，无法删除档案");
        }
        elderlyProfileMapper.deleteById(id);
        log.info("老人档案已删除: id={}, familyId={}", id, familyId);
    }

    @Override
    public IPage<ElderlyProfileVO> page(Long familyId, int current, int size) {
        Page<ElderlyProfile> page = new Page<>(current, size);
        LambdaQueryWrapper<ElderlyProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyProfile::getFamilyId, familyId)
               .orderByDesc(ElderlyProfile::getCreateTime);
        IPage<ElderlyProfile> resultPage = elderlyProfileMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public ElderlyProfileVO getById(Long familyId, Long id) {
        ElderlyProfile profile = elderlyProfileMapper.selectById(id);
        if (profile == null || !profile.getFamilyId().equals(familyId)) {
            throw new BizException(ResultCode.NOT_FOUND, "老人档案不存在");
        }
        return toVO(profile);
    }

    private void copyDtoToEntity(ElderlyProfileDTO dto, ElderlyProfile entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getBirthDate() != null) entity.setBirthDate(dto.getBirthDate());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getEmergencyContact() != null) entity.setEmergencyContact(dto.getEmergencyContact());
        if (dto.getEmergencyName() != null) entity.setEmergencyName(dto.getEmergencyName());
        if (dto.getMobility() != null) entity.setMobility(dto.getMobility());
        if (dto.getAllergyHistory() != null) entity.setAllergyHistory(dto.getAllergyHistory());
        if (dto.getChronicDisease() != null) entity.setChronicDisease(dto.getChronicDisease());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());
    }

    private ElderlyProfileVO toVO(ElderlyProfile entity) {
        ElderlyProfileVO vo = new ElderlyProfileVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setGender(entity.getGender());
        vo.setBirthDate(entity.getBirthDate());
        // 解密后展示（家属本人可见全量）
        vo.setIdCard(aesUtil.decrypt(entity.getIdCard()));
        vo.setPhone(entity.getPhone());
        vo.setAddress(aesUtil.decrypt(entity.getAddress()));
        vo.setEmergencyContact(entity.getEmergencyContact());
        vo.setEmergencyName(entity.getEmergencyName());
        vo.setMobility(entity.getMobility());
        vo.setAllergyHistory(entity.getAllergyHistory());
        vo.setChronicDisease(entity.getChronicDisease());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}