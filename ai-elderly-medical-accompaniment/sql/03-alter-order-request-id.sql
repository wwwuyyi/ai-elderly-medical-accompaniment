-- ============================================
-- P4修正：escort_order新增request_id幂等字段
-- MySQL唯一索引允许多个NULL，未使用请求号不占唯一约束
-- ============================================
ALTER TABLE escort_order ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT '幂等请求号（客户端生成）' AFTER remark;
ALTER TABLE escort_order ADD UNIQUE KEY uk_request_id (request_id);