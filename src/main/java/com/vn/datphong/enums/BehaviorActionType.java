package com.vn.datphong.enums;

/**
 * Tập mã hành vi cũ được giữ để tương thích với mô hình hiện có.
 *
 * <p>Dùng tại: {@code UserBehaviorLog.actionType}.</p>
 *
 * <p>Không suy diễn ý nghĩa các mã này. Phân loại sự kiện funnel dùng UserBehaviorLog.eventType với FunnelEventType.</p>
 */
public enum BehaviorActionType {
    /** Mã HI cũ, chưa có định nghĩa nghiệp vụ trong catalog hoặc nơi sử dụng hiện tại. */
    HI,

    /** Mã HIE cũ, chưa có định nghĩa nghiệp vụ trong catalog hoặc nơi sử dụng hiện tại. */
    HIE,

    /** Mã HIA cũ, chưa có định nghĩa nghiệp vụ trong catalog hoặc nơi sử dụng hiện tại. */
    HIA
}
