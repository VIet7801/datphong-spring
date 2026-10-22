package com.vn.datphong.exception;

import com.vn.datphong.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Nhánh trả lỗi của Spring MVC khi controller/service ném exception hoặc validation thất bại.
 * Lỗi filter trả bằng response.sendError(...) không được tự động bọc bởi advice này.
 * Dùng một advice thống nhất cho lỗi controller/service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<?>> handleMissingEndpoint(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(
                "API hoac tai nguyen khong ton tai", new ApiError("RESOURCE_NOT_FOUND", null)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<?>> handleInvalidInput(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Du lieu khong hop le hoac chua truong khong duoc phep", new ApiError("VALIDATION_ERROR", null)));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<?>> handleVersionConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                "Du lieu da thay doi, hay tai lai", new ApiError("VERSION_CONFLICT", null)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleIntegrityConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                "Du lieu trung hoac dang duoc su dung", new ApiError("DATA_CONFLICT", null)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Khong du quyen truy cap", new ApiError("FORBIDDEN", null)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Chua xac thuc", new ApiError("UNAUTHORIZED", null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Du lieu khong hop le",
                        new ApiError("VALIDATION_ERROR", errors)
                ));
    }

    @ExceptionHandler(ResourceNotFoundExcept.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundExcept ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "Khong ton tai",
                        new ApiError("RESOURCE_NOT_FOUND", ex.getMessage())
                ));
    }

    @ExceptionHandler(BadRequestExcept.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestExcept ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Yeu cau khong hop le",
                        new ApiError("BAD_REQUEST", ex.getMessage())
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "Chua xac thuc",
                        new ApiError("UNAUTHORIZED", ex.getMessage())
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(ValidationException ex) {
        Object details = ex.getErrorMap() != null ? ex.getErrorMap() : ex.getErrors();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        new ApiError("VALIDATION_ERROR", details)
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(
                        ex.getMessage(),
                        new ApiError(ex.getCode(), ex.getMessage())
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Loi he thong",
                        new ApiError("INTERNAL_SERVER_ERROR", null)
                ));
    }
}
