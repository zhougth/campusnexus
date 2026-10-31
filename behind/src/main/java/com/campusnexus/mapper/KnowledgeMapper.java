package com.campusnexus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusnexus.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /**
     * 全文索引召回：按 BM25 相关度排序，取代原先按 view_count 排序的截断方式。
     *
     * <p>使用 BOOLEAN MODE 而非 NATURAL LANGUAGE MODE，原因有三：
     * 一是语义显式——无前缀词项是 OR 语义（命中任一即召回），需要精确控制时还能加
     * {@code +}（必须包含）、{@code -}（排除）、{@code "…"}（短语），扩展性更好；
     * 二是 BOOLEAN MODE 不过滤内置停用词表，短词项也能参与匹配；
     * 三是相关度同样由 InnoDB 的 BM25/TF-IDF 计算，可直接 ORDER BY。</p>
     *
     * <p>注：NATURAL LANGUAGE MODE 的「50% 阈值」（词出现在超过半数行则权重归零）
     * 只作用于 MyISAM 表，InnoDB 不受影响。本项目是 InnoDB，选 BOOLEAN 并非为了绕开该阈值。</p>
     *
     * <p>需要先执行 {@code docs/sql/fulltext-index.sql} 建索引，并通过
     * {@code app.chat.fulltext-enabled=true} 开启；未开启时走 LIKE 兜底路径。</p>
     */
    @Select("""
            SELECT * FROM knowledge
            WHERE status = 1
              AND deleted = 0
              AND MATCH(title, question, keywords, answer) AGAINST(#{query} IN BOOLEAN MODE)
            ORDER BY MATCH(title, question, keywords, answer) AGAINST(#{query} IN BOOLEAN MODE) DESC,
                     id DESC
            LIMIT #{limit}
            """)
    List<Knowledge> searchByFulltext(@Param("query") String query, @Param("limit") int limit);
}
