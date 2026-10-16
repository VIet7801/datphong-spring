package com.vn.datphong.entity;

import com.vn.datphong.enums.RoomUnitStatus;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "room_units",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "code"})
)
public class RoomUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RoomUnitStatus status = RoomUnitStatus.ACTIVE;

    @Column(length = 50)
    private String floor;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    private Long version;
}
