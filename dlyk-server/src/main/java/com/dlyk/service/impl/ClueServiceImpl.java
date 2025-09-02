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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
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
    
    @Override
    public int deleteClue(Integer id) {
        // 逻辑删除，将deleted字段设置为1
        TClue tClue = new TClue();
        tClue.setId(id);
        tClue.setDeleted(1);
        return tClueMapper.updateByPrimaryKeySelective(tClue);
    }
    
    @Override
    public int batchDeleteClue(List<Integer> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        // 批量逻辑删除
        return tClueMapper.batchDeleteByIds(idList);
    }
    
    @Override
    public void exportClueExcel(HttpServletResponse response) throws Exception {
        // 查询所有线索数据
        List<TClue> clueList = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        
        // 设置文件名
        String fileName = URLEncoder.encode("线索数据_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        
        // 导出数据
        EasyExcel.write(response.getOutputStream(), TClue.class)
                .sheet("线索数据")
                .doWrite(clueList);
    }
    
    @Override
    public void exportSelectedClue(List<Integer> idList, HttpServletResponse response) throws Exception {
        if (idList == null || idList.isEmpty()) {
            throw new RuntimeException("请选择要导出的线索");
        }
        
        // 根据ID列表查询线索数据
        List<TClue> clueList = tClueMapper.selectCluesByIds(idList);
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        
        // 设置文件名
        String fileName = URLEncoder.encode("线索数据_选中_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        
        // 导出数据
        EasyExcel.write(response.getOutputStream(), TClue.class)
                .sheet("线索数据")
                .doWrite(clueList);
    }
}
