package com.example.escort.module.user.service;

import com.example.escort.module.user.dto.ChangePasswordDTO;
import com.example.escort.module.user.dto.UserProfileDTO;
import com.example.escort.module.user.vo.UserInfoVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 查询个人资料 */
    UserInfoVO getProfile(Long userId);

    /** 修改个人资料 */
    void updateProfile(Long userId, UserProfileDTO dto);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 更新头像 */
    String updateAvatar(Long userId, String avatarPath);
}