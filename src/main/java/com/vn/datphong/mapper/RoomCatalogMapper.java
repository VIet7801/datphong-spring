package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.RoomTypeCreateRequest;
import com.vn.datphong.dto.request.RoomUnitCreateRequest;
import com.vn.datphong.dto.response.RoomTypeResponse;
import com.vn.datphong.dto.response.RoomUnitResponse;
import com.vn.datphong.entity.CancellationPolicy;
import com.vn.datphong.entity.Property;
import com.vn.datphong.entity.RoomTypeEntity;
import com.vn.datphong.entity.RoomUnit;
import com.vn.datphong.enums.RoomUnitStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoomCatalogMapper {

    public static RoomTypeEntity toRoomTypeEntity(RoomTypeCreateRequest req, Property property, CancellationPolicy policy) {
        return RoomTypeEntity.builder()
                .property(property)
                .code(req.getCode())
                .name(req.getName())
                .maxGuests(req.getMaxGuests())
                .maxAdults(req.getMaxAdults())
                .maxChildren(req.getMaxChildren())
                .bedType(req.getBedType())
                .description(req.getDescription())
                .cancellationPolicy(policy)
                .active(true)
                .build();
    }

    public static RoomTypeResponse toRoomTypeResponse(RoomTypeEntity e) {
        if (e == null) return null;
        return RoomTypeResponse.builder()
                .id(e.getId())
                .propertyId(e.getProperty() != null ? e.getProperty().getId() : null)
                .propertyName(e.getProperty() != null ? e.getProperty().getName() : null)
                .code(e.getCode())
                .name(e.getName())
                .maxGuests(e.getMaxGuests())
                .maxAdults(e.getMaxAdults())
                .maxChildren(e.getMaxChildren())
                .bedType(e.getBedType())
                .description(e.getDescription())
                .cancellationPolicyId(e.getCancellationPolicy() != null ? e.getCancellationPolicy().getId() : null)
                .cancellationPolicyName(e.getCancellationPolicy() != null ? e.getCancellationPolicy().getName() : null)
                .active(e.getActive())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static RoomUnit toRoomUnitEntity(RoomUnitCreateRequest req, Property property, RoomTypeEntity roomType) {
        return RoomUnit.builder()
                .property(property)
                .roomType(roomType)
                .code(req.getCode())
                .status(req.getStatus() != null ? req.getStatus() : RoomUnitStatus.ACTIVE)
                .floor(req.getFloor())
                .notes(req.getNotes())
                .build();
    }

    public static RoomUnitResponse toRoomUnitResponse(RoomUnit e) {
        if (e == null) return null;
        return RoomUnitResponse.builder()
                .id(e.getId())
                .propertyId(e.getProperty() != null ? e.getProperty().getId() : null)
                .propertyName(e.getProperty() != null ? e.getProperty().getName() : null)
                .roomTypeId(e.getRoomType() != null ? e.getRoomType().getId() : null)
                .roomTypeName(e.getRoomType() != null ? e.getRoomType().getName() : null)
                .code(e.getCode())
                .status(e.getStatus())
                .floor(e.getFloor())
                .notes(e.getNotes())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
