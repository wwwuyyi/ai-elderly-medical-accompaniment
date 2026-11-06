package com.example.escort.module.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.record.dto.MedicalRecordDTO;
import com.example.escort.module.record.entity.MedicalDocument;
import com.example.escort.module.record.entity.MedicalRecord;
import com.example.escort.module.record.mapper.MedicalDocumentMapper;
import com.example.escort.module.record.mapper.MedicalRecordMapper;
import com.example.escort.module.record.service.MedicalRecordService;
import com.example.escort.module.record.vo.MedicalDocumentVO;
import com.example.escort.module.record.vo.MedicalRecordVO;
import com.example.escort.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper recordMapper;
    private final MedicalDocumentMapper documentMapper;
    private final ElderlyProfileMapper elderlyMapper;
    private final EscortOrderMapper orderMapper;

    // ========== 档案CRUD（FAMILY） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecordVO create(Long userId, String role, MedicalRecordDTO dto) {
        if (!"FAMILY".equals(role)) throw new BizException(ResultCode.FORBIDDEN);
        ElderlyProfile e = elderlyMapper.selectById(dto.getElderlyId());
        if (e == null || !e.getFamilyId().equals(userId))
            throw new BizException(ResultCode.BAD_REQUEST, "老人档案不属于您");
        MedicalRecord r = buildEntity(dto);
        recordMapper.insert(r);
        return toVO(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecordVO update(Long userId, String role, Long id, MedicalRecordDTO dto) {
        if (!"FAMILY".equals(role)) throw new BizException(ResultCode.FORBIDDEN);
        MedicalRecord r = recordMapper.selectById(id);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        if (dto.getHospitalId() != null) r.setHospitalId(dto.getHospitalId());
        if (dto.getDepartment() != null) r.setDepartment(dto.getDepartment());
        if (dto.getVisitDate() != null) r.setVisitDate(dto.getVisitDate());
        if (dto.getChiefComplaint() != null) r.setChiefComplaint(dto.getChiefComplaint());
        if (dto.getDiagnosis() != null) r.setDiagnosis(dto.getDiagnosis());
        if (dto.getPrescription() != null) r.setPrescription(dto.getPrescription());
        if (dto.getRemark() != null) r.setRemark(dto.getRemark());
        recordMapper.updateById(r);
        return toVO(r);
    }

    @Override
    public IPage<MedicalRecordVO> page(Long userId, String role, Long elderlyId, int current, int size) {
        Page<MedicalRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<MedicalRecord> w = new LambdaQueryWrapper<>();
        if ("FAMILY".equals(role)) {
            List<Long> ids = elderlyMapper.selectList(new LambdaQueryWrapper<ElderlyProfile>()
                    .eq(ElderlyProfile::getFamilyId, userId))
                    .stream().map(ElderlyProfile::getId).collect(Collectors.toList());
            if (ids.isEmpty()) return new Page<>(current, size, 0);
            w.in(MedicalRecord::getElderlyId, ids);
        } else if ("ESCORT".equals(role)) {
            List<Long> ids = orderMapper.selectList(new LambdaQueryWrapper<EscortOrder>()
                    .eq(EscortOrder::getEscortId, userId))
                    .stream().map(EscortOrder::getElderlyId).distinct().collect(Collectors.toList());
            if (ids.isEmpty()) return new Page<>(current, size, 0);
            w.in(MedicalRecord::getElderlyId, ids);
        }
        if (elderlyId != null) w.eq(MedicalRecord::getElderlyId, elderlyId);
        w.orderByDesc(MedicalRecord::getVisitDate);
        return recordMapper.selectPage(page, w).convert(this::toVO);
    }

    @Override
    public MedicalRecordVO getById(Long userId, String role, Long id) {
        MedicalRecord r = recordMapper.selectById(id);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        checkVisibility(userId, role, r);
        return toVO(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String role, Long id) {
        if (!"FAMILY".equals(role)) throw new BizException(ResultCode.FORBIDDEN);
        MedicalRecord r = recordMapper.selectById(id);
        if (r == null) return;
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        recordMapper.deleteById(id);
        documentMapper.delete(new LambdaQueryWrapper<MedicalDocument>().eq(MedicalDocument::getRecordId, id));
    }

    // ========== 单据（ESCORT上传 + 各角色查看） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalDocumentVO uploadDocument(Long userId, String role, Long recordId, String docType, String imageUrl) {
        if (!"ESCORT".equals(role)) throw new BizException(ResultCode.FORBIDDEN);
        // 1. 档案必须存在
        MedicalRecord r = recordMapper.selectById(recordId);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "就医档案不存在");
        // 2. 陪诊师必须有该老人IN_SERVICE状态的订单
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                .eq(EscortOrder::getEscortId, userId)
                .eq(EscortOrder::getElderlyId, r.getElderlyId())
                .eq(EscortOrder::getStatus, "IN_SERVICE"));
        if (count == 0) throw new BizException(ResultCode.FORBIDDEN, "仅可在服务中的订单关联档案上传单据");
        MedicalDocument d = new MedicalDocument();
        d.setRecordId(recordId); d.setDocType(docType); d.setImageUrl(imageUrl);
        d.setOcrStatus("PENDING");
        documentMapper.insert(d);
        return toDocVO(d);
    }

    @Override
    public List<MedicalDocumentVO> getDocuments(Long userId, String role, Long recordId) {
        MedicalRecord r = recordMapper.selectById(recordId);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "就医档案不存在");
        checkVisibility(userId, role, r);
        return documentMapper.selectList(new LambdaQueryWrapper<MedicalDocument>()
                .eq(MedicalDocument::getRecordId, recordId))
                .stream().map(this::toDocVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long userId, String role, Long documentId) {
        MedicalDocument doc = documentMapper.selectById(documentId);
        if (doc == null) return;
        MedicalRecord r = recordMapper.selectById(doc.getRecordId());
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        // 校验单据→档案→老人→家属链
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (!"FAMILY".equals(role)) throw new BizException(ResultCode.FORBIDDEN);
        if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        documentMapper.deleteById(documentId);
    }

    // ========== private helpers ==========

    private MedicalRecord buildEntity(MedicalRecordDTO dto) {
        MedicalRecord r = new MedicalRecord();
        r.setElderlyId(dto.getElderlyId()); r.setOrderId(dto.getOrderId());
        r.setHospitalId(dto.getHospitalId()); r.setDepartment(dto.getDepartment());
        r.setVisitDate(dto.getVisitDate()); r.setChiefComplaint(dto.getChiefComplaint());
        r.setDiagnosis(dto.getDiagnosis()); r.setPrescription(dto.getPrescription());
        r.setRemark(dto.getRemark());
        return r;
    }

    private void checkVisibility(Long userId, String role, MedicalRecord r) {
        if ("FAMILY".equals(role)) {
            ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
            if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        } else if ("ESCORT".equals(role)) {
            Long c = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                    .eq(EscortOrder::getEscortId, userId).eq(EscortOrder::getElderlyId, r.getElderlyId()));
            if (c == 0) throw new BizException(ResultCode.FORBIDDEN);
            // 检查窗口期：是否存在ACCEPTED或IN_SERVICE状态的订单
            Long windowCount = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                    .eq(EscortOrder::getEscortId, userId)
                    .eq(EscortOrder::getElderlyId, r.getElderlyId())
                    .in(EscortOrder::getStatus, "ACCEPTED", "IN_SERVICE"));
            UserContext.setPhoneFullAccess(windowCount > 0);
        }
    }

    private MedicalRecordVO toVO(MedicalRecord r) {
        MedicalRecordVO vo = new MedicalRecordVO();
        vo.setId(r.getId()); vo.setElderlyId(r.getElderlyId());
        vo.setOrderId(r.getOrderId()); vo.setHospitalId(r.getHospitalId());
        vo.setDepartment(r.getDepartment()); vo.setVisitDate(r.getVisitDate());
        vo.setChiefComplaint(r.getChiefComplaint()); vo.setDiagnosis(r.getDiagnosis());
        vo.setPrescription(r.getPrescription()); vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (e != null) {
            vo.setElderlyName(e.getName());
            vo.setPhone(e.getPhone());
        }
        vo.setDocuments(documentMapper.selectList(new LambdaQueryWrapper<MedicalDocument>()
                .eq(MedicalDocument::getRecordId, r.getId()))
                .stream().map(this::toDocVO).collect(Collectors.toList()));
        return vo;
    }

    private MedicalDocumentVO toDocVO(MedicalDocument d) {
        MedicalDocumentVO vo = new MedicalDocumentVO();
        vo.setId(d.getId()); vo.setRecordId(d.getRecordId());
        vo.setDocType(d.getDocType()); vo.setImageUrl(d.getImageUrl());
        vo.setOcrStatus(d.getOcrStatus()); vo.setOcrResult(d.getOcrResult());
        vo.setOcrConfidence(d.getOcrConfidence()); vo.setManualConfirmed(d.getManualConfirmed());
        vo.setCreateTime(d.getCreateTime());
        return vo;
    }
}