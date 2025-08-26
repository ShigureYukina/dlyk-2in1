package com.dlyk.web;

import com.dlyk.model.TClue;
import com.dlyk.query.ClueQuery;
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

    @GetMapping(value = "/api/clue/{phone}")
    public R checkPhone(@PathVariable("phone") String phone) {
        Boolean check = clueService.checkPhone(phone);
        return check ? R.OK() : R.FAIL();
    }

    @PostMapping(value = "/api/clue")
    public R addClue(ClueQuery cluequery, @RequestHeader(value = "Authorization") String token) {
        cluequery.setToken(token);
        int save = clueService.addClue(cluequery);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/clue/detail/{id}")
    public R getClue(@PathVariable("id") Integer id) {
        TClue clue = clueService.getClue(id);
        return clue != null ? R.OK(clue) : R.FAIL();
    }

    @PutMapping(value = "/api/clue")
    public R updateClue(ClueQuery cluequery, @RequestHeader(value = "Authorization") String token) {
        cluequery.setToken(token);
        int update = clueService.updateClue(cluequery);
        return update >= 1 ? R.OK() : R.FAIL();
    }

}