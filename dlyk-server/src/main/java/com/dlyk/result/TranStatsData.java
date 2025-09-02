package com.dlyk.result;

/**
 * 交易统计数据类
 * 用于封装按负责人统计的交易数据
 */
public class TranStatsData {
    /**
     * 负责人姓名
     */
    private String ownerName;
    
    /**
     * 交易总数
     */
    private Integer totalTranCount;
    
    /**
     * 成交数
     */
    private Integer successTranCount;

    public TranStatsData() {
    }

    public TranStatsData(String ownerName, Integer totalTranCount, Integer successTranCount) {
        this.ownerName = ownerName;
        this.totalTranCount = totalTranCount;
        this.successTranCount = successTranCount;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Integer getTotalTranCount() {
        return totalTranCount;
    }

    public void setTotalTranCount(Integer totalTranCount) {
        this.totalTranCount = totalTranCount;
    }

    public Integer getSuccessTranCount() {
        return successTranCount;
    }

    public void setSuccessTranCount(Integer successTranCount) {
        this.successTranCount = successTranCount;
    }

    @Override
    public String toString() {
        return "TranStatsData{" +
                "ownerName='" + ownerName + '\'' +
                ", totalTranCount=" + totalTranCount +
                ", successTranCount=" + successTranCount +
                '}';
    }
}