package com.example.escort.module.elderly.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.module.elderly.dto.ElderlyProfileDTO;
import com.example.escort.module.elderly.vo.ElderlyProfileVO;

/**
 * 老人档案服务接口
 */
public interface ElderlyProfileService {

    /** 新增老人档案 */
    void create(Long familyId, ElderlyProfileDTO dto);

    /** 修改老人档案 */
    void update(Long familyId, Long id, ElderlyProfileDTO dto);

    /** 删除（存在进行中订单时禁止删除，返回409） */
    void delete(Long familyId, Long id);

    /** 分页查询（仅当前家属名下） */
    IPage<ElderlyProfileVO> page(Long familyId, int current, int size);

    /** 详情 */
    ElderlyProfileVO getById(Long familyId, Long id);
}