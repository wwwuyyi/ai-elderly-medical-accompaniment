package com.example.escort.module.escort.service;

import com.example.escort.module.escort.dto.EscortApplyDTO;
import com.example.escort.module.escort.dto.EscortProfileDTO;
import com.example.escort.module.escort.vo.EscortProfileVO;

/**
 * 陪诊师服务接口
 */
public interface EscortProfileService {

    /** 提交资质认证申请 */
    void apply(Long userId, EscortApplyDTO dto);

    /** 查询本人审核状态 */
    EscortProfileVO getApplyStatus(Long userId);

    /** 查询服务资料 */
    EscortProfileVO getProfile(Long userId);

    /** 修改服务资料 */
    void updateProfile(Long userId, EscortProfileDTO dto);

    /** 可接订单池 */
    java.util.List<com.example.escort.module.order.entity.EscortOrder> getOrderPool(Long userId);

    /** 我的统计 */
    java.util.Map<String, Object> getStatistics(Long userId);
}