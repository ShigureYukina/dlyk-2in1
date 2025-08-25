package com.dlyk.config.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.dlyk.DlykServerApplication;
import com.dlyk.model.TDicValue;
import com.dlyk.result.DicEnum;

import java.util.List;

/**
 * 线索来源字段转换器
 * Excel中的线索来源文本转换为对应的字典值ID
 */
public class SourceConverter implements Converter<Object> {
    
    /**
     * Excel数据转Java数据
     */
    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String cellSourceName = cellData.getStringValue();
        
        // 如果Excel单元格为空，返回-1
        if (cellSourceName == null || cellSourceName.trim().isEmpty()) {
            return -1;
        }

        List<TDicValue> tDicValueList = (List<TDicValue>) DlykServerApplication.cacheMap.get(DicEnum.SOURCE.getCode());
        for (TDicValue tDicValue : tDicValueList) {
            Integer id = tDicValue.getId();
            String name = tDicValue.getTypeValue();

            if (cellSourceName.equals(name)) {
                return id;
            }
        }
        return -1;
    }
}