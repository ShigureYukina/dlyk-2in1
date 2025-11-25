package com.dlyk.cache;

import com.dlyk.mapper.TActivityMapper;
import com.dlyk.mapper.TDicTypeMapper;
import com.dlyk.mapper.TDicValueMapper;
import com.dlyk.mapper.TUserMapper;
import com.dlyk.model.TActivity;
import com.dlyk.model.TDicType;
import com.dlyk.model.TDicValue;
import com.dlyk.model.TUser;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缓存预热服务
 * 应用启动时自动预加载热点数据到缓存
 * 
 * @author Kilo Code
 */
@Component
public class CacheWarmupService implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(CacheWarmupService.class);
    
    @Resource
    private TwoLevelCacheManager cacheManager;
    
    @Resource
    private TDicTypeMapper dicTypeMapper;
    
    @Resource
    private TDicValueMapper dicValueMapper;
    
    @Resource
    private TUserMapper userMapper;
    
    @Resource
    private TActivityMapper activityMapper;
    
    /**
     * 应用启动后执行缓存预热
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("================== 开始缓存预热 ==================");
        long startTime = System.currentTimeMillis();
        
        try {
            // 预热字典数据
            warmupDictionaryData();
            
            // 预热用户数据
            warmupUserData();
            
            // 预热活动数据
            warmupActivityData();
            
            long endTime = System.currentTimeMillis();
            log.info("================== 缓存预热完成，耗时: {} ms ==================", 
                    endTime - startTime);
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }
    
    /**
     * 预热字典数据
     * 字典数据变化频率低，是典型的热点数据
     */
    private void warmupDictionaryData() {
        try {
            // 预热所有字典类型
            List<TDicType> dicTypes = dicTypeMapper.selectByAll();
            if (dicTypes != null && !dicTypes.isEmpty()) {
                cacheManager.put("dicTypeCache", "allDicTypes", dicTypes);
                log.info("已预热字典类型数据，数量: {}", dicTypes.size());
            }
            
            // 预热所有字典值
            List<TDicValue> dicValues = dicTypeMapper.selectByAll().stream()
                .filter(tDicType -> tDicType.getDicValueList() != null) // 过滤掉dicValueList为null的项
                .flatMap(tDicType -> tDicType.getDicValueList().stream())
                .filter(dicValue -> dicValue != null) // 过滤掉null的字典值
                .distinct()
                .toList();
            if (dicValues != null && !dicValues.isEmpty()) {
                cacheManager.put("dicValueCache", "allDicValues", dicValues);
                log.info("已预热字典值数据，数量: {}", dicValues.size());
                
                // 按字典类型分组缓存
                dicValues.stream()
                    .filter(dicValue -> dicValue.getTypeCode() != null) // 确保typeCode不为null
                    .collect(java.util.stream.Collectors.groupingBy(TDicValue::getTypeCode))
                    .forEach((typeCode, values) -> {
                        String key = "dicValuesByType:" + typeCode;
                        cacheManager.put("dicValueCache", key, values);
                    });
                log.info("已预热分组字典值数据");
            }
        } catch (Exception e) {
            log.error("预热字典数据失败", e);
        }
    }
    
    /**
     * 预热用户数据
     * 预加载负责人列表等常用数据
     */
    private void warmupUserData() {
        try {
            // 预热所有用户列表（可根据实际情况调整）
            List<TUser> users = userMapper.selectByOwner();
            if (users != null && !users.isEmpty()) {
                cacheManager.put("userCache", "allUsers", users);
                log.info("已预热用户数据，数量: {}", users.size());
            }
        } catch (Exception e) {
            log.error("预热用户数据失败", e);
        }
    }
    
    /**
     * 预热活动数据
     * 预加载进行中的活动
     */
    private void warmupActivityData() {
        try {
            // 预热正在进行的活动
            List<TActivity> ongoingActivities = activityMapper.selectOngoingActivity();
            if (ongoingActivities != null && !ongoingActivities.isEmpty()) {
                cacheManager.put("ongoingActivityCache", "ongoingActivities", ongoingActivities);
                log.info("已预热进行中的活动数据，数量: {}", ongoingActivities.size());
            }
        } catch (Exception e) {
            log.error("预热活动数据失败", e);
        }
    }
    
    /**
     * 手动触发缓存预热
     * 可通过管理接口调用
     */
    public void manualWarmup() {
        log.info("手动触发缓存预热");
        run(null);
    }
}