package com.example.escort.common.desensitize;

import com.example.escort.security.UserContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Jackson自定义序列化器，根据当前角色动态脱敏。
 * 陪诊师窗口期可见性由Service层设置UserContext扩展标记控制（窗口期内不脱敏phone），
 * 此处仅做序列化层判断。
 */
public class SensitiveFieldSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type;

    public SensitiveFieldSerializer() {}

    public SensitiveFieldSerializer(SensitiveType type) { this.type = type; }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider prov) throws IOException {
        if (value == null) { gen.writeNull(); return; }
        String role = UserContext.getUserRole();
        if ("FAMILY".equals(role)) { gen.writeString(value); return; }
        if ("ADMIN".equals(role)) { gen.writeString(DesensitizeUtil.desensitize(value, type)); return; }
        if ("ESCORT".equals(role)) {
            if (type == SensitiveType.PHONE && UserContext.isPhoneFullAccess()) {
                gen.writeString(value); return; // 窗口期内电话全量
            }
            gen.writeString(DesensitizeUtil.desensitize(value, type));
            return;
        }
        gen.writeString(value);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty prop)
            throws JsonMappingException {
        SensitiveField ann = prop != null ? prop.getAnnotation(SensitiveField.class) : null;
        return ann != null ? new SensitiveFieldSerializer(ann.type()) : this;
    }
}