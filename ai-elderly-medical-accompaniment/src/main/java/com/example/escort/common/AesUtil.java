package com.example.escort.common;

import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AES加密工具类
 * 用于身份证号、住址等敏感字段的加密存储。
 * 生产环境须使用KMS/环境变量注入密钥，毕设演示使用本地配置。
 */
@Component
public class AesUtil {

    private final AES aes;

    public AesUtil(@Value("${encrypt.aes-key}") String aesKey) {
        // AES密钥需16/24/32字节
        byte[] keyBytes = new byte[16];
        byte[] src = aesKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        this.aes = new AES(keyBytes);
    }

    /**
     * 加密，返回Base64密文
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        return aes.encryptBase64(plainText);
    }

    /**
     * 解密
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        return aes.decryptStr(cipherText);
    }
}