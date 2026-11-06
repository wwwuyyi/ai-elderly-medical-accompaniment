package com.example.escort.module.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.example.escort.common.BizException;
import com.example.escort.common.ResultCode;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.auth.mapper.SysUserMapper;
import com.example.escort.module.user.dto.ChangePasswordDTO;
import com.example.escort.module.user.dto.UserProfileDTO;
import com.example.escort.module.user.service.UserService;
import com.example.escort.module.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserInfoVO getProfile(Long userId) {
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

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "旧密码不正确");
        }
        // 更新为新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        sysUserMapper.updateById(user);
        log.info("用户修改密码成功: userId={}", userId);
    }

    @Override
    public String updateAvatar(Long userId, String avatarPath) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setAvatar(avatarPath);
        sysUserMapper.updateById(user);
        return avatarPath;
    }
}