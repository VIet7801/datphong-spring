package com.vn.datphong.entity;

import java.time.*;
import java.util.*;

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
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "stay_date", "currency"})
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

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal nightlyRate;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "closed_to_sale", nullable = false)
    @Builder.Default
    private Boolean closedToSale = false;

    @Column(name = "price_version", nullable = false)
    @Builder.Default
    private Long priceVersion = 1L;

    @Version
    private Long version;
}
