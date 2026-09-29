package com.dlyk.service;

import com.dlyk.model.TClue;
import com.dlyk.query.ClueQuery;
import com.github.pagehelper.PageInfo;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/24-下午2:27 */
public interface ClueService {

    PageInfo<TClue> getCluePage(Integer current);

    /**
     * 游标分页查询线索（深分页优化）
     *
     * @param lastId 上一页最后一条的 id，为 null 表示查第一页
     * @param size   每页条数，为空时取默认分页大小
     * @return 线索列表，按 id 倒序
     */
    List<TClue> getCluePageByCursor(Integer lastId, Integer size);

    void importExcel(InputStream inputStream,String token);

    Boolean checkPhone(String phone);

    int addClue(ClueQuery cluequery);

    TClue getClue(Integer id);

    int updateClue(ClueQuery cluequery);
    
    int deleteClue(Integer id);
    
    int batchDeleteClue(List<Integer> idList);
    
    void exportClueExcel(HttpServletResponse response) throws Exception;
    
    void exportSelectedClue(List<Integer> idList, HttpServletResponse response) throws Exception;
}
