package com.campusnexus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateDTO {

    @Size(max = 32, message = "分类名称不能超过 32 个字符")
    private String name;

    private Integer sort;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
