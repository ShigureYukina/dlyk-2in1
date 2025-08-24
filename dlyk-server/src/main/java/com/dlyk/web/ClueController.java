package com.dlyk.web;

import com.dlyk.model.TClue;
import com.dlyk.model.TUser;
import com.dlyk.result.R;
import com.dlyk.service.ClueService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** copy by ShigureYukina,from 2025/8/24-下午2:25 */
@RestController
public class ClueController {

    @Resource
    private ClueService clueService;
    @GetMapping(value = "/api/clues")
    public R CluePage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TClue> pageInfo = clueService.getCluePage(current);
        return R.OK(pageInfo);
    }
}
