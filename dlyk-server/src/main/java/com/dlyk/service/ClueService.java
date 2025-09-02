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
