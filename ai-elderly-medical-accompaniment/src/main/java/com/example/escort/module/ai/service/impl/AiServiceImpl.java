package com.example.escort.module.ai.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.common.*;
import com.example.escort.common.SimpleCircuitBreaker;
import com.example.escort.config.AiProperties;
import com.example.escort.module.ai.dto.*;
import com.example.escort.module.ai.entity.AiGeneratedContent;
import com.example.escort.module.ai.entity.AiTask;
import com.example.escort.module.ai.mapper.AiGeneratedContentMapper;
import com.example.escort.module.ai.mapper.AiTaskMapper;
import com.example.escort.module.ai.service.AiService;
import com.example.escort.module.ai.vo.ContentVO;
import com.example.escort.module.ai.vo.OcrTaskVO;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.record.entity.MedicalDocument;
import com.example.escort.module.record.entity.MedicalRecord;
import com.example.escort.module.record.mapper.MedicalDocumentMapper;
import com.example.escort.module.record.mapper.MedicalRecordMapper;
import com.example.escort.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j @Service
public class AiServiceImpl implements AiService {

    private final AiTaskMapper taskMapper;
    private final AiGeneratedContentMapper contentMapper;
    private final MedicalDocumentMapper docMapper;
    private final MedicalRecordMapper recordMapper;
    private final EscortOrderMapper orderMapper;
    private final ElderlyProfileMapper elderlyMapper;
    private final AiHttpClient httpClient;
    private final ThreadPoolTaskExecutor aiExecutor;
    private final SimpleCircuitBreaker ocrBreaker;
    private final SimpleCircuitBreaker llmBreaker;
    private final AiProperties aiProps;
    private static final String DISCLAIMER = "AI生成内容仅供参考，不构成任何诊疗建议";

