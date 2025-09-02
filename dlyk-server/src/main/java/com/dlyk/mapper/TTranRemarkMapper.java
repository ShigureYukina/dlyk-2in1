package com.dlyk.mapper;

import com.dlyk.model.TTranRemark;

import java.util.List;

public interface TTranRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranRemark record);

    int insertSelective(TTranRemark record);

    TTranRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranRemark record);

    int updateByPrimaryKey(TTranRemark record);
    
    /**
     * 根据交易ID查询备注列表
     */
    List<TTranRemark> selectByTranId(Integer tranId);
}