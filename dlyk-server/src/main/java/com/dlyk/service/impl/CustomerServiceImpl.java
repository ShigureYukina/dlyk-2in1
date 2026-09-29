package com.dlyk.service.impl;

import com.dlyk.cache.CacheLockManager;
import com.dlyk.cache.ListTotalCache;
import com.dlyk.constant.Constants;
import com.dlyk.event.CustomerConvertedEvent;
import com.dlyk.mq.CustomerConvertedEventPublisher;
import com.dlyk.exception.DuplicateRequestException;
import com.dlyk.manager.CustomerManager;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.model.TCustomer;
import com.dlyk.query.CustomerQuery;
import com.dlyk.result.CustomerExcel;
import com.dlyk.service.CustomerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/26-下午10:13 */
@Service
public class CustomerServiceImpl implements CustomerService {
    @Resource
    private CustomerManager customerManager;
    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private CacheLockManager cacheLockManager;

    @Resource
    private ListTotalCache listTotalCache;

    @Resource
    private CustomerConvertedEventPublisher eventPublisher;

    /**
     * 线索转客户。
     *
     * <p>原实现是"先查状态再更新"的检查后执行（check-then-act）：并发下两个请求
     * 可能同时读到 state != -1，从而各自插入一条客户记录，产生重复客户。
     *
     * <p>此处用分布式锁按 clueId 串行化同一线索的转换。锁必须在事务之外获取、
     * 且在 {@link CustomerManager#convertCustomer} 提交之后释放 —— 若把锁放在事务方法内部，
     * 锁会在事务提交前释放，后一个线程仍能读到未提交前的旧状态，互斥就形同虚设。
     */
    @Override
    public Boolean convertCustomer(CustomerQuery customerquery) {
        Integer clueId = customerquery.getClueId();
        if (clueId == null) {
            throw new DuplicateRequestException("线索ID不能为空");
        }

        String lockKey = "customer:convert:" + clueId;
        if (!cacheLockManager.tryLock(lockKey)) {
            throw new DuplicateRequestException("该线索正在转换中，请稍后重试");
        }
        try {
            Boolean converted = customerManager.convertCustomer(customerquery);

            // 事件必须在事务提交之后发布，否则消费方可能读到未提交的数据。
            // convertCustomer 是另一个 Bean 上的 @Transactional 方法，
            // 该方法返回即代表事务已提交，因此此处发布是安全的。
            if (Boolean.TRUE.equals(converted)) {
                eventPublisher.publish(new CustomerConvertedEvent(clueId, new Date()));
            }
            return converted;
        } finally {
            cacheLockManager.unlock(lockKey);
        }
    }

    /**
     * 客户列表分页。
     *
     * <p>同线索列表：total 走单表统计。客户列表 SQL 有 9 个 LEFT JOIN，
     * PageHelper 自动 count 实测 978ms，单表统计 28ms。
     * 再由 {@link ListTotalCache} 加 10 秒 TTL 缓存，避免每次翻页都打一次 COUNT。
     */
    @Override
    public PageInfo<TCustomer> getCustomerByPage(Integer current) {
        long total = listTotalCache.get("t_customer", tCustomerMapper::countAlive);
        PageHelper.startPage(current, Constants.PAGE_SIZE, false);
        List<TCustomer> list = this.tCustomerMapper.selectCustomerPage();

        PageInfo<TCustomer> info = new PageInfo<>(list);
        info.setTotal(total);
        info.setPages((int) ((total + Constants.PAGE_SIZE - 1) / Constants.PAGE_SIZE));
        return info;
    }


    @Override
    public List<CustomerExcel> getCustomerByExcel(List<Integer> idList) {
        List<CustomerExcel> customerExcelList = new ArrayList<>();
        List<TCustomer> tCustomerList = this.tCustomerMapper.selectCustomerExcel(idList);

        //现在需要把 List<TCustomer> 的数据转换到 List<CustomerExcel>

        tCustomerList.forEach(tCustomer -> {
            CustomerExcel customerExcel = new CustomerExcel();

            customerExcel.setOwnerName(tCustomer.getOwnerDO() != null ? tCustomer.getOwnerDO().getName() : Constants.EMPTY);
            customerExcel.setActivityName(tCustomer.getActivityDO() != null ? tCustomer.getActivityDO().getName() : "");

            if (tCustomer.getClueDO() != null) {
                customerExcel.setFullName(tCustomer.getClueDO().getFullName() != null ? tCustomer.getClueDO().getFullName() : "");
                customerExcel.setPhone(tCustomer.getClueDO().getPhone() != null ? tCustomer.getClueDO().getPhone() : "");
                customerExcel.setWeixin(tCustomer.getClueDO().getWeixin() != null ? tCustomer.getClueDO().getWeixin() : "");
                customerExcel.setQq(tCustomer.getClueDO().getQq() != null ? tCustomer.getClueDO().getQq() : "");
                customerExcel.setEmail(tCustomer.getClueDO().getEmail() != null ? tCustomer.getClueDO().getEmail() : "");
                customerExcel.setAge(tCustomer.getClueDO().getAge() != null ? tCustomer.getClueDO().getAge() : 0);
                customerExcel.setJob(tCustomer.getClueDO().getJob() != null ? tCustomer.getClueDO().getJob() : "");
                customerExcel.setYearIncome(tCustomer.getClueDO().getYearIncome());
                customerExcel.setAddress(tCustomer.getClueDO().getAddress() != null ? tCustomer.getClueDO().getAddress() : "");
            } else {
                // 设置默认值
                customerExcel.setFullName("");
                customerExcel.setPhone("");
                customerExcel.setWeixin("");
                customerExcel.setQq("");
                customerExcel.setEmail("");
                customerExcel.setAge(0);
                customerExcel.setJob("");
                customerExcel.setYearIncome(null);
                customerExcel.setAddress("");
            }

            customerExcel.setNeedLoadName(tCustomer.getNeedLoanDO() != null ? tCustomer.getNeedLoanDO().getTypeValue() : "");
            customerExcel.setProductName(tCustomer.getIntentionProductDO() != null ? tCustomer.getIntentionProductDO().getName() : "");
            customerExcel.setSourceName(tCustomer.getSourceDO() != null ? tCustomer.getSourceDO().getTypeValue() : "");
            customerExcel.setDescription(tCustomer.getDescription());
            customerExcel.setNextContactTime(tCustomer.getNextContactTime());

            customerExcelList.add(customerExcel);
        });

        return customerExcelList;
    }
    
    @Override
    public TCustomer getCustomerDetail(Integer id) {
        return tCustomerMapper.selectCustomerDetailById(id);
    }

    @Override
    public int deleteCustomer(Integer id) {
        // 逻辑删除，与线索模块保持一致
        return tCustomerMapper.logicalDeleteById(id);
    }
}


