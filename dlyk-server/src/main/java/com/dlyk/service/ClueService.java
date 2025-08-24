package com.dlyk.service;

import com.dlyk.model.TClue;
import com.github.pagehelper.PageInfo;

/** copy by ShigureYukina,from 2025/8/24-下午2:27 */
public interface ClueService {

    PageInfo<TClue> getCluePage(Integer current);
}
