-- ============================================================
-- CampusNexus 初始化数据库结构
-- Flyway migration: V1
-- MySQL 8.0+
-- 对应文档: docs/数据库建表文档.md
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(20)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    email         VARCHAR(100) NULL COMMENT '邮箱',
    avatar        VARCHAR(512) NULL COMMENT '头像URL',
    phone         VARCHAR(20)  NULL COMMENT '手机号',
    role          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通用户,1=管理员',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用,1=正常',
    token_version INT          NOT NULL DEFAULT 0 COMMENT 'Token版本号，改密/禁用/改角色时+1',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_status_role (status, role)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 知识分类表
-- ------------------------------------------------------------
CREATE TABLE knowledge_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(32) NOT NULL COMMENT '分类名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '0=停用,1=启用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name),
    KEY idx_category_status_sort (status, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '知识分类表';

-- ------------------------------------------------------------
-- 3. 文档导入批次表
-- ------------------------------------------------------------
CREATE TABLE import_batch (
    batch_id          CHAR(36)      NOT NULL COMMENT '批次UUID',
    file_url          VARCHAR(512)  NOT NULL COMMENT 'OSS文件URL',
    original_filename VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_type         VARCHAR(32)   NOT NULL COMMENT '文件类型',
    category_id       BIGINT        NOT NULL COMMENT '分类ID',
    keywords          VARCHAR(500)  NULL COMMENT '补充关键词',
    knowledge_count   INT           NOT NULL DEFAULT 0 COMMENT '生成知识数',
    total_chars       INT           NOT NULL DEFAULT 0 COMMENT '文档总字符数',
    status            VARCHAR(16)   NOT NULL COMMENT 'SUCCESS/FAILED/ROLLED_BACK',
    error_message     VARCHAR(1000) NULL COMMENT '失败原因',
    created_by        BIGINT        NOT NULL COMMENT '上传管理员ID',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    rolled_back_by    BIGINT        NULL COMMENT '回滚操作人',
    rolled_back_time  DATETIME      NULL COMMENT '回滚时间',
    PRIMARY KEY (batch_id),
    KEY idx_batch_status_time (status, create_time),
    KEY idx_batch_category (category_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '文档导入批次表';

-- ------------------------------------------------------------
-- 4. 知识条目表
-- ------------------------------------------------------------
CREATE TABLE knowledge (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识ID',
    category_id       BIGINT       NOT NULL COMMENT '分类ID',
    title             VARCHAR(200) NULL COMMENT '标题',
    question          VARCHAR(500) NULL COMMENT '标准问题',
    answer            TEXT         NULL COMMENT '标准答案',
    keywords          VARCHAR(500) NULL COMMENT '关键词，逗号分隔',
    view_count        INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '0=下架,1=正常',
    source_type       VARCHAR(16)  NOT NULL DEFAULT 'manual' COMMENT 'manual/upload',
    file_url          VARCHAR(512) NULL COMMENT 'OSS原始文件URL',
    original_filename VARCHAR(255) NULL COMMENT '原始文件名',
    file_type         VARCHAR(32)  NULL COMMENT '文件类型',
    vector_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待同步,1=已同步,2=失败',
    vector_error      VARCHAR(500) NULL COMMENT '最近向量化失败原因',
    vector_synced_time DATETIME    NULL COMMENT '最近同步成功时间',
    import_batch_id   CHAR(36)     NULL COMMENT '上传批次UUID，手动录入为NULL',
    created_by        BIGINT       NULL COMMENT '创建管理员ID',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除,1=已删除',
    PRIMARY KEY (id),
    KEY idx_knowledge_public (status, category_id, create_time),
    KEY idx_knowledge_vector (status, vector_status),
    KEY idx_knowledge_batch (import_batch_id),
    KEY idx_knowledge_deleted (deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '知识条目表';

-- ------------------------------------------------------------
-- 5. 知识分片与 Chroma 向量 ID 映射表
-- ------------------------------------------------------------
CREATE TABLE knowledge_chunk (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分片ID',
    knowledge_id    BIGINT       NOT NULL COMMENT '知识ID',
    chunk_index     INT          NOT NULL COMMENT '分片序号，从0开始',
    chroma_id       VARCHAR(128) NOT NULL COMMENT 'Chroma向量ID',
    content         TEXT         NOT NULL COMMENT '分片文本',
    embedding_model VARCHAR(64)  NULL COMMENT 'Embedding模型',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chunk_knowledge_index (knowledge_id, chunk_index),
    KEY idx_chunk_chroma (chroma_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '知识分片与向量ID映射表';

-- ------------------------------------------------------------
-- 6. 会话表
-- ------------------------------------------------------------
CREATE TABLE conversation_session (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '内部会话ID',
    session_key   CHAR(36)     NOT NULL COMMENT '对外会话UUID',
    user_id       BIGINT       NULL COMMENT '所属用户ID，匿名会话为NULL',
    title         VARCHAR(100) NOT NULL COMMENT '会话标题',
    message_count INT          NOT NULL DEFAULT 0 COMMENT '消息数量',
    start_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time      DATETIME     NULL COMMENT '结束时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_key (session_key),
    KEY idx_session_user_time (user_id, start_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '会话表';

-- ------------------------------------------------------------
-- 7. 会话消息表
-- ------------------------------------------------------------
CREATE TABLE conversation_message (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id    BIGINT      NOT NULL COMMENT '内部会话ID',
    role          VARCHAR(16) NOT NULL COMMENT 'user/assistant/system',
    content       MEDIUMTEXT  NOT NULL COMMENT '消息内容',
    message_index INT         NOT NULL COMMENT '会话内顺序，从0开始',
    source        VARCHAR(32) NULL COMMENT 'rag_knowledge_base/ai_generated',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_session_index (session_id, message_index),
    KEY idx_message_session (session_id, message_index)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '会话消息表';

-- ------------------------------------------------------------
-- 8. 提问日志表
-- ------------------------------------------------------------
CREATE TABLE ask_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID，即接口logId',
    user_id        BIGINT       NULL COMMENT '提问用户ID，匿名日志为NULL',
    session_id     BIGINT       NOT NULL COMMENT '内部会话ID',
    question       VARCHAR(500) NOT NULL COMMENT '用户问题',
    answer_content MEDIUMTEXT   NULL COMMENT '完整回答内容',
    confidence     VARCHAR(16)  NULL COMMENT 'high/medium/low',
    source         VARCHAR(32)  NULL COMMENT 'rag_knowledge_base/ai_generated',
    match_count    INT          NOT NULL DEFAULT 0 COMMENT '合并去重后的知识命中数',
    citations_json JSON         NULL COMMENT '引用知识列表 [{knowledgeId,title}]',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    PRIMARY KEY (id),
    KEY idx_log_user_time (user_id, create_time),
    KEY idx_log_time (create_time),
    KEY idx_log_session (session_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '提问日志表';

-- ------------------------------------------------------------
-- 9. 回答反馈表
-- ------------------------------------------------------------
CREATE TABLE feedback (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    user_id                 BIGINT        NOT NULL COMMENT '反馈用户ID',
    log_id                  BIGINT        NOT NULL COMMENT '提问日志ID',
    knowledge_id            BIGINT        NULL COMMENT '关联知识ID',
    feedback_type           VARCHAR(16)   NOT NULL COMMENT 'like/dislike/report',
    content                 VARCHAR(1000) NULL COMMENT '反馈文字',
    status                  TINYINT       NOT NULL DEFAULT 0 COMMENT '0=未处理,1=已处理',
    session_id              BIGINT        NULL COMMENT '内部会话ID',
    correct_answer          VARCHAR(2000) NULL COMMENT '用户提交的正确答案',
    created_contribution_id BIGINT        NULL COMMENT '自动创建的纠错贡献ID',
    reviewed_by             BIGINT        NULL COMMENT '审核人ID',
    reviewed_time           DATETIME      NULL COMMENT '审核时间',
    review_remark           VARCHAR(500)  NULL COMMENT '审核备注',
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_user_log_type (user_id, log_id, feedback_type),
    KEY idx_feedback_status_time (status, create_time),
    KEY idx_feedback_type_status (feedback_type, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '回答反馈表';

-- ------------------------------------------------------------
-- 10. 用户贡献表
-- ------------------------------------------------------------
CREATE TABLE contribution (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '贡献ID',
    user_id       BIGINT       NOT NULL COMMENT '提交用户ID',
    category_id   BIGINT       NOT NULL COMMENT '分类ID',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    content       MEDIUMTEXT   NOT NULL COMMENT '内容',
    type          VARCHAR(16)  NOT NULL COMMENT 'qa/article/correction',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待审核,1=已通过,2=已驳回',
    submit_remark VARCHAR(500) NULL COMMENT '用户补充说明',
    review_remark VARCHAR(500) NULL COMMENT '管理员审核备注',
    reviewed_by   BIGINT       NULL COMMENT '审核人ID',
    reviewed_time DATETIME     NULL COMMENT '审核时间',
    knowledge_id  BIGINT       NULL COMMENT '审核通过后写入的知识ID',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_contribution_status_time (status, create_time),
    KEY idx_contribution_user_time (user_id, create_time),
    KEY idx_contribution_category (category_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户贡献表';

-- ------------------------------------------------------------
-- 11. 异步任务表
-- ------------------------------------------------------------
CREATE TABLE async_task (
    task_id       CHAR(36)      NOT NULL COMMENT '任务UUID',
    task_type     VARCHAR(32)   NOT NULL COMMENT 'VECTORIZE/BATCH_ROLLBACK',
    status        VARCHAR(16)   NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/PARTIAL/FAILED',
    total         INT           NOT NULL DEFAULT 0 COMMENT '总条目数',
    processed     INT           NOT NULL DEFAULT 0 COMMENT '已处理数',
    success_count INT           NOT NULL DEFAULT 0 COMMENT '成功数',
    failed_count  INT           NOT NULL DEFAULT 0 COMMENT '失败数',
    progress      INT           NOT NULL DEFAULT 0 COMMENT '进度0-100',
    payload_json  JSON          NULL COMMENT '任务参数，如{"batchId":"..."}',
    error_message VARCHAR(1000) NULL COMMENT '任务级错误',
    created_by    BIGINT        NOT NULL COMMENT '创建管理员ID',
    started_at    DATETIME      NULL COMMENT '开始时间',
    finished_at   DATETIME      NULL COMMENT '结束时间',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (task_id),
    KEY idx_task_type_status_time (task_type, status, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '异步任务表';
