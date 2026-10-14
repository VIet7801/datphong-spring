package com.vn.datphong.entity;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "rooms")
public class Room extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomTypeEntity roomTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_unit_id")
    private RoomUnit roomUnit;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "price_per_night", nullable = false)
    private Long pricePerNight;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;
}
