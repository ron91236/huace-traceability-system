package com.huace.trace.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.error(403, "无权限访问");
    }

    /**
     * 唯一键冲突的原文是一整段带 ### 的 SQL 加索引名，拼进"系统内部错误: "就是乱码。
     * 服务层的预检挡得住常见路径，两个请求同时提交仍会撞到这里，所以兜底也得说人话。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突: {}", e.getMessage());
        return Result.error("保存失败：内容有重复（如登录账号、编码已存在），请检查后重试");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统内部错误: " + e.getMessage());
    }
}
