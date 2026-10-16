package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ConversationParticipantId implements Serializable {
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
