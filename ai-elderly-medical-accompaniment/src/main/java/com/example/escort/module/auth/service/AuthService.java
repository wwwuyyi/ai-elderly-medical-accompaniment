package com.example.escort.module.auth.service;

import com.example.escort.module.auth.dto.LoginDTO;
import com.example.escort.module.auth.dto.RegisterDTO;
import com.example.escort.module.auth.vo.CaptchaVO;
import com.example.escort.module.auth.vo.LoginVO;
import com.example.escort.module.user.vo.UserInfoVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /** 生成图形验证码，答案存入HttpSession */
    CaptchaVO generateCaptcha();

    /** 注册（FAMILY/ESCORT） */
    void register(RegisterDTO dto);

    /** 登录 */
    LoginVO login(LoginDTO dto);

    /** 登出 */
    void logout(String token);

    /** 获取当前用户信息 */
    UserInfoVO getCurrentUserInfo(Long userId);
}