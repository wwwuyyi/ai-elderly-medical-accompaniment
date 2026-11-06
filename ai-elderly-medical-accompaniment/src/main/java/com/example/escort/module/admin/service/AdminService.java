package com.example.escort.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.module.admin.vo.DashboardVO;
import com.example.escort.module.admin.vo.EscortAuditDetailVO;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.entity.Evaluation;

public interface AdminService {
    DashboardVO dashboard();
    IPage<EscortProfile> auditPage(int current, int size, String auditStatus);
    void audit(Long adminId, Long escortUserId, String status, String remark);
    /** 审核详情：含AES解密后的身份证号明文（仅ADMIN审核场景） */
    EscortAuditDetailVO getAuditDetail(Long escortUserId);
    IPage<SysUser> userPage(int current, int size, String keyword);
    void toggleUserStatus(Long userId, String status);
    IPage<EscortOrder> orderPage(int current, int size, String status, String keyword);
    IPage<Evaluation> evaluationPage(int current, int size);
    void hideEvaluation(Long evalId, String reason);
}