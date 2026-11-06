package com.example.escort.module.notify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.module.notify.entity.SysNotification;

public interface NotifyService {
    IPage<SysNotification> page(Long userId, int current, int size);
    void markRead(Long userId, Long id);
    void markAllRead(Long userId);
    void send(Long recipientId, String title, String content, String bizType, Long bizId);
}