package com.dlyk.service;

import com.dlyk.model.TActivityRemark;
import com.dlyk.model.TClueRemark;
import com.dlyk.query.ActivityRemarkQuery;
import com.dlyk.query.ClueRemarkQuery;
import com.github.pagehelper.PageInfo;

/** copy by ShigureYukina,from 2025/8/26-下午6:20 */
public interface ClueRemarkService {

    int addActivityRemark(ClueRemarkQuery clueRemarkQuery);

    PageInfo<TActivityRemark> getClueRemarkPage(Integer current, ClueRemarkQuery query);
    
    TClueRemark getClueRemarkById(Integer id);
    
    int updateClueRemark(ClueRemarkQuery clueRemarkQuery);
    
    int deleteClueRemark(Integer id);
}