package com.vn.datphong.entity;

import com.vn.datphong.enums.BookingSourceChannel;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.GuestSegment;

import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_unit_id")
    private RoomUnit roomUnit;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkoutDate;

    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    private Integer guestsCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatusType status = BookingStatusType.PENDING_PAYMENT;

    @Transient
    private Instant holdExpiresAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "loyalty_discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal loyaltyDiscountPercent = BigDecimal.ZERO;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "quote_token", length = 120)
    private String quoteToken;

    @Column(name = "price_version", nullable = false)
    private Long priceVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cancellation_policy_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> cancellationPolicySnapshot;

    @Column(name = "special_request", nullable = true)
    private String specialRequest;

    @Column(name = "confirmed_at", nullable = true)
    private Instant confirmedAt;

    @Column(name = "cancelled_at", nullable = true)
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by", nullable = true)
    private User cancelledBy;

    @Column(name = "cancel_reason", nullable = true)
    private String cancellationReason;

    @Column(name = "cancellation_fee", nullable = true, precision = 19, scale = 2)
    private BigDecimal cancellationFee;

    @Column(name = "refundable_amount", nullable = true, precision = 19, scale = 2)
    private BigDecimal refundableAmount;

    @Column(name = "actual_check_in", nullable = true)
    private Instant actualCheckIn;

    @Column(name = "actual_check_out", nullable = true)
    private Instant actualCheckOut;

    @Version
    private Long version;

    @Column(name = "property_timezone_snapshot", nullable = false)
    private String propertyTimezoneSnapshot;

    @Column(name = "checkin_local_snapshot", nullable = false)
    private LocalTime checkinLocalSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false)
    @Builder.Default
    private BookingSourceChannel sourceChannel = BookingSourceChannel.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_segment_snapshot", nullable = false)
    @Builder.Default
    private GuestSegment guestSegmentSnapshot = GuestSegment.UNKNOWN;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "checked_in_by_id", nullable = true)
    private User checkedInBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "checked_out_by_id", nullable = true)
    private User checkedOutBy;

    @Column(name = "checkin_verification_method", nullable = true)
    private String checkinVerificationMethod;

    /** Legacy response alias; only expires_at is persisted. */
    public Instant getHoldExpiresAt() { return expiresAt != null ? expiresAt : holdExpiresAt; }
}
