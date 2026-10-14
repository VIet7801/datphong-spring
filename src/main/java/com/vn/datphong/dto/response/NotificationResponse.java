package com.vn.datphong.dto.response;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.enums.NotificationChannelType;
import com.vn.datphong.enums.NotificationTypeEnum;
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
