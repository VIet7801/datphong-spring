//package com.vn.datphong.config;
//
//import com.vn.datphong.proxy.LoggingInvocationHandler;
//import com.vn.datphong.service.Impl.RoleServiceImpl;
//import com.vn.datphong.service.RoleService;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//import java.lang.reflect.Proxy;
//
//@Configuration
//public class ProxyConfig {
//
//    @Bean
//    @Primary
//    public RoleService roleServiceProxy(RoleServiceImpl roleServiceImpl) {
//
//        return (RoleService) Proxy.newProxyInstance(
//                RoleService.class.getClassLoader(),
//                new Class<?>[]{RoleService.class},
//                new LoggingInvocationHandler(roleServiceImpl)
//        );
//    }
//}