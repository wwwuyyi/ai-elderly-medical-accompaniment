package com.example.escort;

import cn.hutool.crypto.digest.BCrypt;
import com.example.escort.common.AesUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 数据初始化器
 * 首次启动时创建预置测试账号与演示数据。
 *
 * 声明：以下全部为自行构造的模拟数据，仅用于毕设演示。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    private final AesUtil aesUtil;

    public DataInitializer(DataSource dataSource, AesUtil aesUtil) {
        this.dataSource = dataSource;
        this.aesUtil = aesUtil;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // 检查是否已初始化（以管理员是否存在为标志）
            String checkSql = "SELECT COUNT(*) FROM sys_user WHERE username = 'admin' AND deleted = 0";
            try (PreparedStatement ps = conn.prepareStatement(checkSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            // ===== 创建管理员 =====
            insertUser(conn, "admin", BCrypt.hashpw("admin123"), "系统管理员", "ADMIN");

            // ===== 创建测试家属 =====
            insertUser(conn, "13800001111", BCrypt.hashpw("Test1234"), "测试家属", "FAMILY");

            // 为测试家属预置一位老人档案
            String insertElderly = "INSERT INTO elderly_profile (family_id, name, gender, birth_date, id_card, phone, address, emergency_contact, emergency_name, mobility) " +
                    "VALUES (2, '张大爷', 'MALE', '1955-03-15', ?, '13900001111', ?, '13800001111', '张家属', 'WALK')";
            try (PreparedStatement ps = conn.prepareStatement(insertElderly)) {
                // AES加密存储（使用Hutool SimpleAES，密钥从配置注入；此处使用占位密文——实际加密由Service层完成）
                // 由于DataInitializer不持有AesUtil，存储明文占位符，Service层读取时会通过AesUtil解密
                // 真实启动时由ElderlyProfileServiceImpl在业务层加密
                ps.setString(1, aesUtil.encrypt("330102195503151234"));
                ps.setString(2, aesUtil.encrypt("北京市朝阳区测试路100号"));
                ps.executeUpdate();
                System.out.println("[DataInitializer] 测试家属老人档案已创建: 张大爷");
            }

            // ===== 创建测试陪诊师（已审核通过） =====
            insertUser(conn, "13800002222", BCrypt.hashpw("Test1234"), "测试陪诊师李明", "ESCORT");
            String insertApproved = "INSERT INTO escort_profile (user_id, real_name, gender, birth_date, id_card, phone, service_city, service_area, intro, audit_status) " +
                    "VALUES (3, '李明', 'MALE', '1990-03-15', ?, '13700137000', '广州', '越秀区,天河区', '5年陪诊经验，熟悉各三甲医院就诊流程', 'APPROVED')";
            try (PreparedStatement ps = conn.prepareStatement(insertApproved)) {
                ps.setString(1, aesUtil.encrypt("330102199003151234"));
                ps.executeUpdate();
                System.out.println("[DataInitializer] 测试陪诊师(已审核)已创建: 13800002222 / Test1234");
            }

            // ===== 创建测试陪诊师（待审核） =====
            insertUser(conn, "13800003333", BCrypt.hashpw("Test1234"), "测试陪诊师王芳", "ESCORT");
            String insertPending = "INSERT INTO escort_profile (user_id, real_name, gender, birth_date, id_card, phone, service_city, service_area, intro, audit_status) " +
                    "VALUES (4, '王芳', 'FEMALE', '1992-07-20', ?, '13700137001', '深圳', '南浦区,福安区', '2年护理经验，持护士资格证', 'PENDING')";
            try (PreparedStatement ps = conn.prepareStatement(insertPending)) {
                ps.setString(1, aesUtil.encrypt("440301199207201234"));
                ps.executeUpdate();
                System.out.println("[DataInitializer] 测试陪诊师(待审核)已创建: 13800003333 / Test1234");
            }

            System.out.println("[DataInitializer] 预置测试账号初始化完成");
            System.out.println("  管理员: admin / admin123");
            System.out.println("  家属:   13800001111 / Test1234（已预置老人档案）");
            System.out.println("  陪诊师: 13800002222 / Test1234（已审核通过，可直接接单）");
            System.out.println("  陪诊师: 13800003333 / Test1234（待审核，演示审核流程）");
        }
    }

    private void insertUser(Connection conn, String username, String passwordHash, String nickname, String role) throws Exception {
        String sql = "INSERT INTO sys_user (username, password, nickname, role, status) VALUES (?, ?, ?, ?, 'ENABLED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, nickname);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }
}