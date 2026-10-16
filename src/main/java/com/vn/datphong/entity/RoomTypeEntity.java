package com.vn.datphong.entity;

import java.time.*;
import java.util.*;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "room_types",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "code"})
)
public class RoomTypeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @Column(name = "max_adults", nullable = true)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = true)
    private Integer maxChildren;

    @Column(name = "bed_type", nullable = false)
    private String bedType;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_policy_id", nullable = true)
    private CancellationPolicy cancellationPolicy;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_type_amenities", joinColumns = @JoinColumn(name = "room_type_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();
}
