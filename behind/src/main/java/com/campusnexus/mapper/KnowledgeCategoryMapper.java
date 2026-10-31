package com.campusnexus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusnexus.entity.KnowledgeCategory;
import com.campusnexus.vo.AdminCategoryVO;
import com.campusnexus.vo.CategoryVO;
import com.campusnexus.vo.HotCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {

    List<CategoryVO> listEnabled();

    List<AdminCategoryVO> listAdmin(@Param("status") Integer status, @Param("keyword") String keyword);

    List<HotCategoryVO> listHotCategories(@Param("limit") int limit);
}
