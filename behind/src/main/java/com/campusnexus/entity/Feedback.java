package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long logId;

    private Long knowledgeId;

    private String feedbackType;

    private String content;

    private Integer status;

    private Long sessionId;

    private String correctAnswer;

    private Long createdContributionId;

    private Long reviewedBy;

    private LocalDateTime reviewedTime;

    private String reviewRemark;

    private LocalDateTime createTime;
}
