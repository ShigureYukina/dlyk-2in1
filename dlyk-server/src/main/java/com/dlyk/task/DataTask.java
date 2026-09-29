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
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class DataTask {

    private static final Logger log = LoggerFactory.getLogger(DataTask.class);

    @Resource
    private DicTypeService dicTypeService;
    @Resource
    private ProductService productService;
    @Resource
    private ActivityService activityservice;

    /**
     * 应用启动时预热一次本地缓存，避免首个请求穿透到数据库。
     * 这是启动动作而非定时任务，因此保留 @PostConstruct。
     */
    @PostConstruct
    public void init() {
        log.info("应用启动，执行缓存预热...");
        loadDataToCache();
    }

    /**
     * 字典 / 产品 / 进行中活动的周期性缓存预热。
     *
     * <p>原 {@code @Scheduled(cron = "${project.task.cron}")} 已迁移至 XXL-Job 调度中心：
     * 调度周期、失败重试次数、执行日志与告警策略统一在平台侧配置，
     * 应用不再各自维护 cron 表达式。
     */
    @XxlJob("cacheWarmupJobHandler")
    public void warmupCache() {
        try {
            loadDataToCache();
            XxlJobHelper.log("缓存预热完成");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            XxlJobHelper.log("缓存预热失败: {}", e.getMessage());
            XxlJobHelper.handleFail("缓存预热失败: " + e.getMessage());
        }
    }

    private void loadDataToCache() {
        log.info("开始执行缓存预热任务, 时间: {}", new Date());
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
