package com.dlyk.web;

import com.dlyk.DlykServerApplication;
import com.dlyk.model.TActivity;
import com.dlyk.model.TDicValue;
import com.dlyk.model.TProduct;
import com.dlyk.result.DicEnum;
import com.dlyk.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/26-下午2:00 */
@RestController
public class DicController {
    @GetMapping(value = "/api/dicvalue/{typeCode}")
    public R dicData(@PathVariable("typeCode") String typeCode) {
        if (typeCode.equals(DicEnum.ACTIVITY.getCode())) {
            List<TActivity> tActivityList = (List<TActivity>) DlykServerApplication.cacheMap.get(typeCode);
            return R.OK(tActivityList);
        } else if (typeCode.equals(DicEnum.PRODUCT.getCode())) {
            List<TProduct> tProductList = (List<TProduct>) DlykServerApplication.cacheMap.get(typeCode);
            return R.OK(tProductList);
        }
        List<TDicValue> tDicValueList = (List<TDicValue>) DlykServerApplication.cacheMap.get(typeCode);
        return R.OK(tDicValueList);
    }

}
