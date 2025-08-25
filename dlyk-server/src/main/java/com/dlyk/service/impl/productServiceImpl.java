package com.dlyk.service.impl;

import com.dlyk.mapper.TProductMapper;
import com.dlyk.model.TProduct;
import com.dlyk.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/25-下午4:15 */
@Service
public class productServiceImpl implements ProductService {
    @Resource
    private TProductMapper tProductMapper;

    @Override
    public List<TProduct> loadAllProduct() {
        return tProductMapper.selectAllOnSale();
    }
}
