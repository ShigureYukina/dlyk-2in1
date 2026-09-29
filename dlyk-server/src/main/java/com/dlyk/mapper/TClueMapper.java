package com.dlyk.mapper;

import com.dlyk.model.TClue;
import com.dlyk.query.BaseQuery;
import com.dlyk.result.NameValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TClueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    List<TClue> selectClueByPage(BaseQuery build);

    /**
     * 存活线索总数。
     *
     * <p>列表页需要的 total 单独用单表统计，而不是让 PageHelper 对
     * 7 表联查的 SQL 做 count：联查的每个 LEFT JOIN 都挂在被驱动表的主键上，
     * 不会放大行数，因此两者结果一致，但单表统计快一个数量级。
     */
    long countAlive();

    /**
     * 游标分页：取 id 小于 lastId 的 limit 条记录（lastId 为空表示第一页）。
     * 用 WHERE id &lt; lastId 取代 OFFSET，扫描行数恒为 limit，与页码无关。
     */
    List<TClue> selectClueByCursor(@Param("lastId") Integer lastId, @Param("limit") int limit);

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
    
    /**
     * 批量逻辑删除线索
     */
    int batchDeleteByIds(List<Integer> idList);
    
    /**
     * 根据ID列表查询线索
     */
    List<TClue> selectCluesByIds(List<Integer> idList);
}