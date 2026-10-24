package com.campusnexus.controller;

import com.campusnexus.common.result.PageResultVO;
import com.campusnexus.common.result.Result;
import com.campusnexus.service.KnowledgeService;
import com.campusnexus.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/common")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/search")
    public Result<PageResultVO<KnowledgeVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(knowledgeService.search(keyword, null, page, size));
    }

    @GetMapping("/search/category")
    public Result<PageResultVO<KnowledgeVO>> searchByCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(knowledgeService.search(null, categoryId, page, size));
    }

    @GetMapping("/knowledge/{id}")
    public Result<KnowledgeVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.detail(id));
    }
}
