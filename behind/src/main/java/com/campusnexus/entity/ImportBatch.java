package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_batch")
public class ImportBatch {

    @TableId(value = "batch_id", type = IdType.INPUT)
    private String batchId;

    private String fileUrl;

    private String originalFilename;

    private String fileType;

    private Long categoryId;

    private String keywords;

    private Integer knowledgeCount;

    private Integer totalChars;

    private String status;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long rolledBackBy;

    private LocalDateTime rolledBackTime;
}
