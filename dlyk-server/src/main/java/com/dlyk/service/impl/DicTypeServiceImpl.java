package com.dlyk.service.impl;

import com.dlyk.mapper.TDicTypeMapper;
import com.dlyk.model.TDicType;
import com.dlyk.model.TProduct;
import com.dlyk.service.DicTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/25-下午2:37 */
@Service
public class DicTypeServiceImpl implements DicTypeService {
    @Resource
    private TDicTypeMapper tDicTypeMapper;

    @Override
    public List<TDicType> loadAllDicType() {
        return tDicTypeMapper.selectByAll();
    }

}