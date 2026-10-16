package com.vn.datphong.enums;

/**
 * Tên hạng thành viên trong phần mở rộng khách hàng thân thiết.
 *
 * <p>Dùng tại: {@code LoyaltyRank.name}.</p>
 *
 * <p>Ngưỡng điểm và quyền lợi là dữ liệu của LoyaltyRank, không được quyết định chỉ bằng tên enum.</p>
 */
public enum RankNameType {
    /** Hạng thành viên cơ bản. */
    MEMBER,

    /** Hạng bạc. */
    SILVER,

    /** Hạng vàng. */
    GOLD,

    /** Hạng bạch kim. */
    PLATINUM,

    /** Hạng kim cương. */
    DIAMOND
}
