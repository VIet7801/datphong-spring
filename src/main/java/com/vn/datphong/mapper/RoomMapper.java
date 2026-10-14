package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.response.RoomMediaItem;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoomMapper {

    public static Room toEntity(RoomCreateRequest req) {
        return Room.builder()
                .code(req.getCode())
                .name(req.getName())
                .type(req.getType())
                .status(req.getStatus() != null ? req.getStatus() : RoomStatus.AVAILABLE)
                .capacity(req.getCapacity())
                .pricePerNight(req.getPricePerNight())
                .basePrice(req.getPricePerNight() != null ? java.math.BigDecimal.valueOf(req.getPricePerNight()) : null)
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .active(true)
                .build();
    }

    public static RoomResponse toResponse(Room e) {
        return toResponse(e, null);
    }

    /** media = toàn bộ ảnh + video của phòng; rỗng -> images/videos/media = null (FE dùng ảnh demo). */
    public static RoomResponse toResponse(Room e, List<RoomMediaItem> media) {
        List<RoomMediaItem> safe = media == null || media.isEmpty() ? null : media;
        List<String> images = safe == null ? null : safe.stream().filter(m -> "IMAGE".equals(m.getMediaType())).map(RoomMediaItem::getUrl).toList();
        List<String> videos = safe == null ? null : safe.stream().filter(m -> "VIDEO".equals(m.getMediaType())).map(RoomMediaItem::getUrl).toList();
        return RoomResponse.builder()
                .id(e.getId())
                .propertyId(e.getProperty() != null ? e.getProperty().getId() : null)
                .propertyName(e.getProperty() != null ? e.getProperty().getName() : null)
                .roomTypeId(e.getRoomTypeEntity() != null ? e.getRoomTypeEntity().getId() : null)
                .roomTypeName(e.getRoomTypeEntity() != null ? e.getRoomTypeEntity().getName() : null)
                .roomUnitId(e.getRoomUnit() != null ? e.getRoomUnit().getId() : null)
                .roomUnitCode(e.getRoomUnit() != null ? e.getRoomUnit().getCode() : null)
                .code(e.getCode()).name(e.getName())
                .type(e.getType()).status(e.getStatus())
                .capacity(e.getCapacity()).pricePerNight(e.getPricePerNight())
                .basePrice(e.getBasePrice())
                .description(e.getDescription()).imageUrl(e.getImageUrl())
                .images(images == null || images.isEmpty() ? null : images)
                .videos(videos == null || videos.isEmpty() ? null : videos)
                .media(safe)
                .active(e.getActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}
