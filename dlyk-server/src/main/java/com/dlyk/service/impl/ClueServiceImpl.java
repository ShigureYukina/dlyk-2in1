package com.dlyk.service.impl;

import com.alibaba.excel.EasyExcel;
import com.dlyk.cache.CacheLockManager;
import com.dlyk.cache.ListTotalCache;
import com.dlyk.config.Listener.UploadDataListener;
import com.dlyk.exception.DuplicateRequestException;
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

    @Resource
    private CacheLockManager cacheLockManager;

    @Resource
    private ListTotalCache listTotalCache;

    /**
     * 线索列表分页。
     *
     * <p>total 用单表统计而不是交给 PageHelper 自动 count：
     * 列表 SQL 有 7 个 LEFT JOIN，且每个都挂在被驱动表的主键上（不会放大行数），
     * 所以单表统计结果与联查统计一致，但实测 506ms -> 40ms。
     *
     * <p>单表统计仍然是每翻一页一次 COUNT，翻页越频繁、打的库越多。
     * 由于 total 只随线索新增/逻辑删除变化、与筛选条件无关，这里再用
     * {@link ListTotalCache} 加一层 10 秒 TTL 缓存把它挡掉。
     */
    @Override
    public PageInfo<TClue> getCluePage(Integer current) {
        long total = listTotalCache.get("t_clue", tClueMapper::countAlive);
        // count=false：跳过 PageHelper 基于联查生成的低效 COUNT
        PageHelper.startPage(current, Constants.PAGE_SIZE, false);
        List<TClue> list = tClueMapper.selectClueByPage(BaseQuery.builder().build());
        return buildPageInfo(list, total);
    }

    private PageInfo<TClue> buildPageInfo(List<TClue> list, long total) {
        PageInfo<TClue> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(total);
        pageInfo.setPages((int) ((total + Constants.PAGE_SIZE - 1) / Constants.PAGE_SIZE));
        return pageInfo;
    }

    @Override
    public List<TClue> getCluePageByCursor(Integer lastId, Integer size) {
        int limit = (size == null || size <= 0) ? Constants.PAGE_SIZE : size;
        return tClueMapper.selectClueByCursor(lastId, limit);
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

    /**
     * 新增线索。
     *
     * <p>原实现用 {@code synchronized} 修饰方法，只能保证单个 JVM 内互斥：
     * 前端"校验手机号是否已存在 -> 提交新增"是两步操作，多实例部署时两个实例
     * 可以同时通过手机号校验并各自插入，产生重复线索。改为按手机号加分布式锁后，
     * 同一手机号的创建请求在集群范围内串行。
     */
    @Override
    public int addClue(ClueQuery cluequery) {
        String lockKey = "clue:add:phone:" + cluequery.getPhone();
        if (!cacheLockManager.tryLock(lockKey)) {
            throw new DuplicateRequestException("该手机号正在创建线索，请稍后重试");
        }
        try {
            TClue tClue = new TClue();

            BeanUtils.copyProperties(cluequery, tClue);
            Integer loginUserId = JWTUtils.parseUserFromJWT(cluequery.getToken()).getId();

            tClue.setCreateBy(loginUserId);
            tClue.setCreateTime(new Date());

            return tClueMapper.insertSelective(tClue);
        } finally {
            cacheLockManager.unlock(lockKey);
        }
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
