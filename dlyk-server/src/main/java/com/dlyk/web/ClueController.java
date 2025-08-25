package com.dlyk.web;

import com.dlyk.model.TClue;
import com.dlyk.result.R;
import com.dlyk.service.ClueService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/api/importExcel")
    public R importExcel(MultipartFile file, @RequestHeader(value = "Authorization") String token) throws Exception {//file的文件名要和前端传过来的文件名一致
        clueService.importExcel(file.getInputStream(), token);
        return R.OK();
    }

}

