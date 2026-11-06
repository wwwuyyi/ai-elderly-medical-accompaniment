package com.example.escort.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.escort.module.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}