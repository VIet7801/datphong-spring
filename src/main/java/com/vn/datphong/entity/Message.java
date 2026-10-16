package com.vn.datphong.entity;

import com.vn.datphong.enums.MessagePurpose;
import com.vn.datphong.enums.MessageStatus;
import com.vn.datphong.enums.MessageTypeEnum;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_message_0", columnNames = {"conversation_id", "seq"}),
    @UniqueConstraint(name = "uq_message_1", columnNames = {"conversation_id", "sender_id", "client_message_id"})
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    @Builder.Default
    private MessageTypeEnum msgType = MessageTypeEnum.TEXT;

    @Column(name = "body", nullable = true, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "client_message_id", nullable = false)
    private String clientMessageId;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    @Builder.Default
    private MessagePurpose purpose = MessagePurpose.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "media_asset_id", nullable = true)
    private MediaAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "redacted_at", nullable = true)
    private Instant redactedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "redacted_by_id", nullable = true)
    private User redactedBy;

    @Column(name = "redaction_reason", nullable = true)
    private String redactionReason;
}
