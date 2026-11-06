package com.example.escort.module.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.module.notify.entity.SysNotification;
import com.example.escort.module.notify.mapper.SysNotificationMapper;
import com.example.escort.module.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {
    private final SysNotificationMapper notifyMapper;

    @Override
    public IPage<SysNotification> page(Long userId, int current, int size) {
        Page<SysNotification> p = new Page<>(current, size);
        return notifyMapper.selectPage(p, new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getRecipientId, userId)
                .orderByDesc(SysNotification::getCreateTime));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long id) {
        notifyMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, id).eq(SysNotification::getRecipientId, userId)
                .set(SysNotification::getIsRead, 1).set(SysNotification::getReadTime, LocalDateTime.now()));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notifyMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getRecipientId, userId).eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1).set(SysNotification::getReadTime, LocalDateTime.now()));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void send(Long recipientId, String title, String content, String bizType, Long bizId) {
        SysNotification n = new SysNotification();
        n.setRecipientId(recipientId); n.setTitle(title); n.setContent(content);
        n.setBizType(bizType); n.setBizId(bizId); n.setIsRead(0);
        notifyMapper.insert(n);
    }
}