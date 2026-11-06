-- ============================================
-- BUG-G 修复：gender 列宽与 MALE/FEMALE 枚举值不兼容
-- 仅修改 elderly_profile / escort_profile 的 gender 列：
--   VARCHAR(4) -> VARCHAR(10) NOT NULL
-- 保留既有枚举值（MALE/FEMALE），不改列名、不删数据、不改索引。
-- 幂等说明：MySQL 8.0 的 ALTER TABLE ... MODIFY COLUMN 可重复执行，
--           连续运行两次均成功；重复应用相同列定义不会报错。
-- ============================================

USE escort_db;

ALTER TABLE elderly_profile
    MODIFY COLUMN gender VARCHAR(10) NOT NULL COMMENT '性别：MALE-男 / FEMALE-女';

ALTER TABLE escort_profile
    MODIFY COLUMN gender VARCHAR(10) NOT NULL COMMENT '性别：MALE-男 / FEMALE-女';