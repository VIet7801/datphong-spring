package com.vn.datphong.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Around("execution(* com.vn.datphong.service.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        LocalDateTime startTime = LocalDateTime.now();
        log.info("{} bắt đầu chạy lúc {}", methodName, startTime.format(FORMATTER));

        try {
            Object result = joinPoint.proceed();
            LocalDateTime endTime = LocalDateTime.now();
            log.info("{} chạy xong lúc {}", methodName, endTime.format(FORMATTER));
            return result;
        } catch (Throwable ex) {
            LocalDateTime errorTime = LocalDateTime.now();
            log.error("{} phát sinh lỗi lúc {}", methodName, errorTime.format(FORMATTER), ex);
            throw ex;
        }
    }
}
