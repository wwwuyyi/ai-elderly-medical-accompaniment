-- ============================================
-- 面向老年群体AI赋能陪诊助医服务系统
-- 数据库建表脚本（MySQL 8.0）
-- 声明：全部业务数据为自行构造的模拟数据，不涉及任何真实医疗数据
-- ============================================

CREATE DATABASE IF NOT EXISTS escort_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE escort_db;

-- ============================================
-- 1. sys_user 系统用户表
-- 三角色统一账号：FAMILY/ESCORT/ADMIN
-- ============================================
CREATE TABLE sys_user (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    username        VARCHAR(50)      NOT NULL                 COMMENT '登录账号（手机号）',
    password        VARCHAR(200)     NOT NULL                 COMMENT '登录密码（BCrypt加密）',
    nickname        VARCHAR(50)      DEFAULT NULL             COMMENT '用户昵称',
    avatar          VARCHAR(255)     DEFAULT NULL             COMMENT '头像相对路径',
    phone           VARCHAR(20)      DEFAULT NULL             COMMENT '联系电话',
    role            VARCHAR(10)      NOT NULL                 COMMENT '角色：FAMILY-家属 / ESCORT-陪诊师 / ADMIN-管理员',
    status          VARCHAR(10)      NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED-启用 / DISABLED-禁用',
    last_login_time DATETIME         DEFAULT NULL             COMMENT '最后登录时间',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表（家属/陪诊师/管理员统一账号）';

-- ============================================
-- 2. elderly_profile 老人档案表
-- 家属为其名下的老人创建就医档案
-- ============================================
CREATE TABLE elderly_profile (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '档案ID',
    family_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '所属家属用户ID（逻辑外键→sys_user.id）',
    name            VARCHAR(30)      NOT NULL                 COMMENT '老人姓名',
    gender          VARCHAR(10)      NOT NULL                 COMMENT '性别：MALE-男 / FEMALE-女',
    birth_date      DATE             NOT NULL                 COMMENT '出生日期（服务层计算年龄）',
    id_card         VARCHAR(200)     NOT NULL                 COMMENT '身份证号（AES加密存储，密文）',
    phone           VARCHAR(20)      DEFAULT NULL             COMMENT '联系电话',
    address         VARCHAR(200)     DEFAULT NULL             COMMENT '住址（AES加密存储，密文）',
    emergency_contact VARCHAR(20)    DEFAULT NULL             COMMENT '紧急联系人电话',
    emergency_name  VARCHAR(30)      DEFAULT NULL             COMMENT '紧急联系人姓名',
    mobility        VARCHAR(10)      DEFAULT NULL             COMMENT '行动能力：WALK-自主行走 / ASSIST-需搀扶 / WHEELCHAIR-轮椅',
    allergy_history TEXT             DEFAULT NULL             COMMENT '过敏史',
    chronic_disease TEXT             DEFAULT NULL             COMMENT '慢性病史',
    remark          VARCHAR(500)     DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_family_id (family_id),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='老人档案表';

-- ============================================
-- 3. escort_profile 陪诊师信息表
-- 含资质审核字段，与sys_user.id一对一关联
-- ============================================
CREATE TABLE escort_profile (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '陪诊师信息ID',
    user_id         BIGINT UNSIGNED  NOT NULL                 COMMENT '关联用户ID（逻辑外键→sys_user.id，唯一）',
    real_name       VARCHAR(30)      NOT NULL                 COMMENT '真实姓名',
    gender          VARCHAR(10)      NOT NULL                 COMMENT '性别：MALE-男 / FEMALE-女',
    birth_date      DATE             NOT NULL                 COMMENT '出生日期（服务层计算年龄）',
    id_card         VARCHAR(200)     NOT NULL                 COMMENT '身份证号（AES加密存储，密文）',
    phone           VARCHAR(20)      NOT NULL                 COMMENT '联系电话',
    service_city    VARCHAR(50)      NOT NULL                 COMMENT '服务城市',
    service_area    VARCHAR(200)     DEFAULT NULL             COMMENT '服务区域（可多个，逗号分隔）',
    intro           VARCHAR(500)     DEFAULT NULL             COMMENT '个人简介/服务说明',
    materials       TEXT             DEFAULT NULL             COMMENT '资质材料图片路径JSON数组',
    audit_status    VARCHAR(10)      NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING-待审核 / APPROVED-审核通过 / REJECTED-审核驳回',
    audit_remark    VARCHAR(500)     DEFAULT NULL             COMMENT '审核备注/驳回原因',
    audit_time      DATETIME         DEFAULT NULL             COMMENT '审核时间',
    auditor_id      BIGINT UNSIGNED  DEFAULT NULL             COMMENT '审核人ID（逻辑外键→sys_user.id，管理员）',
    avg_rating      DECIMAL(3,2)     NOT NULL DEFAULT 5.00   COMMENT '平均评分（1.00-5.00）',
    total_orders    INT UNSIGNED     NOT NULL DEFAULT 0       COMMENT '累计完成订单数',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_audit_status (audit_status),
    KEY idx_service_city (service_city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='陪诊师信息表（含资质审核）';

-- ============================================
-- 4. hospital 医院字典表
-- 模拟数据，预置10家虚构医院
-- ============================================
CREATE TABLE hospital (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '医院ID',
    name            VARCHAR(100)     NOT NULL                 COMMENT '医院名称',
    address         VARCHAR(200)     NOT NULL                 COMMENT '医院地址',
    city            VARCHAR(50)      NOT NULL                 COMMENT '所属城市',
    level           VARCHAR(10)      NOT NULL                 COMMENT '医院等级：三甲/三乙/二甲/二乙',
    phone           VARCHAR(20)      DEFAULT NULL             COMMENT '医院联系电话',
    departments     TEXT             NOT NULL                 COMMENT '科室列表JSON数组',
    description     VARCHAR(500)     DEFAULT NULL             COMMENT '医院简介',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_city (city),
    KEY idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='医院字典表（模拟数据，全部虚构）';

-- ============================================
-- 5. escort_order 陪诊订单表
-- 核心业务表，含乐观锁version与支付状态字段
-- ============================================
CREATE TABLE escort_order (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '订单ID',
    order_no        VARCHAR(20)      NOT NULL                 COMMENT '订单编号（OD+yyyyMMdd+6位序列）',
    family_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '下单家属用户ID（逻辑外键→sys_user.id）',
    elderly_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '服务对象老人档案ID（逻辑外键→elderly_profile.id）',
    escort_id       BIGINT UNSIGNED  DEFAULT NULL             COMMENT '指派陪诊师用户ID（逻辑外键→sys_user.id，T2派单后写入）',
    hospital_id     BIGINT UNSIGNED  NOT NULL                 COMMENT '就诊医院ID（逻辑外键→hospital.id）',
    department      VARCHAR(50)      NOT NULL                 COMMENT '就诊科室',
    service_type    VARCHAR(10)      NOT NULL                 COMMENT '服务类型：BASIC-基础陪诊 / EXAM-陪同检查',
    appointment_time DATETIME        NOT NULL                 COMMENT '预约就诊开始时间（精确到分钟）',
    duration        DECIMAL(4,1)     NOT NULL DEFAULT 1.0     COMMENT '服务时长（小时），不足1按1计',
    amount          DECIMAL(10,2)    NOT NULL                 COMMENT '服务总金额（单位：元）',
    status          VARCHAR(20)      NOT NULL DEFAULT 'PENDING_DISPATCH' COMMENT '订单状态（枚举见状态机）',
    version         INT              NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    pay_status      VARCHAR(20)      NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态：UNPAID-未支付 / PAID-已支付 / PARTIAL_REFUNDED-部分退款 / REFUNDED-全额退款',
    pay_time        DATETIME         DEFAULT NULL             COMMENT '支付完成时间（T1下单时记录）',
    cancel_reason   VARCHAR(500)     DEFAULT NULL             COMMENT '取消原因（T10必须填写）',
    refund_amount   DECIMAL(10,2)    DEFAULT NULL             COMMENT '退款金额（模拟账目）',
    remark          VARCHAR(500)     DEFAULT NULL             COMMENT '订单备注',
    request_id      VARCHAR(64)      DEFAULT NULL             COMMENT '幂等请求号（客户端生成，唯一索引）',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_family_id (family_id),
    KEY idx_escort_id (escort_id),
    KEY idx_status (status),
    KEY idx_appointment_time (appointment_time),
    KEY idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='陪诊订单表';

-- ============================================
-- 6. order_status_log 订单状态流转日志表
-- ============================================
CREATE TABLE order_status_log (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    order_id        BIGINT UNSIGNED  NOT NULL                 COMMENT '关联订单ID（逻辑外键→escort_order.id）',
    order_no        VARCHAR(20)      NOT NULL                 COMMENT '订单编号（冗余字段，便于查询）',
    from_status     VARCHAR(20)      DEFAULT NULL             COMMENT '变更前状态（首次为NULL）',
    to_status       VARCHAR(20)      NOT NULL                 COMMENT '变更后状态',
    operator_id     BIGINT UNSIGNED  NOT NULL                 COMMENT '操作人用户ID（逻辑外键→sys_user.id）',
    operator_role   VARCHAR(10)      NOT NULL                 COMMENT '操作人角色：FAMILY/ESCORT/ADMIN',
    remark          VARCHAR(500)     DEFAULT NULL             COMMENT '操作备注',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（即状态变更时间）',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态流转日志表';

-- ============================================
-- 7. settlement 结算记录表
-- ============================================
CREATE TABLE settlement (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '结算ID',
    order_id        BIGINT UNSIGNED  NOT NULL                 COMMENT '关联订单ID（逻辑外键→escort_order.id，唯一）',
    order_no        VARCHAR(20)      NOT NULL                 COMMENT '订单编号（冗余）',
    escort_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '陪诊师用户ID（逻辑外键→sys_user.id）',
    order_amount    DECIMAL(10,2)    NOT NULL                 COMMENT '订单总金额',
    platform_rate   DECIMAL(4,2)     NOT NULL                 COMMENT '平台抽成比例（默认0.20=20%，模拟规则）',
    platform_fee    DECIMAL(10,2)    NOT NULL                 COMMENT '平台抽成金额',
    escort_income   DECIMAL(10,2)    NOT NULL                 COMMENT '陪诊师收入（order_amount - platform_fee）',
    settle_status   VARCHAR(20)      NOT NULL DEFAULT 'SETTLED' COMMENT '结算状态：SETTLED-已结算',
    operator_id     BIGINT UNSIGNED  NOT NULL                 COMMENT '结算操作人ID（管理员）',
    remark          VARCHAR(500)     DEFAULT NULL             COMMENT '结算备注',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_escort_id (escort_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结算记录表（模拟账目）';

-- ============================================
-- 8. evaluation 服务评价表
-- order_id唯一索引兜底"每单仅一次评价"
-- ============================================
CREATE TABLE evaluation (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '评价ID',
    order_id        BIGINT UNSIGNED  NOT NULL                 COMMENT '关联订单ID（逻辑外键→escort_order.id，唯一）',
    order_no        VARCHAR(20)      NOT NULL                 COMMENT '订单编号（冗余）',
    family_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '评价人家属用户ID（逻辑外键→sys_user.id）',
    escort_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '被评价陪诊师用户ID（逻辑外键→sys_user.id）',
    score           TINYINT UNSIGNED NOT NULL                 COMMENT '评分（1-5星）',
    content         VARCHAR(1000)    DEFAULT NULL             COMMENT '评价文字内容',
    is_hidden       TINYINT          NOT NULL DEFAULT 0       COMMENT '是否隐藏：0-显示 / 1-隐藏（管理员标记违规）',
    hide_reason     VARCHAR(500)     DEFAULT NULL             COMMENT '隐藏原因',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_escort_id (escort_id),
    KEY idx_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务评价表';

-- ============================================
-- 9. medical_record 就医档案表
-- ============================================
CREATE TABLE medical_record (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '档案ID',
    elderly_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '关联老人档案ID（逻辑外键→elderly_profile.id）',
    order_id        BIGINT UNSIGNED  DEFAULT NULL             COMMENT '关联订单ID（逻辑外键→escort_order.id，可为空）',
    hospital_id     BIGINT UNSIGNED  NOT NULL                 COMMENT '就诊医院ID（逻辑外键→hospital.id）',
    department      VARCHAR(50)      NOT NULL                 COMMENT '就诊科室',
    visit_date      DATE             NOT NULL                 COMMENT '就诊日期',
    chief_complaint VARCHAR(500)     DEFAULT NULL             COMMENT '主诉/就诊原因',
    diagnosis       VARCHAR(500)     DEFAULT NULL             COMMENT '诊断结论（对陪诊师角色脱敏）',
    prescription    TEXT             DEFAULT NULL             COMMENT '医嘱/用药建议',
    remark          VARCHAR(500)     DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_elderly_id (elderly_id),
    KEY idx_order_id (order_id),
    KEY idx_visit_date (visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='就医档案表';

-- ============================================
-- 10. medical_document 医疗单据表
-- 存放检验单/处方/病历三类单据及其OCR识别结果
-- ============================================
CREATE TABLE medical_document (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '单据ID',
    record_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '关联就医档案ID（逻辑外键→medical_record.id）',
    doc_type        VARCHAR(20)      NOT NULL                 COMMENT '单据类型：LAB_REPORT-检验单 / PRESCRIPTION-处方 / MEDICAL_RECORD-病历',
    image_url       VARCHAR(255)     NOT NULL                 COMMENT '单据图片相对路径',
    ocr_status      VARCHAR(20)      NOT NULL DEFAULT 'PENDING' COMMENT 'OCR识别状态：PENDING-待识别 / PROCESSING-识别中 / SUCCESS-成功 / FAILED-失败 / DEGRADED-降级',
    ocr_result      TEXT             DEFAULT NULL             COMMENT 'OCR识别结果（JSON格式，结构化数据）',
    ocr_confidence  DECIMAL(4,2)     DEFAULT NULL             COMMENT 'OCR置信度（0.00-1.00，<0.85标记manual_confirmed=false）',
    manual_confirmed TINYINT         NOT NULL DEFAULT 0       COMMENT '人工确认：0-未确认 / 1-已确认',
    confirmed_by    BIGINT UNSIGNED  DEFAULT NULL             COMMENT '确认人用户ID（逻辑外键→sys_user.id）',
    confirmed_time  DATETIME         DEFAULT NULL             COMMENT '确认时间',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_record_id (record_id),
    KEY idx_doc_type (doc_type),
    KEY idx_ocr_status (ocr_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='医疗单据表（检验单/处方/病历）';

-- ============================================
-- 11. ai_task AI调用任务表
-- ============================================
CREATE TABLE ai_task (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '任务ID',
    task_type       VARCHAR(10)      NOT NULL                 COMMENT '任务类型：OCR-文字识别 / NOTICE-告知书生成 / EXPLAIN-医嘱解读',
    biz_id          BIGINT UNSIGNED  NOT NULL                 COMMENT '关联业务ID（OCR→medical_document.id / NOTICE→escort_order.id / EXPLAIN→medical_record.id）',
    status          VARCHAR(10)      NOT NULL DEFAULT 'RUNNING' COMMENT '任务状态：RUNNING-执行中 / SUCCESS-成功 / FAILED-失败 / DEGRADED-降级',
    retry_count     TINYINT UNSIGNED NOT NULL DEFAULT 0       COMMENT '重试次数',
    error_msg       VARCHAR(1000)    DEFAULT NULL             COMMENT '错误信息（最新一次）',
    start_time      DATETIME         NOT NULL                 COMMENT '任务开始时间',
    end_time        DATETIME         DEFAULT NULL             COMMENT '任务结束时间',
    cost_ms         INT UNSIGNED     DEFAULT NULL             COMMENT '耗时（毫秒）',
    request_digest  VARCHAR(500)     DEFAULT NULL             COMMENT '请求摘要（脱敏后关键字段+长度）',
    response_digest VARCHAR(500)     DEFAULT NULL             COMMENT '响应摘要（SHA-256哈希+关键字段+长度，禁止明文全文）',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_biz_type (biz_id, task_type),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI调用任务表';

-- ============================================
-- 12. ai_generated_content AI生成内容表
-- 存放LLM生成的就诊告知书与医嘱解读
-- task_id唯一索引确保一次任务对应一条内容（1:1关系）
-- ============================================
CREATE TABLE ai_generated_content (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '内容ID',
    task_id         BIGINT UNSIGNED  NOT NULL                 COMMENT '关联AI任务ID（逻辑外键→ai_task.id，唯一）',
    content_type    VARCHAR(10)      NOT NULL                 COMMENT '内容类型：NOTICE-就诊告知书 / EXPLAIN-医嘱解读',
    biz_id          BIGINT UNSIGNED  NOT NULL                 COMMENT '关联业务ID（NOTICE→escort_order.id / EXPLAIN→medical_record.id）',
    content         TEXT             NOT NULL                 COMMENT 'AI生成内容原文',
    disclaimer      VARCHAR(200)     NOT NULL                 COMMENT '固定免责声明：AI生成内容仅供参考，不构成任何诊疗建议',
    review_status   VARCHAR(20)      NOT NULL DEFAULT 'UNREVIEWED' COMMENT '复核状态：UNREVIEWED-未复核 / REVIEWED-已通过 / REJECTED-已驳回',
    reviewer_id     BIGINT UNSIGNED  DEFAULT NULL             COMMENT '复核人用户ID（逻辑外键→sys_user.id）',
    review_time     DATETIME         DEFAULT NULL             COMMENT '复核时间',
    review_remark   VARCHAR(500)     DEFAULT NULL             COMMENT '复核备注',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_biz_type (biz_id, content_type),
    KEY idx_review_status (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI生成内容表（告知书/医嘱解读）';

-- ============================================
-- 13. sys_notification 系统通知表
-- ============================================
CREATE TABLE sys_notification (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '通知ID',
    recipient_id    BIGINT UNSIGNED  NOT NULL                 COMMENT '接收人用户ID（逻辑外键→sys_user.id）',
    title           VARCHAR(100)     NOT NULL                 COMMENT '通知标题',
    content         VARCHAR(500)     NOT NULL                 COMMENT '通知内容',
    biz_type        VARCHAR(20)      DEFAULT NULL             COMMENT '业务类型：ORDER-订单 / AUDIT-审核 / SETTLE-结算 / SYSTEM-系统',
    biz_id          BIGINT UNSIGNED  DEFAULT NULL             COMMENT '关联业务ID',
    is_read         TINYINT          NOT NULL DEFAULT 0       COMMENT '是否已读：0-未读 / 1-已读',
    read_time       DATETIME         DEFAULT NULL             COMMENT '阅读时间',
    create_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    update_time     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT          NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 / 1-已删除',
    PRIMARY KEY (id),
    KEY idx_recipient_read (recipient_id, is_read),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知表';