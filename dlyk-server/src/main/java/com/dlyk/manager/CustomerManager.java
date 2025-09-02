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

        // 更新线索状态为已转换
        tclue.setState(-1);
        int update = tClueMapper.updateByPrimaryKeySelective(tclue);

        return update >= 1 && insert >= 1;
    }
}
