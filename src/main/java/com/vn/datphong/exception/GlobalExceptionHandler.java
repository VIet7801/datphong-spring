package com.vn.datphong.exception;

import com.vn.datphong.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Nhánh trả lỗi của Spring MVC. Định dạng thống nhất: success=false, message, error{code, details, traceId}.
 * Lỗi phát sinh trong filter bảo mật được ghi bởi JsonErrorWriter với cùng cấu trúc.
 * Không bao giờ trả stack trace hoặc message nội bộ của exception không lường trước.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ApiResponse<?>> build(HttpStatus status, String message, String code, Object details) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, new ApiError(code, details)));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<?>> handleDomain(DomainException ex) {
        return build(ex.getStatus(), ex.getMessage(), ex.getCode(), ex.getDetails());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Không đủ quyền truy cập", "FORBIDDEN", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Chưa xác thực", "UNAUTHORIZED", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", "VALIDATION_ERROR", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ServletRequestBindingException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<?>> handleBadInput(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ hoặc thiếu tham số", "VALIDATION_ERROR", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ", "METHOD_NOT_ALLOWED", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Kiểu nội dung không được hỗ trợ", "UNSUPPORTED_MEDIA_TYPE", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", "RESOURCE_NOT_FOUND", null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Dữ liệu đã được thay đổi bởi người khác, hãy tải lại và thử lại",
                "VERSION_CONFLICT", null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleLockTimeout(PessimisticLockingFailureException ex) {
        log.warn("Lock timeout/deadlock: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Hệ thống đang bận xử lý cùng tài nguyên, vui lòng thử lại", "RESOURCE_BUSY", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Dữ liệu xung đột với trạng thái hiện tại", "CONFLICT", null);
    }

    @ExceptionHandler(ResourceNotFoundExcept.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundExcept ex) {
        return build(HttpStatus.NOT_FOUND, "Không tồn tại", "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BadRequestExcept.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestExcept ex) {
        return build(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Chưa xác thực", "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ValidationException ex) {
        Object details = ex.getErrorMap() != null ? ex.getErrorMap() : ex.getErrors();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus());
        return build(status == null ? HttpStatus.BAD_REQUEST : status, ex.getMessage(), "BUSINESS_ERROR", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("Lỗi hệ thống không lường trước", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", "INTERNAL_SERVER_ERROR", null);
    }
}
