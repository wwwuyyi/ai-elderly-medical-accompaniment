package com.example.escort.security;

import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect @Component
public class RequireRoleAspect {

    @Before("@within(com.example.escort.security.RequireRole) || @annotation(com.example.escort.security.RequireRole)")
    public void checkRole(JoinPoint jp) {
        String currentRole = UserContext.getUserRole();
        if (currentRole == null) throw new BizException(ResultCode.UNAUTHORIZED);
        RequireRole requireRole = null;
        MethodSignature sig = (MethodSignature) jp.getSignature();
        requireRole = sig.getMethod().getAnnotation(RequireRole.class);
        if (requireRole == null)
            requireRole = jp.getTarget().getClass().getAnnotation(RequireRole.class);
        if (requireRole == null) return;
        if (!Arrays.asList(requireRole.value()).contains(currentRole))
            throw new BizException(ResultCode.FORBIDDEN);
    }
}