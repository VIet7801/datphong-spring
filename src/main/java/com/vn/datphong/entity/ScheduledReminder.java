package com.vn.datphong.entity;

import com.vn.datphong.enums.ReminderStatus;
import com.vn.datphong.enums.ReminderType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "scheduled_reminders", uniqueConstraints = {
    @UniqueConstraint(name = "uq_scheduled_reminders_0", columnNames = {"booking_id", "reminder_type", "window_code", "policy_version"}),
    @UniqueConstraint(name = "uq_scheduled_reminders_1", columnNames = {"mail_event_id"})
})
public class ScheduledReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "checkin_request_id", nullable = true)
    private CheckinRequest checkinRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    @Builder.Default
    private ReminderType reminderType = ReminderType.CHECKIN_REMINDER;

    @Column(name = "window_code", nullable = false)
    private String windowCode;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> policySnapshot;

    @Column(name = "scheduled_checkin_at", nullable = false)
    private Instant scheduledCheckinAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "mail_event_id", nullable = true)
    private MailEvent mailEvent;

    @Column(name = "lease_owner", nullable = true)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = true)
    private Instant leaseUntil;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @Column(name = "cancel_reason", nullable = true)
    private String cancelReason;

    @Version
    private Long version;

}
