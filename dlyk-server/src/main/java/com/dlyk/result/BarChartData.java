package com.dlyk.result;

import java.util.List;

/**
 * 柱状图数据结果类
 * 用于封装单系列柱状图的数据
 */
public class BarChartData {
    /**
     * X轴数据（类目轴数据）
     */
    private List<String> x;
    
    /**
     * Y轴数据（数值轴数据）
     */
    private List<Integer> y;

    public BarChartData() {
    }

    public BarChartData(List<String> x, List<Integer> y) {
        this.x = x;
        this.y = y;
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<Integer> getY() {
        return y;
    }

    public void setY(List<Integer> y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "BarChartData{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}