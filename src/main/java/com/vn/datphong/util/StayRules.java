package com.vn.datphong.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class StayRules {
    private StayRules() {
    }

    public static final class InvalidStayException extends RuntimeException {
        private final String code;

        public InvalidStayException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * @param today ngày hiện tại theo múi giờ của cơ sở (không phải múi giờ server)
     */
    public static int validate(LocalDate checkIn, LocalDate checkOut, LocalDate today, int maxNights) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidStayException("VALIDATION_ERROR", "Cần nhập ngày nhận và ngày trả phòng");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidStayException("VALIDATION_ERROR", "Ngày trả phòng phải sau ngày nhận phòng");
        }
        if (checkIn.isBefore(today)) {
            throw new InvalidStayException("VALIDATION_ERROR", "Ngày nhận phòng không được ở quá khứ");
        }
        int n = nights(checkIn, checkOut);
        if (n > maxNights) {
            throw new InvalidStayException("VALIDATION_ERROR", "Tối đa " + maxNights + " đêm cho mỗi lần đặt");
        }
        return n;
    }
}

