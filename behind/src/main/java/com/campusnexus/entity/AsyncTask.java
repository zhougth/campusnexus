package com.campusnexus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask {

    @TableId(value = "task_id", type = IdType.INPUT)
    private String taskId;

    private String taskType;

    private String status;

    private Integer total;

    private Integer processed;

    private Integer successCount;

    private Integer failedCount;

    private Integer progress;

    private String payloadJson;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
