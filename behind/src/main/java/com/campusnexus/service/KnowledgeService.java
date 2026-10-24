package com.campusnexus.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusnexus.common.result.PageResultVO;
import com.campusnexus.dto.KnowledgeImportDTO;
import com.campusnexus.dto.KnowledgeListDTO;
import com.campusnexus.dto.KnowledgeUpdateDTO;
import com.campusnexus.entity.Knowledge;
import com.campusnexus.vo.AdminKnowledgeVO;
import com.campusnexus.vo.KnowledgeVO;

public interface KnowledgeService extends IService<Knowledge> {
    PageResultVO<KnowledgeVO> search(String keyword, Long categoryId, long page, long size);
    KnowledgeVO detail(Long id);
    PageResultVO<AdminKnowledgeVO> adminList(KnowledgeListDTO query);
    Long create(KnowledgeImportDTO dto);
    void update(Long id, KnowledgeUpdateDTO dto);
    void delete(Long id);

}
