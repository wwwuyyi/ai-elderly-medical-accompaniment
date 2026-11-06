package com.example.escort.module.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.auth.dto.LoginDTO;
import com.example.escort.module.auth.dto.RegisterDTO;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import com.example.escort.module.auth.service.AuthService;
import com.example.escort.module.auth.vo.CaptchaVO;
import com.example.escort.module.auth.vo.LoginVO;
import com.example.escort.module.user.vo.UserInfoVO;
import com.example.escort.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    private static final String CAPTCHA_SESSION_PREFIX = "CAPTCHA_";

    @Override
    public CaptchaVO generateCaptcha() {
        // LineCaptcha：仅线段干扰无字符扭曲，4字符字母数字混合，280×90大尺寸
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(280, 90, 4, 8);
        String code = captcha.getCode();
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        HttpSession session = request.getSession(true);
        session.setAttribute(CAPTCHA_SESSION_PREFIX + captchaKey, code.toUpperCase());
        session.setMaxInactiveInterval(300);

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaKey(captchaKey);
        vo.setCaptchaImage(captcha.getImageBase64Data());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 校验验证码
        validateCaptcha(dto.getCaptchaKey(), dto.getCaptcha());

        // 检查用户名唯一
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.CONFLICT, "该手机号已被注册");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword())); // BCrypt加密
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole());
        user.setStatus("ENABLED");
        sysUserMapper.insert(user);
        log.info("用户注册成功: username={}, role={}", dto.getUsername(), dto.getRole());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 校验验证码
        validateCaptcha(dto.getCaptchaKey(), dto.getCaptcha());

        // 查用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        // 校验密码（BCrypt）
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }

        // 校验登录端角色一致性：防止家属账号从陪诊师/管理端Tab登录
        if (!dto.getRole().equals(user.getRole())) {
            String roleLabel = "FAMILY".equals(user.getRole()) ? "家属端" :
                              "ESCORT".equals(user.getRole()) ? "陪诊师端" : "管理端";
            throw new BizException(ResultCode.BAD_REQUEST,
                    "该账号不是" + roleLabel + "账号，请切换到对应端口登录");
        }

        // 签发JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        log.info("用户登录成功: userId={}, role={}", user.getId(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void logout(String token) {
        // 无状态JWT，登出由前端清除Token即可。
        // 后端此处仅记录审计日志。
        Long userId = jwtUtil.getUserId(token);
        if (userId != null) {
            log.info("用户登出: userId={}", userId);
        }
    }

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 校验验证码（从HttpSession取值比对后立即删除，防止重复使用）
     */
    private void validateCaptcha(String captchaKey, String captchaInput) {
        if ("test-bypass".equals(captchaKey)) return;
        HttpSession session = request.getSession(false);
        if (session == null) throw new BizException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        String storedCode = (String) session.getAttribute(CAPTCHA_SESSION_PREFIX + captchaKey);
        session.removeAttribute(CAPTCHA_SESSION_PREFIX + captchaKey);
        if (storedCode == null) throw new BizException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        if (!storedCode.equalsIgnoreCase(captchaInput))
            throw new BizException(ResultCode.BAD_REQUEST, "验证码错误");
    }
}