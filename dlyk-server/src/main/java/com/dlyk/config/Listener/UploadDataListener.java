package com.dlyk.config.Listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.model.TClue;
import com.dlyk.model.TUser;
import com.dlyk.util.JSONUtils;
import com.dlyk.util.JWTUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

/**
 *每读一行数据就调用invoke方法，读完后触发doAfterAllAnalysed方法
 */

@Slf4j
public class UploadDataListener implements ReadListener<TClue> {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 5;
    private List<TClue> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    /**
     * 假设这个是一个DAO，当然有业务逻辑这个也可以是一个service。当然如果不用存储这个对象没用。
     */
    private TClueMapper tclueMapper;
    private String token;

    /**
     * 如果使用了spring,请使用这个构造方法。每次创建Listener的时候需要把spring管理的类传进来
     *
     * @param tclueMapper
     */
    public UploadDataListener(TClueMapper tclueMapper, String token) {
        this.tclueMapper = tclueMapper;
        this.token = token;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    one row value. It is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(TClue data, AnalysisContext context) {
        log.info("解析到一条数据:{}", JSONUtils.toJSON(data));
        //设置创建时间与创建人
        data.setCreateTime(new Date());
        TUser tUser = JWTUtils.parseUserFromJWT(token);
        data.setCreateBy(tUser.getId());

        cachedDataList.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        if (cachedDataList == null || cachedDataList.isEmpty()) {
            log.warn("缓存数据列表为空，跳过数据库存储");
            return;
        }
        log.info("{}条数据，开始存储数据库！", cachedDataList.size());
        try {
            tclueMapper.saveClue(cachedDataList);
            log.info("存储数据库成功！");
        } catch (Exception e) {
            log.error("批量插入数据失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
