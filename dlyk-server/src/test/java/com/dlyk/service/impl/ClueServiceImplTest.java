package com.dlyk.service.impl;

import com.dlyk.mapper.TClueMapper;
import com.dlyk.service.ClueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 线索服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ClueServiceImplTest {

    @Mock
    private TClueMapper tClueMapper;

    @InjectMocks
    private ClueServiceImpl clueService;

    @Test
    void testBatchDeleteClue_Success() {
        // 准备测试数据
        List<Integer> idList = Arrays.asList(1, 2, 3);
        when(tClueMapper.batchDeleteByIds(idList)).thenReturn(3);

        // 执行测试
        int result = clueService.batchDeleteClue(idList);

        // 验证结果
        assertEquals(3, result, "应该成功删除3条记录");
        verify(tClueMapper, times(1)).batchDeleteByIds(idList);
    }

    @Test
    void testBatchDeleteClue_EmptyList() {
        // 测试空列表
        List<Integer> emptyList = Collections.emptyList();

        // 执行测试
        int result = clueService.batchDeleteClue(emptyList);

        // 验证结果
        assertEquals(0, result, "空列表应该返回0");
        verify(tClueMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void testBatchDeleteClue_NullList() {
        // 测试null列表
        int result = clueService.batchDeleteClue(null);

        // 验证结果
        assertEquals(0, result, "null列表应该返回0");
        verify(tClueMapper, never()).batchDeleteByIds(anyList());
    }

    @Test
    void testBatchDeleteClue_PartialSuccess() {
        // 准备测试数据 - 部分成功的情况
        List<Integer> idList = Arrays.asList(1, 2, 3, 4, 5);
        when(tClueMapper.batchDeleteByIds(idList)).thenReturn(3); // 只删除了3条

        // 执行测试
        int result = clueService.batchDeleteClue(idList);

        // 验证结果
        assertEquals(3, result, "应该返回实际删除的记录数");
        verify(tClueMapper, times(1)).batchDeleteByIds(idList);
    }

    @Test
    void testDeleteClue_Success() {
        // 准备测试数据
        Integer clueId = 1;
        when(tClueMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        // 执行测试
        int result = clueService.deleteClue(clueId);

        // 验证结果
        assertEquals(1, result, "应该成功删除1条记录");
        verify(tClueMapper, times(1)).updateByPrimaryKeySelective(any());
    }

    @Test
    void testDeleteClue_Failed() {
        // 准备测试数据
        Integer clueId = 1;
        when(tClueMapper.updateByPrimaryKeySelective(any())).thenReturn(0);

        // 执行测试
        int result = clueService.deleteClue(clueId);

        // 验证结果
        assertEquals(0, result, "删除失败应该返回0");
        verify(tClueMapper, times(1)).updateByPrimaryKeySelective(any());
    }
}