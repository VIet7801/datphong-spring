package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "room_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "stay_date"})
)
public class RoomRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "nightly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlyRate;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "closed_to_sale", nullable = false)
    @Builder.Default
    private Boolean closedToSale = false;

    @Column(name = "price_version", nullable = false)
    @Builder.Default
    private Long priceVersion = 1L;
}
