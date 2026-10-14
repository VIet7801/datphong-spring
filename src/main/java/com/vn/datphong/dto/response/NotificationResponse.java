package com.vn.datphong.dto.response;

import com.vivu.booking.enums.NotificationChannelType;
import com.vivu.booking.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationTypeEnum notifType;
    private String title;
    private String content;
    private Boolean isRead;
    private NotificationChannelType channel;
    private LocalDateTime createdAt;
}
