package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ask_log")
public class AskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sessionId;

    private String question;

    private String answerContent;

    private String confidence;

    private String source;

    private Integer matchCount;

    private String citationsJson;

    private LocalDateTime createTime;
}
