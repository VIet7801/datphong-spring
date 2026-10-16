package com.vn.datphong.entity;

import com.vn.datphong.enums.PropertyStatus;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "properties")
public class Property extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = true, unique = true)
    private String slug;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false)
    private String addressLine;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country", nullable = true)
    @Builder.Default
    private String country = "VN";

    @Column(name = "latitude", nullable = true, precision = 19, scale = 2)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = true, precision = 19, scale = 2)
    private BigDecimal longitude;

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "Asia/Bangkok";

    @Column(name = "checkin_local", nullable = false)
    private LocalTime checkInFrom;

    @Column(name = "checkout_local", nullable = false)
    private LocalTime checkOutUntil;

    @Column(name = "public_contact", nullable = true)
    private String publicContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_policy_id", nullable = true)
    private CancellationPolicy cancellationPolicy;

    @Version
    private Long version;
}
