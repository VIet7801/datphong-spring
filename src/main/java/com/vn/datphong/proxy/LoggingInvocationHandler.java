//package com.vn.datphong.proxy;
//
//
//import lombok.extern.slf4j.Slf4j;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//import java.time.LocalDateTime;
//
//@Slf4j
//public class LoggingInvocationHandler implements InvocationHandler {
//
//    private final Object target;
//
//    public LoggingInvocationHandler(Object target) {
//        this.target = target;
//    }
//
//    @Override
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//
//        // Log trước khi chạy nghiệp vụ
//        log.info("-------------- Method {} được gọi - {}",
//                method.getName(),
//                LocalDateTime.now());
//
//        long start = System.currentTimeMillis();
//
//        try {
//            // GỌI NGHIỆP VỤ GỐC
//            Object result = method.invoke(target, args);
//
//            long end = System.currentTimeMillis();
//
//            // Log sau khi nghiệp vụ chạy thành công
//            log.info("---------- Method {} thành công - {} - {} ms",
//                    method.getName(),
//                    LocalDateTime.now(),
//                    end - start);
//
//            return result;
//
//        } catch (Throwable e) {
//
//            log.error("---------- Method {} xảy ra lỗi - {}",
//                    method.getName(),
//                    LocalDateTime.now());
//
//            throw e;
//        }
//    }
//
//    public static Object createProxy(Object target) {
//
//        return Proxy.newProxyInstance(
//                target.getClass().getClassLoader(),
//
//                // JDK Dynamic Proxy yêu cầu interface
//                target.getClass().getInterfaces(),
//
//                new LoggingInvocationHandler(target)
//        );
//    }
//}