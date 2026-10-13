package com.vn.datphong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DatphongApplication {

    public static void main(String[] args) {
//        java.util.TimeZone.setDefault(
//                java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
//        );
//
//        System.out.println("JVM TimeZone = "
//                + java.util.TimeZone.getDefault().getID());

        SpringApplication.run(DatphongApplication.class, args);
    }
}
