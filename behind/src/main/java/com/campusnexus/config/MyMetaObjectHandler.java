package com.campusnexus.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        TableInfo tableInfo = findTableInfo(metaObject);
        if (tableInfo == null) {
            return;
        }
        boolean fillable = tableInfo.getFieldList().stream()
                .anyMatch(f -> "updateTime".equals(f.getProperty()) && f.isWithUpdateFill());
        if (fillable) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
    }
}