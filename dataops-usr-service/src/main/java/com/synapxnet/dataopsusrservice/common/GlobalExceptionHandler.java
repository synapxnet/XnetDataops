package com.synapxnet.dataopsusrservice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TOKEN_ERROR_MESSAGE = "Invalid or expired token";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        if (TOKEN_ERROR_MESSAGE.equals(e.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, e.getMessage()));
        }
        return ResponseEntity.ok(Result.error(400, e.getMessage()));
    }

    /**
     * 将已认证但无管理权限的访问转换为 HTTP 403。
     *
     * @param exception 管理权限拒绝异常
     * @return 标准禁止访问响应
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Result<?>> handleForbiddenOperation(ForbiddenOperationException exception) {
        log.warn("Forbidden request: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(403, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("Internal error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "Internal server error: " + e.getMessage()));
    }
}
