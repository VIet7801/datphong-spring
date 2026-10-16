package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class RoomTypeAmenityId implements Serializable {
    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;
    @Column(name = "amenity_id", nullable = false)
    private Long amenityId;
}
