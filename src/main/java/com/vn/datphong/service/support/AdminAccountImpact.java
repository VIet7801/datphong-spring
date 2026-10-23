package com.vn.datphong.service.support;

import com.vn.datphong.entity.*;
import com.vn.datphong.enums.BookingStatusType;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AdminAccountImpact {
    @PersistenceContext
    private EntityManager entities;

    public List<String> warnings(Long userId) {
        long properties = entities.createQuery("select count(p) from Property p where p.owner.id = :id", Long.class)
                .setParameter("id", userId).getSingleResult();
        long bookings = entities.createQuery("""
                select count(b) from Booking b
                where (b.user.id = :id or b.property.owner.id = :id) and b.status in :states
                """, Long.class).setParameter("id", userId)
                .setParameter("states", EnumSet.of(BookingStatusType.HOLD, BookingStatusType.PENDING_PAYMENT,
                        BookingStatusType.CONFIRMED, BookingStatusType.CHECKED_IN,
                        BookingStatusType.CANCELLATION_PENDING, BookingStatusType.REFUND_PENDING))
                .getSingleResult();
        List<String> warnings = new ArrayList<>();
        if (properties > 0) warnings.add("Tài khoản đang sở hữu " + properties + " cơ sở lưu trú; dữ liệu được giữ nguyên");
        if (bookings > 0) warnings.add("Có " + bookings + " booking chưa kết thúc; cần bố trí người tiếp tục xử lý");
        return warnings;
    }
}
