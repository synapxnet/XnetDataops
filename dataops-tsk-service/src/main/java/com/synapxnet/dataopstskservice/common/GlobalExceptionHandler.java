package com.synapxnet.dataopstskservice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 将输入参数错误转换为平台统一的业务错误响应。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.ok(Result.error(400, e.getMessage()));
    }

    /** 将认证和授权拒绝转换为对应 HTTP 状态，避免被统一包装为 500。 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<?>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String reason = exception.getReason() == null ? "Request rejected" : exception.getReason();
        log.warn("Request rejected with status {}: {}", status, reason);
        return ResponseEntity.status(exception.getStatusCode()).body(Result.error(status, reason));
    }

    /** 将未分类异常记录后转换为不暴露内部细节的服务错误响应。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("Internal error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(500, "Internal server error: " + e.getMessage()));
    }
}
