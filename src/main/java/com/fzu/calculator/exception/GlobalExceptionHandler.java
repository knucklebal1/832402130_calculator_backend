package com.fzu.calculator.exception;

import com.fzu.calculator.model.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器：把所有异常统一转换成前端可直接展示的 JSON 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：可预期，按错误码返回对应状态码。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        log.debug("业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code.name(), ex.getMessage()));
    }

    /** @Valid 参数校验失败，例如表达式为空。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.defaultMessage());
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
                .body(ApiError.of(ErrorCode.EXPRESSION_EMPTY.name(), message));
    }

    /** 请求体不是合法 JSON、或字段类型不匹配。 */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        log.debug("请求格式错误: {}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
                .body(ApiError.of(ErrorCode.INVALID_REQUEST.name(),
                        ErrorCode.INVALID_REQUEST.defaultMessage()));
    }

    /**
     * 兜底处理。
     *
     * <p>注意：Spring Boot 3.2 对未匹配的路径会抛出 {@link ErrorResponseException}，
     * 这里要保持它原本的 404 状态码，不能一律当成 500。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponseException errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            String detail = errorResponse.getBody() == null
                    ? ErrorCode.NOT_FOUND.defaultMessage()
                    : errorResponse.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(ApiError.of(ErrorCode.NOT_FOUND.name(), detail));
        }
        log.error("未预期的服务端异常", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiError.of(ErrorCode.INTERNAL_ERROR.name(),
                        ErrorCode.INTERNAL_ERROR.defaultMessage()));
    }
}
