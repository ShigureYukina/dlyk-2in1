package com.dlyk.mapper;

import com.dlyk.model.TClue;
import com.dlyk.query.BaseQuery;
import com.dlyk.result.NameValue;

import java.util.List;

public interface TClueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    List<TClue> selectClueByPage(BaseQuery build);

    int saveClue(List<TClue> clueList);

    int selectByPhone(String phone);

    TClue selectDetailById(Integer id);

    /**
     * 统计线索来源分布
     */
    List<NameValue> selectClueSourceStats();

    /**
     * 按负责人统计线索数量
     */
    List<NameValue> selectClueStatsByOwner();
}