package com.vn.datphong.dto.response;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant readAt;
    private Instant createdAt;
}
