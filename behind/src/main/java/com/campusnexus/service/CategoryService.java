package com.campusnexus.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusnexus.dto.CategoryCreateDTO;
import com.campusnexus.dto.CategoryUpdateDTO;
import com.campusnexus.entity.KnowledgeCategory;
import com.campusnexus.vo.AdminCategoryVO;
import com.campusnexus.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<KnowledgeCategory> {

    List<CategoryVO> listEnabled();
    List<AdminCategoryVO> listAdmin(Integer status, String keyword);
    Long create(CategoryCreateDTO dto);
    void update(Long id, CategoryUpdateDTO dto);
    void delete(Long id);
}
