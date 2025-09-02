package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TTranHistoryMapper;
import com.dlyk.mapper.TTranMapper;
import com.dlyk.mapper.TTranRemarkMapper;
import com.dlyk.model.TTran;
import com.dlyk.model.TTranHistory;
import com.dlyk.model.TTranRemark;
import com.dlyk.query.TranQuery;
import com.dlyk.query.TranRemarkQuery;
import com.dlyk.service.TranService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 交易服务实现类
 */
@Service
public class TranServiceImpl implements TranService {
    
    @Resource
    private TTranMapper tTranMapper;
    
    @Resource
    private TTranRemarkMapper tTranRemarkMapper;
    
    @Resource
    private TTranHistoryMapper tTranHistoryMapper;
    
    @Override
    public PageInfo<TTran> getTranPage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        TranQuery tranQuery = new TranQuery();
        List<TTran> list = tTranMapper.selectTranPage(tranQuery);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
    
    @Override
    public PageInfo<TTran> getTranPage(Integer current, TranQuery tranQuery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        if (tranQuery == null) {
            tranQuery = new TranQuery();
        }
        List<TTran> list = tTranMapper.selectTranPage(tranQuery);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        
        // 把TranQuery对象里的属性数据复制到TTran对象里去
        BeanUtils.copyProperties(tranQuery, tTran);
        tTran.setCreateTime(new Date()); // 创建时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setCreateBy(loginUserId); // 创建人
        
        // 生成交易编号
        String tranNo = "TRAN" + System.currentTimeMillis();
        tTran.setTranNo(tranNo);
        
        // 设置初始阶段
        if (tTran.getStage() == null) {
            tTran.setStage(1); // 初步洽谈，使用Integer类型
        }
        
        int result = tTranMapper.insertSelective(tTran);
        
        // 添加交易历史记录
        if (result > 0) {
            TTranHistory tranHistory = new TTranHistory();
            tranHistory.setTranId(tTran.getId());
            tranHistory.setStage(tTran.getStage());
            tranHistory.setMoney(tTran.getMoney());
            tranHistory.setExpectedDate(tTran.getExpectedDate());
            tranHistory.setCreateBy(loginUserId);
            tranHistory.setCreateTime(new Date());
            tTranHistoryMapper.insertSelective(tranHistory);
        }
        
        return result;
    }
    
    @Override
    public TTran getTranDetail(Integer id) {
        return tTranMapper.selectTranDetailById(id);
    }
    
    @Override
    public int updateTran(TranQuery tranQuery) {
        TTran tTran = new TTran();
        
        // 把TranQuery对象里的属性数据复制到TTran对象里去
        BeanUtils.copyProperties(tranQuery, tTran);
        tTran.setEditTime(new Date()); // 编辑时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setEditBy(loginUserId); // 编辑人
        
        return tTranMapper.updateByPrimaryKeySelective(tTran);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTranStage(TranQuery tranQuery) {
        TTran tTran = new TTran();
        
        // 把TranQuery对象里的属性数据复制到TTran对象里去
        BeanUtils.copyProperties(tranQuery, tTran);
        tTran.setEditTime(new Date()); // 编辑时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(tranQuery.getToken()).getId();
        tTran.setEditBy(loginUserId); // 编辑人
        
        int result = tTranMapper.updateByPrimaryKeySelective(tTran);
        
        // 添加交易历史记录
        if (result > 0) {
            TTranHistory tranHistory = new TTranHistory();
            tranHistory.setTranId(tTran.getId());
            tranHistory.setStage(tTran.getStage());
            tranHistory.setMoney(tTran.getMoney());
            tranHistory.setExpectedDate(tTran.getExpectedDate());
            tranHistory.setCreateBy(loginUserId);
            tranHistory.setCreateTime(new Date());
            tTranHistoryMapper.insertSelective(tranHistory);
        }
        
        return result;
    }
    
    @Override
    public int addTranRemark(TranRemarkQuery tranRemarkQuery) {
        TTranRemark tTranRemark = new TTranRemark();
        
        // 把TranRemarkQuery对象里的属性数据复制到tTranRemark对象里去
        BeanUtils.copyProperties(tranRemarkQuery, tTranRemark);
        tTranRemark.setCreateTime(new Date()); // 创建时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(tranRemarkQuery.getToken()).getId();
        tTranRemark.setCreateBy(loginUserId); // 创建人
        
        return tTranRemarkMapper.insertSelective(tTranRemark);
    }
    
    @Override
    public PageInfo<TTranRemark> getTranRemarkPage(Integer current, Integer tranId) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TTranRemark> list = tTranRemarkMapper.selectByTranId(tranId);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
    
    @Override
    public List<TTranHistory> getTranHistory(Integer tranId) {
        return tTranHistoryMapper.selectByTranId(tranId);
    }
}