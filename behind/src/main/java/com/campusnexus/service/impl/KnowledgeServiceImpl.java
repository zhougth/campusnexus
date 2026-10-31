package com.campusnexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusnexus.ai.vector.VectorStoreService;
import com.campusnexus.common.cache.CacheSpec;
import com.campusnexus.common.cache.RedisCacheHelper;
import com.campusnexus.common.exception.BizException;
import com.campusnexus.common.exception.ErrorCode;
import com.campusnexus.common.result.PageResultVO;
import com.campusnexus.dto.KnowledgeImportDTO;
import com.campusnexus.dto.KnowledgeListDTO;
import com.campusnexus.dto.KnowledgeUpdateDTO;
import com.campusnexus.entity.Contribution;
import com.campusnexus.entity.Knowledge;
import com.campusnexus.entity.KnowledgeCategory;
import com.campusnexus.entity.KnowledgeChunk;
import com.campusnexus.mapper.KnowledgeCategoryMapper;
import com.campusnexus.mapper.KnowledgeChunkMapper;
import com.campusnexus.mapper.KnowledgeMapper;
import com.campusnexus.security.SecurityUtils;
import com.campusnexus.service.KnowledgeService;
import com.campusnexus.vo.AdminKnowledgeVO;
import com.campusnexus.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    private static final Pattern CONTRIBUTION_QA_PATTERN =
            Pattern.compile("问\\s*[:：]\\s*(.+?)\\s*答\\s*[:：]\\s*(.+)", Pattern.DOTALL);
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int QUESTION_MAX_LENGTH = 500;
    private static final int ANSWER_MAX_LENGTH = 5000;

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeCategoryMapper knowledgeCategoryMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final RedisCacheHelper redisCacheHelper;
    private final VectorStoreService vectorStoreService;


    @Override
    public PageResultVO<KnowledgeVO> search(String keyword, Long categoryId, long page, long size) {

        long pageNo = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 100);
        if (categoryId != null && knowledgeCategoryMapper.selectById(categoryId) == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        String kw = trimToNull(keyword);
        LambdaQueryWrapper<Knowledge> wrapper = Wrappers.<Knowledge>lambdaQuery()
                .eq(Knowledge::getStatus, 1)
                .eq(categoryId != null, Knowledge::getCategoryId, categoryId);
        if (kw != null) {
            wrapper.and(w -> w
                    .like(Knowledge::getTitle, kw)
                    .or().like(Knowledge::getQuestion, kw)
                    .or().like(Knowledge::getAnswer, kw)
                    .or().like(Knowledge::getKeywords, kw));
        }
        wrapper.orderByDesc(Knowledge::getCreateTime).orderByDesc(Knowledge::getId);

        Page<Knowledge> result = knowledgeMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, String> categoryNames = loadCategoryNames(result.getRecords());

        List<KnowledgeVO> records = result.getRecords().stream()
                .map(item -> toKnowledgeVO(item, categoryNames.get(item.getCategoryId())))
                .toList();

        return PageResultVO.of(result.getTotal(), result.getCurrent(), result.getSize(), records);

    }

    @Override
    public KnowledgeVO detail(Long id) {
        int updated = knowledgeMapper.update(null, Wrappers.<Knowledge>lambdaUpdate()
                .eq(Knowledge::getId, id)
                .eq(Knowledge::getStatus, 1)
                .setSql("view_count = view_count + 1, update_time = update_time"));

        if (updated == 0) {
            throw new BizException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        }

        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null || !Integer.valueOf(1).equals(knowledge.getStatus())) {
            throw new BizException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        }

        KnowledgeCategory category = knowledgeCategoryMapper.selectById(knowledge.getCategoryId());
        return toKnowledgeVO(knowledge, category == null ? null : category.getName());
    }

    @Override
    public PageResultVO<AdminKnowledgeVO> adminList(KnowledgeListDTO query) {
        long pageNo = query.getPage() == null ? 1 : Math.max(query.getPage(), 1);
        long pageSize = query.getSize() == null ? 10 : Math.min(Math.max(query.getSize(), 1), 100);
        String kw = trimToNull(query.getKeyword());
        String batchId = trimToNull(query.getImportBatchId());

        LambdaQueryWrapper<Knowledge> wrapper = Wrappers.<Knowledge>lambdaQuery()
                .eq(query.getCategoryId() != null, Knowledge::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Knowledge::getStatus, query.getStatus())
                .eq(batchId != null, Knowledge::getImportBatchId, batchId);

        if (kw != null) {
            wrapper.and(w -> w
                    .like(Knowledge::getTitle, kw)
                    .or().like(Knowledge::getQuestion, kw)
                    .or().like(Knowledge::getAnswer, kw)
                    .or().like(Knowledge::getKeywords, kw));
        }
        wrapper.orderByDesc(Knowledge::getCreateTime).orderByDesc(Knowledge::getId);

        Page<Knowledge> result = knowledgeMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, String> categoryNames = loadCategoryNames(result.getRecords());

        List<AdminKnowledgeVO> records = result.getRecords().stream()
                .map(item -> toAdminKnowledgeVO(item, categoryNames.get(item.getCategoryId())))
                .toList();

        return PageResultVO.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional
    public Long create(KnowledgeImportDTO dto) {
        KnowledgeCategory category = knowledgeCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        String title = trimToNull(dto.getTitle());
        String question = trimToNull(dto.getQuestion());
        String answer = trimToNull(dto.getAnswer());
        validateContent(title, question, answer);

        Knowledge knowledge = new Knowledge();
        knowledge.setCategoryId(dto.getCategoryId());
        knowledge.setTitle(title);
        knowledge.setQuestion(question);
        knowledge.setAnswer(answer);
        knowledge.setKeywords(trimToNull(dto.getKeywords()));
        knowledge.setViewCount(0);
        knowledge.setStatus(1);
        knowledge.setSourceType("manual");
        knowledge.setVectorStatus(0);
        knowledge.setCreatedBy(SecurityUtils.userId());

        knowledgeMapper.insert(knowledge);
        evictCategoryCache();

        return knowledge.getId();
    }

    @Override
    @Transactional
    public void update(Long id, KnowledgeUpdateDTO dto) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BizException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        }

        boolean hasField = dto.getCategoryId() != null
                || dto.getTitle() != null
                || dto.getQuestion() != null
                || dto.getAnswer() != null
                || dto.getKeywords() != null
                || dto.getStatus() != null;
        if (!hasField) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "至少提交一个字段");
        }
        boolean statusChanged = dto.getStatus() != null
                && !dto.getStatus().equals(knowledge.getStatus());
        Long categoryId = dto.getCategoryId();
        if (categoryId != null) {
            KnowledgeCategory category = knowledgeCategoryMapper.selectById(categoryId);
            if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
                throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        String newTitle = dto.getTitle() != null ? trimToNull(dto.getTitle()) : null;
        String newQuestion = dto.getQuestion() != null ? trimToNull(dto.getQuestion()) : null;
        String newAnswer = dto.getAnswer() != null ? trimToNull(dto.getAnswer()) : null;
        String newKeywords = dto.getKeywords() != null ? trimToNull(dto.getKeywords()) : null;

        String finalTitle = dto.getTitle() != null ? newTitle : knowledge.getTitle();
        String finalQuestion = dto.getQuestion() != null ? newQuestion : knowledge.getQuestion();
        String finalAnswer = dto.getAnswer() != null ? newAnswer : knowledge.getAnswer();
        validateContent(finalTitle, finalQuestion, finalAnswer);

        boolean contentChanged =
                (dto.getTitle() != null && !Objects.equals(newTitle, knowledge.getTitle()))
                        || (dto.getQuestion() != null && !Objects.equals(newQuestion, knowledge.getQuestion()))
                        || (dto.getAnswer() != null && !Objects.equals(newAnswer, knowledge.getAnswer()))
                        || (dto.getKeywords() != null && !Objects.equals(newKeywords, knowledge.getKeywords()));

        LambdaUpdateWrapper<Knowledge> update = Wrappers.<Knowledge>lambdaUpdate()
                .eq(Knowledge::getId, id);
        if (contentChanged) {
            update.set(Knowledge::getVectorStatus, 0)
                    .set(Knowledge::getVectorError, null)
                    .set(Knowledge::getVectorSyncedTime, null)
                    .set(Knowledge::getUpdateTime, LocalDateTime.now());
        } else {
            // 显式保留旧值，避免 status/category 变更触发 ON UPDATE CURRENT_TIMESTAMP
            update.set(Knowledge::getUpdateTime, knowledge.getUpdateTime());
        }
        if (categoryId != null) {
            update.set(Knowledge::getCategoryId, categoryId);
        }
        if (dto.getTitle() != null) {
            update.set(Knowledge::getTitle, newTitle);
        }
        if (dto.getQuestion() != null) {
            update.set(Knowledge::getQuestion, newQuestion);
        }
        if (dto.getAnswer() != null) {
            update.set(Knowledge::getAnswer, newAnswer);
        }
        if (dto.getKeywords() != null) {
            update.set(Knowledge::getKeywords, newKeywords);
        }
        if (dto.getStatus() != null) {
            update.set(Knowledge::getStatus, dto.getStatus());
        }

        knowledgeMapper.update(null, update);

        if (statusChanged || (categoryId != null && !categoryId.equals(knowledge.getCategoryId()))) {
            evictCategoryCache();
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            throw new BizException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        }

        List<String> chromaIds = knowledgeChunkMapper.selectList(
                        Wrappers.<KnowledgeChunk>lambdaQuery()
                                .eq(KnowledgeChunk::getKnowledgeId, id))
                .stream()
                .map(KnowledgeChunk::getChromaId)
                .filter(StringUtils::hasText)
                .toList();

        knowledgeMapper.deleteById(id);
        knowledgeChunkMapper.delete(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getKnowledgeId, id));
        evictCategoryCache();
        deleteVectorsAfterCommit(chromaIds);
    }

    @Override
    @Transactional
    public Long createFromContribution(Contribution contribution, Long categoryId) {
        String question = null;
        String answer = truncate(contribution.getContent(), ANSWER_MAX_LENGTH);
        if ("qa".equals(contribution.getType())) {
            Matcher matcher = CONTRIBUTION_QA_PATTERN.matcher(nullToEmpty(contribution.getContent()));
            if (matcher.find()) {
                question = truncate(matcher.group(1), QUESTION_MAX_LENGTH);
                answer = truncate(matcher.group(2), ANSWER_MAX_LENGTH);
            }
        }

        Knowledge knowledge = new Knowledge();
        knowledge.setCategoryId(categoryId);
        knowledge.setTitle(truncate(contribution.getTitle(), TITLE_MAX_LENGTH));
        knowledge.setQuestion(question);
        knowledge.setAnswer(answer);
        knowledge.setViewCount(0);
        knowledge.setStatus(1);
        knowledge.setSourceType("manual");
        knowledge.setVectorStatus(0);
        knowledge.setCreatedBy(SecurityUtils.userId());

        knowledgeMapper.insert(knowledge);
        evictCategoryCache();

        return knowledge.getId();
    }

    private String truncate(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void validateContent(String title, String question, String answer) {
        if (!StringUtils.hasText(title)
                && !StringUtils.hasText(question)
                && !StringUtils.hasText(answer)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "标题、标准问题、答案至少填写一项");
        }
        if (StringUtils.hasText(question) && !StringUtils.hasText(answer)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "填写标准问题时必须填写答案");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, String> loadCategoryNames(List<Knowledge> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> categoryIds = records.stream()
                .map(Knowledge::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return knowledgeCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
    }

    private KnowledgeVO toKnowledgeVO(Knowledge entity, String categoryName) {
        KnowledgeVO vo = new KnowledgeVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCategoryName(categoryName);
        return vo;
    }

    private AdminKnowledgeVO toAdminKnowledgeVO(Knowledge entity, String categoryName) {
        AdminKnowledgeVO vo = new AdminKnowledgeVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCategoryName(categoryName);
        return vo;
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

    private void deleteVectorsAfterCommit(List<String> chromaIds) {
        if (chromaIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteVectorsQuietly(chromaIds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteVectorsQuietly(chromaIds);
            }
        });
    }

    private void deleteVectorsQuietly(List<String> chromaIds) {
        try {
            vectorStoreService.delete(chromaIds);
        } catch (Exception e) {
            log.warn("清理知识向量失败，已跳过（检索端会按 MySQL 状态过滤）: {}", e.getMessage());
        }
    }
}
