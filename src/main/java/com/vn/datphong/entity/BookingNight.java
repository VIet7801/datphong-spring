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
        name = "booking_nights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "stay_date"})
)
public class BookingNight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "nightly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlyRate;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "price_version")
    private Long priceVersion;
}
