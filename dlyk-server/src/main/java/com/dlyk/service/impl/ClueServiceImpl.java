package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.model.TClue;
import com.dlyk.query.BaseQuery;
import com.dlyk.service.ClueService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/24-下午2:27 */
@Service
public class ClueServiceImpl implements ClueService {

    @Resource
    private TClueMapper tClueMapper;

    @Override
    public PageInfo<TClue> getCluePage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TClue> list = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
}
