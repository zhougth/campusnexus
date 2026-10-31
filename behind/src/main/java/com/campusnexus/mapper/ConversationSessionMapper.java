package com.campusnexus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusnexus.entity.ConversationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {

    @Select("SELECT * FROM conversation_session WHERE id = #{id} FOR UPDATE")
    ConversationSession selectForUpdate(@Param("id") Long id);
}
