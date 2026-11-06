INSERT INTO sys_user (id,username,password,nickname,role,status) VALUES (2,'13800001111','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E','家属测试','FAMILY','ENABLED');
INSERT INTO sys_user (id,username,password,nickname,role,status) VALUES (3,'13800002222','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E','陪诊师测试','ESCORT','ENABLED');
INSERT INTO escort_profile (user_id,real_name,gender,birth_date,id_card,phone,service_city,materials,audit_status) VALUES (3,'李明','MALE','1990-03-15','test123','13800002222','广州','[]','APPROVED');
INSERT INTO hospital (name,address,city,level,phone,departments) VALUES ('颐年康人民医院','广州市云山区桃源路88号','广州','三甲','020-00000001','["内科","外科"]');
INSERT INTO elderly_profile (family_id,name,gender,birth_date,id_card,phone,mobility) VALUES (2,'张老伯','MALE','1950-05-20','test123','13800001112','WALK');
