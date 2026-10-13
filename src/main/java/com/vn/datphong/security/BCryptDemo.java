package com.vn.datphong.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class BCryptDemo {
    public static void main(String[] args) {
        // Khởi tạo bộ băm BCrypt
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Mật khẩu gốc
        String rawPassword = "123456";

        // Tạo chuỗi BCrypt
        String bcryptPassword = encoder.encode(rawPassword);

        // In ra màn hình
        System.out.println(bcryptPassword);
    }
}