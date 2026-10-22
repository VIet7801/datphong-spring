package com.vn.datphong.util;

import com.vn.datphong.enums.BookingStatusType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.vn.datphong.enums.BookingStatusType.*;
import static com.vn.datphong.enums.BookingStatusType.CHECKED_IN;

/** Bảng chuyển trạng thái hợp lệ của booking (đặc tả mục 4). Không thay thế kiểm tra quyền và thời gian. */
public final class BookingStateMachine {

    private static final Map<BookingStatusType, Set<BookingStatusType>> ALLOWED = new EnumMap<>(BookingStatusType.class);

    static {
        ALLOWED.put(HOLD, EnumSet.of(PENDING_PAYMENT, EXPIRED, CANCELLED));
        ALLOWED.put(PENDING_PAYMENT, EnumSet.of(CONFIRMED, EXPIRED, CANCELLED, NEEDS_ATTENTION));
        ALLOWED.put(CONFIRMED, EnumSet.of(CHECKED_IN, CANCELLED, REFUND_PENDING, CANCELLATION_PENDING, NO_SHOW, NEEDS_ATTENTION));
        ALLOWED.put(CHECKED_IN, EnumSet.of(COMPLETED, NEEDS_ATTENTION));
        ALLOWED.put(CANCELLATION_PENDING, EnumSet.of(CANCELLED, REFUND_PENDING, NEEDS_ATTENTION));
        ALLOWED.put(REFUND_PENDING, EnumSet.of(CANCELLED, REFUNDED, NEEDS_ATTENTION));
        ALLOWED.put(EXPIRED, EnumSet.of(NEEDS_ATTENTION));
        ALLOWED.put(NEEDS_ATTENTION, EnumSet.of(EXPIRED, CANCELLED, CONFIRMED, REFUND_PENDING));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(BookingStatusType.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(BookingStatusType.class));
        ALLOWED.put(REFUNDED, EnumSet.noneOf(BookingStatusType.class));
        ALLOWED.put(NO_SHOW, EnumSet.noneOf(BookingStatusType.class));
    }

    private BookingStateMachine() {
    }

    public static boolean canTransition(BookingStatusType from, BookingStatusType to) {
        Set<BookingStatusType> next = ALLOWED.get(from);
        return next != null && next.contains(to);
    }

    public static void assertTransition(BookingStatusType from, BookingStatusType to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Chuyển trạng thái booking không hợp lệ: " + from + " -> " + to);
        }
    }

    /** Trạng thái còn chiếm kho. EXPIRED/CANCELLED/REFUND_PENDING (đã chấp nhận hủy) không chiếm kho. */
    public static boolean occupiesInventory(BookingStatusType status) {
        return status == HOLD || status == PENDING_PAYMENT || status == CONFIRMED || status == CHECKED_IN;
    }

    public static boolean isTerminal(BookingStatusType status) {
        return ALLOWED.getOrDefault(status, Set.of()).isEmpty();
    }
}
