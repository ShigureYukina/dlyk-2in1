package com.dlyk.manager;

/** copy by ShigureYukina,from 2025/8/26-下午10:16 */

import com.dlyk.mapper.TClueMapper;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.model.TClue;
import com.dlyk.model.TCustomer;
import com.dlyk.query.CustomerQuery;
import com.dlyk.util.JWTUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public class CustomerManager {

    @Resource
    TCustomerMapper tCustomerMapper;
    @Resource
    TClueMapper tClueMapper;

    @Transactional(rollbackFor = Exception.class)
    public Boolean convertCustomer(CustomerQuery customerquery) {
        TClue tclue = tClueMapper.selectByPrimaryKey(customerquery.getClueId());
        if (tclue == null) {
            throw new RuntimeException("线索不存在");
        }
        if (tclue.getState() == -1) {
            throw new RuntimeException("该线索已被转换");
        }
        
        TCustomer tCustomer = new TCustomer();
        // 从线索信息中复制基本信息到客户
        tCustomer.setClueId(tclue.getId());
        
        // 把CustomerQuery对象里的属性数据复制到TCustomer 对象里去
        BeanUtils.copyProperties(customerquery, tCustomer);
        tCustomer.setCreateTime(new Date()); // 创建时间

        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(customerquery.getToken()).getId();
        tCustomer.setCreateBy(loginUserId); // 创建人

        int insert = tCustomerMapper.insertSelective(tCustomer);
        if (insert < 1) {
            throw new RuntimeException("客户插入失败，线索转换已终止");
        }

        // 更新线索状态为已转换
        tclue.setState(-1);
        int update = tClueMapper.updateByPrimaryKeySelective(tclue);
        if (update < 1) {
            // 插入成功但状态更新失败：必须抛异常触发回滚。
            // 若只 return false，事务照常提交，会留下"客户已落库、线索仍可再次转换"的部分成功脏数据
            throw new RuntimeException("线索状态更新失败，客户创建已回滚");
        }

        return true;
    }
}
