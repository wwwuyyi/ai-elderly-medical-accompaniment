package com.example.escort.module.ai.service;

import com.example.escort.module.ai.dto.*;
import com.example.escort.module.ai.vo.ContentVO;
import com.example.escort.module.ai.vo.OcrTaskVO;

public interface AiService {
    OcrTaskVO submitOcr(Long userId, OcrSubmitDTO dto);
    OcrTaskVO pollOcrTask(Long taskId);
    void confirmOcr(Long userId, Long taskId, OcrConfirmDTO dto);
    ContentVO generateNotice(Long userId, NoticeGenerateDTO dto);
    ContentVO generateExplain(Long userId, ExplainGenerateDTO dto);
    ContentVO getContent(Long id);
    void reviewContent(Long userId, Long contentId, ContentReviewDTO dto);
}