package com.campusnexus.controller;

import com.campusnexus.common.result.Result;
import com.campusnexus.service.CategoryService;
import com.campusnexus.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common/categories")
@RequiredArgsConstructor
public class CommonCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> getCategories() {
        return Result.success(categoryService.listEnabled());
    }
}
