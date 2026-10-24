package com.campusnexus.controller;

import com.campusnexus.common.result.PageResultVO;
import com.campusnexus.common.result.Result;
import com.campusnexus.dto.KnowledgeImportDTO;
import com.campusnexus.dto.KnowledgeListDTO;
import com.campusnexus.dto.KnowledgeUpdateDTO;
import com.campusnexus.service.KnowledgeService;
import com.campusnexus.vo.AdminKnowledgeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/knowledge")
public class AdminKnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public Result<PageResultVO<AdminKnowledgeVO>> list(@Valid KnowledgeListDTO query) {
        return Result.success(knowledgeService.adminList(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody KnowledgeImportDTO dto) {
        return Result.success(knowledgeService.create(dto));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeUpdateDTO dto) {
        knowledgeService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }
}
