package com.dlyk.config.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.dlyk.DlykServerApplication;
import com.dlyk.model.TProduct;
import com.dlyk.result.DicEnum;

import java.util.List;

/**
 * 意向产品字段转换器
 * Excel中的产品名称转换为对应的产品ID
 * 注意：此转换器与其他转换器不同，它查找的是产品表而非字典表
 */
public class IntentionProductConverter implements Converter<Object> {

    /**
     * Excel数据转Java数据
     */
    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String cellProductName = cellData.getStringValue();

        // 如果Excel单元格为空，返回-1
        if (cellProductName == null || cellProductName.trim().isEmpty()) {
            return -1;
        }

        // 从缓存中获取产品列表
        List<TProduct> tProductList = (List<TProduct>) DlykServerApplication.cacheMap.get(DicEnum.PRODUCT.getCode());

        // 如果产品列表为空，返回-1
        if (tProductList == null || tProductList.isEmpty()) {
            return -1;
        }

        // 遍历产品列表，查找匹配的产品名称
        for (TProduct tProduct : tProductList) {
            Integer id = tProduct.getId();
            String name = tProduct.getName();

            if (cellProductName.equals(name)) {
                return id;
            }
        }
        return -1;
    }
}