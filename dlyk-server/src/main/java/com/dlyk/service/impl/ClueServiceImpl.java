package com.dlyk.service.impl;

import com.alibaba.excel.EasyExcel;
import com.dlyk.config.Listener.UploadDataListener;
import com.dlyk.constant.Constants;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.model.TClue;
import com.dlyk.query.BaseQuery;
import com.dlyk.query.ClueQuery;
import com.dlyk.service.ClueService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/24-下午2:27 */
@Service
public class ClueServiceImpl implements ClueService {

    @Resource
    private TClueMapper tClueMapper;

    @Override
    public PageInfo<TClue> getCluePage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TClue> list = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }

    @Override
    public void importExcel(InputStream inputStream, String token) {
        //三个参数，第一个是要读取的excel文件，第二个是要读取的pojo类型，第三个是监听器
        EasyExcel.read(inputStream, TClue.class, new UploadDataListener(tClueMapper, token)).sheet().doRead();
    }

    @Override
    public Boolean checkPhone(String phone) {
        int count = tClueMapper.selectByPhone(phone);
        return count <= 0;
    }

    @Override
    public synchronized int addClue(ClueQuery cluequery) {

        TClue tClue = new TClue();

        BeanUtils.copyProperties(cluequery, tClue);
        Integer loginUserId = JWTUtils.parseUserFromJWT(cluequery.getToken()).getId();

        tClue.setCreateBy(loginUserId);
        tClue.setCreateTime(new Date());

        return tClueMapper.insertSelective(tClue);

    }

    @Override
    public TClue getClue(Integer id) {
        return tClueMapper.selectDetailById(id);
    }

    @Override
    public int updateClue(ClueQuery cluequery) {

        TClue tClue = new TClue();

        //把ClueQuery对象里面的属性数据复制到TClue对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(cluequery, tClue);

        tClue.setEditTime(new Date());//设置编辑时间
        Integer userId = JWTUtils.parseUserFromJWT(cluequery.getToken()).getId();
        tClue.setEditBy(userId);//设置编辑人

        return tClueMapper.updateByPrimaryKeySelective(tClue);
    }
}
