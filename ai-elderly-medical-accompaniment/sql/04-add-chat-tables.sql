-- ============================================
-- 面向老年群体AI赋能陪诊助医服务系统
-- AI智能对话模块 - 数据库建表（MySQL 8.0）
-- 声明：全部业务数据为自行构造的模拟数据，不涉及任何真实医疗数据
-- ============================================

USE escort_db;

-- ============================================
-- 14. ai_chat_session AI对话会话表
-- ============================================
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    user_id         BIGINT UNSIGNED  NOT NULL                 COMMENT '会话所属用户ID（逻辑外键→sys_user.id）',
    title           VARCHAR(100)     DEFAULT NULL             COMMENT '会话标题（首条消息前20字自动生成）',
    context_type    VARCHAR(20)      DEFAULT NULL             COMMENT '上下文类型：ELDERLY-老人档案 / ORDER-订单 / RECORD-就医记录 / GENERAL-通用',
    context_id      BIGINT UNSIGNED  DEFAULT NULL             COMMENT '上下文关联业务ID',
    system_prompt   TEXT             DEFAULT NULL             COMMENT '该会话注入的系统提示词（含上下文摘要）',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话会话表';

-- ============================================
-- 15. ai_chat_message AI对话消息表
-- ============================================
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
    session_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '所属会话ID（逻辑外键→ai_chat_session.id）',
    role            VARCHAR(10)      NOT NULL                 COMMENT '消息角色：USER-用户 / ASSISTANT-AI助手 / SYSTEM-系统提示',
    content         TEXT             NOT NULL                 COMMENT '消息内容',
    token_count     INT UNSIGNED     DEFAULT NULL             COMMENT 'token数量估算（用于统计）',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话消息表';