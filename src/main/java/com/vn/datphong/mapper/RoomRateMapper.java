package com.vn.datphong.mapper;

import com.vn.datphong.dto.response.RoomDetailQuoteResponse;
import com.vn.datphong.dto.response.RoomMediaItem;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;

import java.math.BigDecimal;
import java.util.List;

public class RoomRateMapper {
    public static RoomResponse toResponse(Room room, List<RoomMediaItem> mediaItems) {
        if (room == null) {
            return null;
        }

        List<String> images = null;
        List<String> videos = null;
        String coverImageUrl = room.getImageUrl(); // Lấy ảnh bìa từ entity nếu có

        if (mediaItems != null && !mediaItems.isEmpty()) {
            images = mediaItems.stream()
                    .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                    .map(RoomMediaItem::getUrl)
                    .toList();

            videos = mediaItems.stream()
                    .filter(m -> "VIDEO".equalsIgnoreCase(m.getMediaType()))
                    .map(RoomMediaItem::getUrl)
                    .toList();

            // Nếu phòng chưa có ảnh bìa riêng nhưng có danh sách ảnh, tự động lấy ảnh đầu tiên làm ảnh bìa
            if ((coverImageUrl == null || coverImageUrl.isEmpty()) && !images.isEmpty()) {
                coverImageUrl = images.get(0);
            }
        }

        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .type(room.getType())
                .status(room.getStatus())
                .capacity(room.getCapacity())
                .pricePerNight(room.getPricePerNight())
                .description(room.getDescription())
                .imageUrl(coverImageUrl)
                .images(images != null && !images.isEmpty() ? images : null)
                .videos(videos != null && !videos.isEmpty() ? videos : null)
                .media(mediaItems)
                .active(room.getActive())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    /**
     * Overload khi không truyền danh sách media
     */
    public static RoomResponse toResponse(Room room) {
        return toResponse(room, null);
    }

    /**
     * Ánh xạ chi tiết phòng và thông tin báo giá (Quote) phục vụ cho nghiệp vụ xem chi tiết phòng
     */
    public static RoomDetailQuoteResponse toQuoteResponse(
            Room room,
            List<RoomMediaItem> mediaItems,
            BigDecimal subTotal,
            BigDecimal taxAndFee,
            BigDecimal totalPrice,
            Integer numberOfNights
    ) {
        if (room == null) {
            return null;
        }

        List<String> images = null;
        List<String> videos = null;
        String coverImageUrl = room.getImageUrl();

        if (mediaItems != null && !mediaItems.isEmpty()) {
            images = mediaItems.stream()
                    .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                    .map(RoomMediaItem::getUrl)
                    .toList();

            videos = mediaItems.stream()
                    .filter(m -> "VIDEO".equalsIgnoreCase(m.getMediaType()))
                    .map(RoomMediaItem::getUrl)
                    .toList();

            if ((coverImageUrl == null || coverImageUrl.isEmpty()) && !images.isEmpty()) {
                coverImageUrl = images.get(0);
            }
        }

        return RoomDetailQuoteResponse.builder()
                .roomId(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .type(room.getType())
                .capacity(room.getCapacity())
                .description(room.getDescription())
                .imageUrl(coverImageUrl)
                .images(images != null && !images.isEmpty() ? images : null)
                .videos(videos != null && !videos.isEmpty() ? videos : null)
                .basePricePerNight(room.getPricePerNight() != null ? BigDecimal.valueOf(room.getPricePerNight()) : null)
                .subTotal(subTotal)
                .taxAndFee(taxAndFee)
                .totalPrice(totalPrice)
                .currency("VND")
                .numberOfNights(numberOfNights)
                .quoteExpiresAt(numberOfNights != null ? "Báo giá có hiệu lực trong 15 phút tới." : null)
                .cancellationPolicy("Chính sách tiêu chuẩn: Miễn phí hủy trước 2 ngày, hoàn lại 50% sau đó.")
                .build();
    }
}
