package com.example.escort.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.record.entity.MedicalRecord;
import com.example.escort.module.record.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI对话上下文构建器
 * 根据会话的contextType/contextId动态拼装system prompt，注入业务上下文信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final ElderlyProfileMapper elderlyMapper;
    private final MedicalRecordMapper recordMapper;
    private final EscortOrderMapper orderMapper;

    /** 通用基础提示词（无特定上下文时使用） */
    private static final String BASE_SYSTEM_PROMPT =
            "你是【陪诊助医AI健康助手】，专为老年人及家属提供就医辅助服务。\n" +
            "\n" +
            "【你的能力范围】\n" +
            "1. 解答就诊流程问题（挂号、报到、候诊、检查、缴费、取药等）\n" +
            "2. 通俗化解读检查报告指标和医学术语\n" +
            "3. 提醒用药注意事项和就诊准备事项\n" +
            "4. 提供健康生活方式的通用建议\n" +
            "\n" +
            "【严格禁止】\n" +
            "1. 禁止给出诊断结论——你无权诊断任何疾病\n" +
            "2. 禁止推荐具体药品、剂量或替代治疗方案\n" +
            "3. 禁止给出【不需要看医生】【不用去医院】的结论\n" +
            "4. 遇到紧急症状（剧烈胸痛、呼吸困难、意识模糊、大出血等）必须首先建议立即前往急诊就医\n" +
            "\n" +
            "【免责声明】\n" +
            "AI生成内容仅供参考，不构成任何诊疗建议。如有健康问题请及时就医。";

    /**
     * 根据上下文类型构建system prompt
     * @param contextType 上下文类型：ELDERLY/ORDER/RECORD/GENERAL
     * @param contextId   关联业务ID
     * @return 拼接完成的system prompt
     */
    public String buildSystemPrompt(String contextType, Long contextId) {
        if (contextType == null || contextId == null) {
            return BASE_SYSTEM_PROMPT;
        }

        StringBuilder sb = new StringBuilder(BASE_SYSTEM_PROMPT);

        try {
            switch (contextType.toUpperCase()) {
                case "ELDERLY" -> appendElderlyContext(sb, contextId);
                case "ORDER" -> appendOrderContext(sb, contextId);
                case "RECORD" -> appendRecordContext(sb, contextId);
                default -> { /* GENERAL：不追加额外上下文 */ }
            }
        } catch (Exception e) {
            log.warn("构建对话上下文失败 contextType={} contextId={}", contextType, contextId, e);
        }

        return sb.toString();
    }

    /** 注入老人档案摘要 */
    private void appendElderlyContext(StringBuilder sb, Long elderlyId) {
        ElderlyProfile e = elderlyMapper.selectById(elderlyId);
        if (e == null) return;
        sb.append("\n\n【当前老人信息】\n");
        sb.append("- 年龄：").append(calcAge(e.getBirthDate())).append("岁\n");
        sb.append("- 性别：").append("MALE".equals(e.getGender()) ? "男" : "女").append("\n");
        if (e.getMobility() != null) {
            sb.append("- 行动能力：").append(mobilityLabel(e.getMobility())).append("\n");
        }
        if (e.getChronicDisease() != null && !e.getChronicDisease().isBlank()) {
            sb.append("- 慢性病史：").append(e.getChronicDisease()).append("\n");
        }
        if (e.getAllergyHistory() != null && !e.getAllergyHistory().isBlank()) {
            sb.append("- 过敏史：").append(e.getAllergyHistory()).append("\n");
        }
    }

    /** 注入订单信息 + 老人档案 */
    private void appendOrderContext(StringBuilder sb, Long orderId) {
        EscortOrder o = orderMapper.selectById(orderId);
        if (o == null) return;
        sb.append("\n\n【当前订单信息】\n");
        sb.append("- 预约时间：").append(o.getAppointmentTime()).append("\n");
        sb.append("- 就诊科室：").append(o.getDepartment()).append("\n");
        if (o.getRemark() != null && !o.getRemark().isBlank()) {
            sb.append("- 备注：").append(o.getRemark()).append("\n");
        }
        // 注入关联老人信息
        if (o.getElderlyId() != null) {
            appendElderlyContext(sb, o.getElderlyId());
        }
    }

    /** 注入就医记录 + 老人档案 */
    private void appendRecordContext(StringBuilder sb, Long recordId) {
        MedicalRecord r = recordMapper.selectById(recordId);
        if (r == null) return;
        sb.append("\n\n【最近就诊记录】\n");
        sb.append("- 就诊日期：").append(r.getVisitDate()).append("\n");
        if (r.getDepartment() != null) sb.append("- 就诊科室：").append(r.getDepartment()).append("\n");
        if (r.getDiagnosis() != null && !r.getDiagnosis().isBlank())
            sb.append("- 诊断结论：").append(r.getDiagnosis()).append("\n");
        if (r.getPrescription() != null && !r.getPrescription().isBlank())
            sb.append("- 医嘱/处方：").append(r.getPrescription()).append("\n");
        // 注入关联老人信息
        if (r.getElderlyId() != null) {
            appendElderlyContext(sb, r.getElderlyId());
        }
    }

    /** 计算年龄（基于出生日期） */
    private int calcAge(java.time.LocalDate birthDate) {
        if (birthDate == null) return 0;
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    /** 行动能力中文标签 */
    private String mobilityLabel(String mobility) {
        return switch (mobility) {
            case "WALK" -> "自主行走";
            case "ASSIST" -> "需搀扶";
            case "WHEELCHAIR" -> "轮椅";
            default -> mobility;
        };
    }
}