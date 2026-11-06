package com.example.escort.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.escort.module.ai.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话会话Mapper
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
}