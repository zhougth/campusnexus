package com.campusnexus.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryVO {

    private Long id;
    private String name;
    private Integer sort;
    private Integer status;
    private Integer knowledgeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
