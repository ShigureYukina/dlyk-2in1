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
 * 意向状态字段转换器
 * Excel中的意向状态文本转换为对应的字典值ID
 */
public class IntentionStateConverter implements Converter<Object> {
    
    /**
     * Excel数据转Java数据
     */
    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String cellIntentionStateName = cellData.getStringValue();
        
        // 如果Excel单元格为空，返回-1
        if (cellIntentionStateName == null || cellIntentionStateName.trim().isEmpty()) {
            return -1;
        }

        List<TDicValue> tDicValueList = (List<TDicValue>) DlykServerApplication.cacheMap.get(DicEnum.INTENTION_STATE.getCode());
        for (TDicValue tDicValue : tDicValueList) {
            Integer id = tDicValue.getId();
            String name = tDicValue.getTypeValue();

            if (cellIntentionStateName.equals(name)) {
                return id;
            }
        }
        return -1;
    }
}