package com.example.escort.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.escort.module.ai.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话消息Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}