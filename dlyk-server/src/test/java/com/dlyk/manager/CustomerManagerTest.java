package com.dlyk.service.impl;

import com.dlyk.manager.CustomerManager;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.model.TClue;
import com.dlyk.model.TCustomer;
import com.dlyk.model.TUser;
import com.dlyk.query.CustomerQuery;
import com.dlyk.util.JWTUtils;
import com.dlyk.util.JSONUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 客户管理器单元测试
 */
@ExtendWith(MockitoExtension.class)
class CustomerManagerTest {

    @Mock
    private TCustomerMapper tCustomerMapper;

    @Mock
    private TClueMapper tClueMapper;

    @InjectMocks
    private CustomerManager customerManager;

    private CustomerQuery customerQuery;
    private TClue mockClue;

    @BeforeEach
    void setUp() {
        customerQuery = new CustomerQuery();
        customerQuery.setClueId(1);
        // 用真实签发的 JWT，而不是随便一个字符串。
        // 假 token 会在 JWTUtils.parseUserFromJWT 处直接抛解析异常，
        // 使后面的 Mapper 桩函数永远执行不到，触发 Mockito 严格模式的
        // UnnecessaryStubbingException —— 测试看起来"过了"，其实什么都没验证到。
        TUser loginUser = new TUser();
        loginUser.setId(1001);
        customerQuery.setToken(JWTUtils.createJWT(JSONUtils.toJSON(loginUser)));
        customerQuery.setDescription("测试客户转换");

        mockClue = new TClue();
        mockClue.setId(1);
        mockClue.setState(1); // 正常状态
        mockClue.setFullName("张三");
        mockClue.setPhone("13800138000");
    }

    @Test
    void testConvertCustomer_Success() {
        // 准备测试数据
        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(mockClue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(1);

        // 执行测试
        Boolean result = customerManager.convertCustomer(customerQuery);

        // 验证结果
        assertTrue(result, "线索转换为客户应该成功");

        // 验证方法调用
        verify(tClueMapper, times(1)).selectByPrimaryKey(1);
        verify(tCustomerMapper, times(1)).insertSelective(any(TCustomer.class));
        verify(tClueMapper, times(1)).updateByPrimaryKeySelective(any(TClue.class));
    }

    @Test
    void testConvertCustomer_ClueNotFound() {
        // 准备测试数据 - 线索不存在
        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(null);

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(customerQuery);
        });

        assertEquals("线索不存在", exception.getMessage());
        verify(tClueMapper, times(1)).selectByPrimaryKey(1);
        verify(tCustomerMapper, never()).insertSelective(any(TCustomer.class));
    }

    @Test
    void testConvertCustomer_ClueAlreadyConverted() {
        // 准备测试数据 - 线索已转换
        mockClue.setState(-1); // 已转换状态
        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(mockClue);

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(customerQuery);
        });

        assertEquals("该线索已被转换", exception.getMessage());
        verify(tClueMapper, times(1)).selectByPrimaryKey(1);
        verify(tCustomerMapper, never()).insertSelective(any(TCustomer.class));
    }

    @Test
    void testConvertCustomer_DatabaseError() {
        // 准备测试数据
        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(mockClue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(0); // 插入失败

        // 执行测试并验证异常 - 插入失败必须抛异常让事务回滚，而不是返回 false 静默提交
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(customerQuery);
        });

        assertEquals("客户插入失败，线索转换已终止", exception.getMessage());
        verify(tClueMapper, never()).updateByPrimaryKeySelective(any(TClue.class));
    }

    @Test
    void testConvertCustomer_PartialSuccessMustRollback() {
        // 准备测试数据 - 插入成功但线索状态更新失败：这是最危险的"部分成功"场景。
        // 若只 return false，事务会带着"客户已落库、线索未标记转换"的中间状态照常提交
        when(tClueMapper.selectByPrimaryKey(1)).thenReturn(mockClue);
        when(tCustomerMapper.insertSelective(any(TCustomer.class))).thenReturn(1);
        when(tClueMapper.updateByPrimaryKeySelective(any(TClue.class))).thenReturn(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerManager.convertCustomer(customerQuery);
        });

        assertEquals("线索状态更新失败，客户创建已回滚", exception.getMessage());
    }
}