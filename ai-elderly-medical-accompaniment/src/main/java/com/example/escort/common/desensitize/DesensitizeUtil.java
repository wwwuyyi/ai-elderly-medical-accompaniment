package com.example.escort.common.desensitize;

/**
 * 脱敏工具类（手工调用场景：日志输出、消息拼接）
 *
 * 规则：
 * - NAME: 张* / 欧阳**
 * - PHONE: 138****8000
 * - ID_CARD: 3**************4
 * - ADDRESS: 保留到区/县级，详细地址替换为*
 * - DIAGNOSIS: [诊断结论已脱敏]
 */
public class DesensitizeUtil {

    public static String desensitize(String value, SensitiveType type) {
        if (value == null || value.isEmpty()) return value;
        switch (type) {
            case NAME:      return desensitizeName(value);
            case PHONE:     return desensitizePhone(value);
            case ID_CARD:   return desensitizeIdCard(value);
            case ADDRESS:   return desensitizeAddress(value);
            case DIAGNOSIS: return "[诊断结论已脱敏]";
            default:        return value;
        }
    }

    public static String desensitizeName(String name) {
        if (name == null || name.length() < 2) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 2) return idCard;
        return idCard.charAt(0) + "*".repeat(idCard.length() - 2) + idCard.charAt(idCard.length() - 1);
    }

    public static String desensitizeAddress(String address) {
        if (address == null || address.isEmpty()) return address;
        if (address.length() <= 6) return "*";
        return address.substring(0, 6) + "***";
    }
}