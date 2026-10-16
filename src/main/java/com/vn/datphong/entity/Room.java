package com.vn.datphong.entity;

import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_code"}))
public class Room extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_unit_id")
    private RoomUnit roomUnit;

    @Column(name = "room_code", nullable = false)
    private String code;

    @Column(name = "name", nullable = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.ACTIVE;

    @Column
    private Integer capacity;

    @Column(name = "price_per_night")
    private Long pricePerNight;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(length = 500, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;

    @Column(name = "floor", nullable = true)
    private String floor;

    @Column(name = "notes", nullable = true, columnDefinition = "TEXT")
    private String notes;
}
