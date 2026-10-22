package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String question;

    private String answer;

    private String keywords;

    private Integer viewCount;

    private Integer status;

    private String sourceType;

    private String fileUrl;

    private String originalFilename;

    private String fileType;

    private Integer vectorStatus;

    private String vectorError;

    private LocalDateTime vectorSyncedTime;

    private String importBatchId;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