    public AiServiceImpl(AiTaskMapper tm, AiGeneratedContentMapper cm, MedicalDocumentMapper dm,
            MedicalRecordMapper rm, EscortOrderMapper om, ElderlyProfileMapper em, AiHttpClient hc,
            AiProperties ap,
            @Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor ex,
            @Qualifier("ocrBreaker") SimpleCircuitBreaker ob,
            @Qualifier("llmBreaker") SimpleCircuitBreaker lb) {
        taskMapper=tm; contentMapper=cm; docMapper=dm; recordMapper=rm; orderMapper=om; elderlyMapper=em;
        httpClient=hc; aiProps=ap; aiExecutor=ex; ocrBreaker=ob; llmBreaker=lb;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public OcrTaskVO submitOcr(Long userId, OcrSubmitDTO dto) {
        MedicalDocument doc = docMapper.selectById(dto.getDocumentId());
        if (doc == null) throw new BizException(ResultCode.NOT_FOUND, "单据不存在");
        checkEscortDocAccess(userId, doc, true);
        AiTask exist = taskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getBizId, doc.getId()).eq(AiTask::getTaskType, "OCR"));
        if (exist != null && "RUNNING".equals(exist.getStatus())) return toOcrVO(exist, doc);
        if (exist != null && "SUCCESS".equals(exist.getStatus())) return toOcrVO(exist, doc);
        AiTask task = new AiTask(); task.setTaskType("OCR"); task.setBizId(doc.getId());
        task.setStatus("RUNNING"); task.setRetryCount(0); task.setStartTime(LocalDateTime.now());
        task.setRequestDigest("docId="+doc.getId()+",type="+doc.getDocType());
        taskMapper.insert(task); doc.setOcrStatus("PROCESSING"); docMapper.updateById(doc);
        aiExecutor.execute(() -> executeOcr(task.getId(), doc));
        return toOcrVO(task, doc);
    }

    private void executeOcr(Long taskId, MedicalDocument doc) {
        AiTask task = taskMapper.selectById(taskId); if (task == null) return;
        if (!ocrBreaker.allowRequest()) { degrade(task, doc, "OCR熔断降级"); return; }
        LocalDateTime start = LocalDateTime.now();
        try {
            String resp = retryCall(
                    () -> httpClient.ocrPost("/recognize","{\"imageUrl\":\""+doc.getImageUrl()+"\"}"), task);
            OcrParsed p = parseOcr(resp);
            task.setStatus("SUCCESS"); task.setResponseDigest(sha256(resp));
            doc.setOcrResult(p.json); doc.setOcrConfidence(p.confidence);
            doc.setManualConfirmed(p.lowConf ? 0 : 1); doc.setOcrStatus("SUCCESS");
            ocrBreaker.onSuccess();
        } catch (Exception ex) {
            log.error("OCR失败 taskId={}", taskId, ex);
            task.setStatus("FAILED"); task.setErrorMsg(ex.getMessage());
            doc.setOcrStatus("FAILED"); ocrBreaker.onFailure();
        } finally {
            task.setEndTime(LocalDateTime.now());
            task.setCostMs((int)Duration.between(start,LocalDateTime.now()).toMillis());
            taskMapper.updateById(task); docMapper.updateById(doc);
        }
    }

    private OcrParsed parseOcr(String resp) {
        if (resp == null || resp.isBlank()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR返回为空");
        JSONObject obj;
        try { obj = JSONUtil.parseObj(resp); }
        catch (Exception e) { throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR响应非JSON"); }
        // 字段级校验规则需按所选API文档补充（以下为通用示例）
        if (!obj.containsKey("result")) throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR响应缺少result");
        BigDecimal conf = null;
        try { Object v = obj.get("confidence"); if (v instanceof Number) conf = BigDecimal.valueOf(((Number)v).doubleValue()); }
        catch (Exception ignored) {}
        boolean low = conf == null || conf.compareTo(new BigDecimal("0.85")) < 0;
        return new OcrParsed(obj.toStringPretty(), conf, low);
    }
    private static class OcrParsed { String json; BigDecimal confidence; boolean lowConf;
        OcrParsed(String j, BigDecimal c, boolean l) { json=j; confidence=c; lowConf=l; } }

    @Override public OcrTaskVO pollOcrTask(Long taskId) {
        AiTask t = taskMapper.selectById(taskId);
        if (t == null) throw new BizException(ResultCode.NOT_FOUND);
        MedicalDocument d = docMapper.selectById(t.getBizId());
        OcrTaskVO vo = toOcrVO(t, d);
        if (d != null && d.getOcrConfidence() != null && d.getOcrConfidence().compareTo(new BigDecimal("0.85")) < 0)
            vo.setErrorMsg((vo.getErrorMsg()!=null?vo.getErrorMsg()+";":"")+"置信度<0.85需人工确认");
        return vo;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void confirmOcr(Long userId, Long taskId, OcrConfirmDTO dto) {
        AiTask t = taskMapper.selectById(taskId); if (t == null) throw new BizException(ResultCode.NOT_FOUND);
        MedicalDocument d = docMapper.selectById(t.getBizId()); if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        checkEscortDocAccess(userId, d, true);
        if (dto.getCorrectedResult() != null) d.setOcrResult(dto.getCorrectedResult());
        d.setManualConfirmed(1); d.setConfirmedBy(userId); d.setConfirmedTime(LocalDateTime.now());
        docMapper.updateById(d);
    }

    @Override public ContentVO generateNotice(Long userId, NoticeGenerateDTO dto) {
        EscortOrder o = orderMapper.selectById(dto.getOrderId());
        if (o == null || !o.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        ElderlyProfile e = elderlyMapper.selectById(o.getElderlyId());
        String prompt = String.format("生成诊前告知书：医院ID=%d,科室=%s,预约时间=%s,行动能力=%s,过敏史=%s",
                o.getHospitalId(), o.getDepartment(), o.getAppointmentTime(),
                e!=null&&e.getMobility()!=null?e.getMobility():"未填写",
                e!=null&&e.getAllergyHistory()!=null?e.getAllergyHistory():"无");
        return llmGen("NOTICE", dto.getOrderId(), prompt);
    }

    @Override public ContentVO generateExplain(Long userId, ExplainGenerateDTO dto) {
        MedicalRecord r = recordMapper.selectById(dto.getRecordId());
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        String prompt = String.format("通俗化解读医嘱：诊断=%s,用药建议=%s,备注=%s",
                r.getDiagnosis()!=null?r.getDiagnosis():"未填写",
                r.getPrescription()!=null?r.getPrescription():"未填写",
                r.getRemark()!=null?r.getRemark():"无");
        return llmGen("EXPLAIN", dto.getRecordId(), prompt);
    }

    private ContentVO llmGen(String type, Long bizId, String prompt) {
        if (!llmBreaker.allowRequest()) return degraded(type, bizId);
        AiTask t = new AiTask(); t.setTaskType(type); t.setBizId(bizId); t.setStatus("RUNNING");
        t.setRetryCount(0); t.setStartTime(LocalDateTime.now()); t.setRequestDigest("bizId="+bizId);
        taskMapper.insert(t);
        LocalDateTime st = LocalDateTime.now();
        try {
            String resp = retryCall(() -> httpClient.llmPost("/chat","{\"prompt\":\""+prompt+"\"}"), t);
            if (resp == null || resp.isBlank()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "LLM返回空");
            if (resp.length() > 10000) resp = resp.substring(0,10000)+"...";
            t.setStatus("SUCCESS"); t.setResponseDigest(sha256(resp));
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t);
            AiGeneratedContent c = new AiGeneratedContent();
            c.setTaskId(t.getId()); c.setContentType(type); c.setBizId(bizId);
            c.setContent(resp); c.setDisclaimer(DISCLAIMER); c.setReviewStatus("UNREVIEWED");
            contentMapper.insert(c); llmBreaker.onSuccess();
            return toContentVO(c);
        } catch (Exception ex) {
            log.error("LLM失败 taskId={}", t.getId(), ex);
            t.setStatus("FAILED"); t.setErrorMsg(ex.getMessage());
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t); llmBreaker.onFailure();
            return degraded(type, bizId);
        }
    }

    private String retryCall(java.util.concurrent.Callable<String> c, AiTask t) throws Exception {
        long bo = aiProps.getRetryBackoffMs();
        int maxRetry = aiProps.getMaxRetry();
        for (int i = 0; i <= maxRetry; i++) {
            try { t.setRetryCount(i); taskMapper.updateById(t); return c.call(); }
            catch (Exception ex) {
                boolean retry = ex instanceof SocketTimeoutException;
                if (ex instanceof AiApiException) { int sc = ((AiApiException)ex).getStatusCode(); retry = sc >= 500 || sc == 429; }
                if (!retry || i == 3) throw ex;
                Thread.sleep(bo * (1L << i));
            }
        } throw new RuntimeException("重试耗尽");
    }

    @Override public ContentVO getContent(Long id) {
        AiGeneratedContent c = contentMapper.selectById(id);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND);
        checkContentOwner(UserContext.getUserId(), UserContext.getUserRole(), c);
        return toContentVO(c);
    }

    private void checkContentOwner(Long uid, String role, AiGeneratedContent c) {
        if (!"FAMILY".equals(role)) return;
        if ("NOTICE".equals(c.getContentType())) {
            EscortOrder o = orderMapper.selectById(c.getBizId());
            if (o == null || !o.getFamilyId().equals(uid)) throw new BizException(ResultCode.FORBIDDEN);
        } else if ("EXPLAIN".equals(c.getContentType())) {
            MedicalRecord r = recordMapper.selectById(c.getBizId());
            if (r == null) throw new BizException(ResultCode.NOT_FOUND);
            ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
            if (e == null || !e.getFamilyId().equals(uid)) throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void reviewContent(Long uid, Long cid, ContentReviewDTO dto) {
        AiGeneratedContent c = contentMapper.selectById(cid);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND);
        c.setReviewStatus(dto.getReviewStatus()); c.setReviewRemark(dto.getReviewRemark());
        c.setReviewerId(uid); c.setReviewTime(LocalDateTime.now());
        contentMapper.updateById(c);
    }

    private void checkEscortDocAccess(Long uid, MedicalDocument doc, boolean inService) {
        MedicalRecord r = recordMapper.selectById(doc.getRecordId());
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        LambdaQueryWrapper<EscortOrder> w = new LambdaQueryWrapper<EscortOrder>()
                .eq(EscortOrder::getEscortId, uid).eq(EscortOrder::getElderlyId, r.getElderlyId());
        if (inService) w.eq(EscortOrder::getStatus, "IN_SERVICE");
        if (orderMapper.selectCount(w) == 0) throw new BizException(ResultCode.FORBIDDEN);
    }

    private void degrade(AiTask t, MedicalDocument d, String msg) {
        t.setStatus("DEGRADED"); t.setEndTime(LocalDateTime.now()); t.setErrorMsg(msg);
        taskMapper.updateById(t); d.setOcrStatus("DEGRADED"); docMapper.updateById(d);
    }

    private ContentVO degraded(String type, Long bizId) {
        // 先落ai_task记录（保证ai_generated_content.task_id非空约束）
        AiTask t = new AiTask();
        t.setTaskType(type); t.setBizId(bizId); t.setStatus("DEGRADED");
        t.setRetryCount(0); t.setStartTime(LocalDateTime.now());
        t.setEndTime(LocalDateTime.now()); t.setCostMs(0);
        t.setErrorMsg("熔断降级或LLM服务不可用");
        taskMapper.insert(t);
        // 降级内容持久化，设置真实task_id
        AiGeneratedContent c = new AiGeneratedContent();
        c.setTaskId(t.getId()); c.setContentType(type); c.setBizId(bizId);
        c.setContent("【系统提示】AI服务暂时不可用。AI生成内容仅供参考，不构成任何诊疗建议。");
        c.setDisclaimer(DISCLAIMER); c.setReviewStatus("UNREVIEWED");
        contentMapper.insert(c);
        return toContentVO(c);
    }

    private OcrTaskVO toOcrVO(AiTask t, MedicalDocument d) {
        OcrTaskVO v = new OcrTaskVO();
        v.setTaskId(t.getId()); v.setStatus(t.getStatus()); v.setErrorMsg(t.getErrorMsg());
        v.setCreateTime(t.getCreateTime());
        if (d != null) { v.setOcrResult(d.getOcrResult()); v.setOcrConfidence(d.getOcrConfidence()); v.setManualConfirmed(d.getManualConfirmed()); }
        return v;
    }

    private ContentVO toContentVO(AiGeneratedContent c) {
        ContentVO v = new ContentVO();
        v.setId(c.getId()); v.setContentType(c.getContentType()); v.setContent(c.getContent());
        v.setDisclaimer(c.getDisclaimer()); v.setReviewStatus(c.getReviewStatus());
        v.setReviewRemark(c.getReviewRemark()); v.setCreateTime(c.getCreateTime());
        return v;
    }

    private String sha256(String s) { return DigestUtil.sha256Hex(s).substring(0,16)+"..."; }
}