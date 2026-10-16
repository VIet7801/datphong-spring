package com.vn.datphong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DatphongApplication {

    public static void main(String[] args) {
        // Chỉ khởi động ứng dụng/server một lần; mỗi HTTP request đi qua filter rồi controller.
        // Xem docs/REQUEST_FLOW.md để lần theo luồng đăng nhập và API user.
        java.util.TimeZone.setDefault(
                java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        );

        System.out.println("JVM TimeZone = "
                + java.util.TimeZone.getDefault().getID());

        SpringApplication.run(DatphongApplication.class, args);
    }
}
