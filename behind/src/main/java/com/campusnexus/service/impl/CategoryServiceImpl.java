package com.campusnexus.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusnexus.common.cache.CacheSpec;
import com.campusnexus.common.cache.RedisCacheHelper;
import com.campusnexus.common.exception.BizException;
import com.campusnexus.common.exception.ErrorCode;
import com.campusnexus.dto.CategoryCreateDTO;
import com.campusnexus.dto.CategoryUpdateDTO;
import com.campusnexus.entity.Contribution;
import com.campusnexus.entity.Knowledge;
import com.campusnexus.entity.KnowledgeCategory;
import com.campusnexus.mapper.ContributionMapper;
import com.campusnexus.mapper.KnowledgeCategoryMapper;
import com.campusnexus.mapper.KnowledgeMapper;
import com.campusnexus.service.CategoryService;
import com.campusnexus.vo.AdminCategoryVO;
import com.campusnexus.vo.CategoryVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategory>
        implements CategoryService {

    private final KnowledgeCategoryMapper knowledgeCategoryMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final ContributionMapper contributionMapper;
    private final RedisCacheHelper redisCacheHelper;
    private static final TypeReference<List<CategoryVO>> CATEGORY_LIST_TYPE = new TypeReference<>() {};
    @Override
    public List<CategoryVO> listEnabled() {
        List<CategoryVO> list = redisCacheHelper.get(CacheSpec.CATEGORY_ENABLED.key(), CATEGORY_LIST_TYPE);
        if(list != null){
            return  list;
        }
        List<CategoryVO> categoryVOS = knowledgeCategoryMapper.listEnabled();
        redisCacheHelper.set(CacheSpec.CATEGORY_ENABLED.key(), categoryVOS, CacheSpec.CATEGORY_ENABLED.ttl());
        return categoryVOS;
    }

    @Override
    public List<AdminCategoryVO> listAdmin(Integer status, String keyword) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return knowledgeCategoryMapper.listAdmin(status, normalizedKeyword);
    }

    @Override
    @Transactional
    public Long create(CategoryCreateDTO dto) {
        String name = dto.getName().trim();
        ensureNameAvailable(name, null);

        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(name);
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        knowledgeCategoryMapper.insert(category);
        evictCategoryCache();
        return category.getId();

    }

    @Override
    @Transactional
    public void update(Long id, CategoryUpdateDTO dto) {
        KnowledgeCategory category = requireCategory(id);
        boolean changed = false;

        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "分类名称不能为空");
            }
            if (!name.equals(category.getName())) {
                ensureNameAvailable(name, id);
                category.setName(name);
            }
            changed = true;
        }
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
            changed = true;
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
            changed = true;
        }
        if (!changed) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "至少提交一个字段");
        }
        knowledgeCategoryMapper.updateById(category);
        evictCategoryCache();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireCategory(id);

        Long knowledgeCount = knowledgeMapper.selectCount(
                Wrappers.<Knowledge>lambdaQuery().eq(Knowledge::getCategoryId, id));
        Long contributionCount = contributionMapper.selectCount(
                Wrappers.<Contribution>lambdaQuery().eq(Contribution::getCategoryId, id));
        if (knowledgeCount > 0 || contributionCount > 0) {
            throw new BizException(ErrorCode.CATEGORY_IN_USE);
        }
        knowledgeCategoryMapper.deleteById(id);
        evictCategoryCache();
    }

    private KnowledgeCategory requireCategory(Long id) {
        KnowledgeCategory category = knowledgeCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        Long count = knowledgeCategoryMapper.selectCount(Wrappers.<KnowledgeCategory>lambdaQuery()
                .eq(KnowledgeCategory::getName, name)
                .ne(excludeId != null, KnowledgeCategory::getId, excludeId));

        if (count > 0) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "分类名称已存在");
        }
    }

    private void evictCategoryCache() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisCacheHelper.evict(CacheSpec.CATEGORY_ENABLED.key());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisCacheHelper.evict(CacheSpec.CATEGORY_ENABLED.key());
            }
        });
    }
}
