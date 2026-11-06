package com.example.escort.module.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.module.record.dto.MedicalRecordDTO;
import com.example.escort.module.record.vo.MedicalDocumentVO;
import com.example.escort.module.record.vo.MedicalRecordVO;
import java.util.List;

public interface MedicalRecordService {
    MedicalRecordVO create(Long userId, String role, MedicalRecordDTO dto);
    MedicalRecordVO update(Long userId, String role, Long id, MedicalRecordDTO dto);
    IPage<MedicalRecordVO> page(Long userId, String role, Long elderlyId, int current, int size);
    MedicalRecordVO getById(Long userId, String role, Long id);
    void delete(Long userId, String role, Long id);
    MedicalDocumentVO uploadDocument(Long userId, String role, Long recordId, String docType, String imageUrl);
    List<MedicalDocumentVO> getDocuments(Long userId, String role, Long recordId);
    void deleteDocument(Long userId, String role, Long documentId);
}