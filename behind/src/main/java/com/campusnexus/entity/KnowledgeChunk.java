package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeId;

    private Integer chunkIndex;

    private String chromaId;

    private String content;

    private String embeddingModel;

    private LocalDateTime createTime;
}
