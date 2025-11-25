package com.dlyk.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * 空值对象，用于缓存穿透防护
 * 当数据库中不存在对应数据时，缓存此对象以避免频繁查询数据库
 * 
 * @author Kilo Code
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NullValue implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 单例实例
     */
    public static final NullValue INSTANCE = new NullValue();
    
    /**
     * 私有构造函数，防止外部实例化
     */
    @JsonCreator
    private NullValue() {
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NullValue;
    }
    
    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }
    
    @Override
    public String toString() {
        return "NullValue";
    }
    
    @JsonIgnore
    public boolean isNull() {
        return true;
    }
}