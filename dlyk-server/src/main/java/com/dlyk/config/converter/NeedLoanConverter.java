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
 * 是否贷款字段转换器
 * Excel中的是否贷款文本转换为对应的字典值ID
 */
public class NeedLoanConverter implements Converter<Object> {
    
    /**
     * Excel数据转Java数据
     */
    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String cellNeedLoanName = cellData.getStringValue();
        
        // 如果Excel单元格为空，返回-1
        if (cellNeedLoanName == null || cellNeedLoanName.trim().isEmpty()) {
            return -1;
        }

        List<TDicValue> tDicValueList = (List<TDicValue>) DlykServerApplication.cacheMap.get(DicEnum.NEED_LOAN.getCode());
        for (TDicValue tDicValue : tDicValueList) {
            Integer id = tDicValue.getId();
            String name = tDicValue.getTypeValue();

            if (cellNeedLoanName.equals(name)) {
                return id;
            }
        }
        return -1;
    }
}