package com.dlyk.task;

import com.dlyk.result.ReconcileResult;
import com.dlyk.service.ReconcileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据一致性对账任务
 *
 * <p>以「分片广播」方式调度：调度中心把任务同时下发到执行器集群的所有实例，
 * 每个实例通过 {@link XxlJobHelper#getShardIndex()} 拿到自己的分片序号，
 * 只扫描 {@code MOD(id, shardTotal) = shardIndex} 的数据。
 * 这样全表校验的耗时不会随数据量线性增长，而是随执行器数量分摊。
 *
 * <p>一旦发现不一致，任务标记为失败，由调度中心按配置的告警策略通知，
 * 实现"数据不一致主动暴露，而不是等业务方投诉"。
 *
 * @author ShigureYukina
 */
@Component
public class ReconcileJobHandler {

    private static final Logger log = LoggerFactory.getLogger(ReconcileJobHandler.class);

    @Resource
    private ReconcileService reconcileService;

    @XxlJob("reconcileJobHandler")
    public void reconcile() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        if (shardTotal <= 0) {
            shardTotal = 1;
        }
        if (shardIndex < 0 || shardIndex >= shardTotal) {
            shardIndex = 0;
        }

        ReconcileResult result = reconcileService.reconcile(shardIndex, shardTotal);
        XxlJobHelper.log(result.summary());
        log.info(result.summary());

        if (result.getTotalIssues() > 0) {
            XxlJobHelper.handleFail("发现数据不一致：" + result.summary());
        }
    }
}
