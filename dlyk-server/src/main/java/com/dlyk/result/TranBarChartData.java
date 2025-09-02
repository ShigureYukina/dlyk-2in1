package com.dlyk.result;

import java.util.List;

/**
 * 交易柱状图数据结果类
 * 用于封装交易统计的双系列柱状图数据
 */
public class TranBarChartData {
    /**
     * X轴数据（负责人名称列表）
     */
    private List<String> x;
    
    /**
     * Y1轴数据（交易总数）
     */
    private List<Integer> y1;
    
    /**
     * Y2轴数据（成交数）
     */
    private List<Integer> y2;

    public TranBarChartData() {
    }

    public TranBarChartData(List<String> x, List<Integer> y1, List<Integer> y2) {
        this.x = x;
        this.y1 = y1;
        this.y2 = y2;
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<Integer> getY1() {
        return y1;
    }

    public void setY1(List<Integer> y1) {
        this.y1 = y1;
    }

    public List<Integer> getY2() {
        return y2;
    }

    public void setY2(List<Integer> y2) {
        this.y2 = y2;
    }

    @Override
    public String toString() {
        return "TranBarChartData{" +
                "x=" + x +
                ", y1=" + y1 +
                ", y2=" + y2 +
                '}';
    }
}