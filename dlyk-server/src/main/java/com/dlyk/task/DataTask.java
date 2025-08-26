package com.dlyk.task;

import com.dlyk.DlykServerApplication;
import com.dlyk.model.TActivity;
import com.dlyk.model.TDicType;
import com.dlyk.model.TDicValue;
import com.dlyk.model.TProduct;
import com.dlyk.result.DicEnum;
import com.dlyk.service.ActivityService;
import com.dlyk.service.DicTypeService;
import com.dlyk.service.ProductService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@EnableScheduling
public class DataTask {

    @Resource
    private DicTypeService dicTypeService;
    @Resource
    private ProductService productService;
    @Resource
    private ActivityService activityservice;

    @PostConstruct
    public void init() {
        System.out.println("应用启动，执行缓存初始化...");
        loadDataToCache();
    }

    @Scheduled(cron = "${project.task.cron}")
    public void Task() {
        loadDataToCache();
    }

    private void loadDataToCache() {
        System.out.println("这里面就写具体要执行的业务逻辑代码......" + new Date());
        List<TDicType> dicTypeList = dicTypeService.loadAllDicType();

        dicTypeList.forEach(tDicType -> {
            String typeCode = tDicType.getTypeCode();
            List<TDicValue> dicValueList = tDicType.getDicValueList();
            DlykServerApplication.cacheMap.put(typeCode, dicValueList);
        });

        //查询产品信息并缓存
        List<TProduct> tProductList = productService.loadAllProduct();
        DlykServerApplication.cacheMap.put(DicEnum.PRODUCT.getCode(), tProductList);

        //查询进行的活动
        List<TActivity> tActivityList = activityservice.getOngoingActivity();
        DlykServerApplication.cacheMap.put(DicEnum.ACTIVITY.getCode(), tActivityList);
    }
}
