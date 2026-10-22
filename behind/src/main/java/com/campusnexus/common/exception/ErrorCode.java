package com.campusnexus.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求格式错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    VALIDATION_FAILED(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "依赖服务不可用"),

    USERNAME_EXISTS(10001, "用户名已存在"),
    USER_NOT_FOUND(10002, "用户不存在"),
    PASSWORD_ERROR(10003, "密码错误"),
    TOKEN_EXPIRED(10004, "Token 已过期"),
    TOKEN_INVALID(10005, "Token 无效"),
    ACCOUNT_DISABLED(10006, "账号已禁用"),
    EMAIL_EXISTS(10007, "邮箱已存在"),

    KNOWLEDGE_NOT_FOUND(20001, "知识条目不存在"),
    CATEGORY_NOT_FOUND(20002, "分类不存在或已停用"),
    CONTRIBUTION_NOT_FOUND(20003, "贡献条目不存在"),
    CONTRIBUTION_REVIEWED(20004, "该贡献已被审核，不可重复操作"),
    FILE_TOO_LARGE(20005, "上传文件过大"),
    FILE_TYPE_UNSUPPORTED(20006, "不支持的文件格式"),
    DOCUMENT_PARSE_FAILED(20007, "文档解析失败"),
    OSS_UPLOAD_FAILED(20008, "文件上传失败"),
    LLM_SPLIT_FAILED(20009, "LLM 文档切分失败"),
    FEEDBACK_DUPLICATED(20010, "该反馈已提交，请勿重复操作"),
    FEEDBACK_PROCESSED(20011, "该反馈已处理，不可重复操作"),
    CATEGORY_IN_USE(20012, "分类正在使用，无法删除"),
    VECTOR_TASK_RUNNING(20013, "已有向量类异步任务正在执行"),
    BATCH_ROLLED_BACK(20014, "该导入批次已回滚"),
    BATCH_HAS_MODIFIED_KNOWLEDGE(20015, "批次内存在上传后修改的知识，需 force=true"),
    CANNOT_OPERATE_SELF(20016, "不能对当前登录管理员执行该操作"),
    LAST_ADMIN(20017, "系统至少保留一个管理员"),

    AI_SERVICE_ERROR(30001, "AI 服务调用失败"),
    AI_TIMEOUT(30002, "AI 服务响应超时");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
