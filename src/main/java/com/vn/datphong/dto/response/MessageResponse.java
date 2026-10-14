package com.vn.datphong.dto.response;

import com.vivu.booking.entity.User;
import com.vivu.booking.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private User sender;
    private MessageTypeEnum msgType;
    private String content;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
