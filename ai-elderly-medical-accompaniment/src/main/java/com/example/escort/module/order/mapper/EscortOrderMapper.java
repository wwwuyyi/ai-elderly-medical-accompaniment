package com.example.escort.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.escort.module.order.entity.EscortOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 陪诊订单 Mapper
 */
@Mapper
public interface EscortOrderMapper extends BaseMapper<EscortOrder> {
}