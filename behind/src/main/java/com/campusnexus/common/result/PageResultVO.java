package com.campusnexus.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> {

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResultVO<T> of(long total, long page, long size, List<T> records) {
        return new PageResultVO<>(total, page, size, records);
    }
}
