package com.dlyk.service.impl;

import com.dlyk.cache.TwoLevelCacheManager;
import com.dlyk.mapper.TDicTypeMapper;
import com.dlyk.model.TDicType;
import com.dlyk.service.DicTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典类型服务实现（使用双层缓存）
 * 
 * @author ShigureYukina, Enhanced by Kilo Code
 */
@Service
public class DicTypeServiceImpl implements DicTypeService {
    
    @Resource
    private TDicTypeMapper tDicTypeMapper;
    
    @Resource
    private TwoLevelCacheManager cacheManager;

    /**
     * 加载所有字典类型（使用双层缓存）
     * 
     * @return 字典类型列表
     */
    @Override
    public List<TDicType> loadAllDicType() {
        return cacheManager.get(
            "dicTypeCache",           // 缓存空间名称
            "allDicTypes",            // 缓存键
            () -> tDicTypeMapper.selectByAll()  // 数据加载器：从数据库查询
        );
    }
}