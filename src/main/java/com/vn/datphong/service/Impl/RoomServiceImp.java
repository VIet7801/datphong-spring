package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomDetailQuoteResponse;
import com.vn.datphong.dto.response.RoomMediaItem;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.entity.RoomImage;
import com.vn.datphong.entity.RoomRate;
import com.vn.datphong.enums.MediaTypeEnum;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.mapper.RoomMapper;
import com.vn.datphong.mapper.RoomRateMapper;
import com.vn.datphong.repository.RoomImageRepository;
import com.vn.datphong.repository.RoomRateRepository;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.service.MinioService;
import com.vn.datphong.service.RoomService;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomServiceImp implements RoomService {
    private final RoomRepository roomRepository;
    private final MinioService minioService;
    private final RoomImageRepository roomImageRepository;
    private final RoomRateRepository roomRateRepository;

    @Override
    public Page<RoomResponse> list(RoomType type, RoomStatus status, String keyword, Pageable pageable) {
        return roomRepository.search(type, status, keyword, pageable);
    }

    @Override
    public Page<RoomResponse> list(RoomType type, RoomStatus status, String keyword, Long minPrice, Long maxPrice, Integer minCapacity, LocalDate checkIn, LocalDate checkOut, int page, int size, String sortBy, String sortDir) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException(400, "Ngày nhận phòng và trả phòng không được để trống.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "Ngày nhận phòng không được là ngày trong quá khứ.");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new BusinessException(400, "Ngày nhận phòng phải trước ngày trả phòng.");
        }

        String col = switch (sortBy == null ? "" : sortBy) {
            case "pricePerNight", "price" -> "pricePerNight";
            case "name" -> "name";
            case "createdAt" -> "createdAt";
            default -> "id"; // Mặc định sắp xếp theo ID
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, col));
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim() + "%";
        Page<Room> roomPage = roomRepository.searchRoomsWithAvailability(
                type,
                status,
                searchKeyword,
                minPrice,
                maxPrice,
                minCapacity,
                checkIn,
                checkOut,
                pageable
        );
        return roomPage.map(room -> {
            List<RoomMediaItem> mediaItems = loadMedia(room.getId());
            return RoomMapper.toResponse(room, mediaItems.isEmpty() ? null : mediaItems);
        });
    }


    @Override
    public RoomResponse create(RoomCreateRequest req) {
        if (roomRepository.existsByCode(req.getCode())) {
            throw new ResourceNotFoundExcept("Room code already exists: " + req.getCode());
        }
        Room room = RoomMapper.toEntity(req);
        roomRepository.save(room);
        return RoomMapper.toResponse(room);
    }

    @Override
    public RoomResponse getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("Không tìm thấy: " + id));
        List<RoomMediaItem> media = loadMedia(id);
        return RoomMapper.toResponse(room);
    }

    @Override
    public RoomResponse update(Long id, RoomUpdateRequest req) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("Không tìm thấy: " + id));
        if (req.getName() != null) req.setName(req.getName());
        if (req.getType() != null) req.setType(req.getType());
        if (req.getStatus() != null) req.setStatus(req.getStatus());
        if (req.getCapacity() != null) req.setCapacity(req.getCapacity());
        if (req.getPricePerNight() != null) req.setPricePerNight(req.getPricePerNight());
        if (req.getDescription() != null) req.setDescription(req.getDescription());
        if (req.getImageUrl() != null) req.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) req.setActive(req.getActive());
        Room merged = roomRepository.save(room);
        List<RoomMediaItem> media = loadMedia(id);
        return RoomMapper.toResponse(merged, media.isEmpty() ? null : media);
    }

    @Override
    public void delete(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("không tìm thấy: " + id));
        room.setActive(false);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public String uploadMedia(Long id, Part file) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("Không tìm thấy phòng với ID: " + id));
        try {
            String bucketName = minioService.getBucket();
            minioService.createBucket(bucketName);
            String originalFilename = file.getSubmittedFileName();
            String contentType = file.getContentType();

            // Phân loại thư mục và kiểm tra định dạng hợp lệ theo đặc tả MinIO
            String folder;
            MediaTypeEnum mediaType;
            if (contentType != null && contentType.startsWith("video/")) {
                folder = "rooms/" + id + "/videos";
                mediaType = MediaTypeEnum.VIDEO;
            } else if (contentType != null && contentType.startsWith("image/")) {
                folder = "rooms/" + id + "/images";
                mediaType = MediaTypeEnum.IMAGE;
            } else {
                throw new BusinessException(400, "Chỉ hỗ trợ tải lên định dạng hình ảnh hoặc video.");
            }

            // Đã sửa lại lỗi cú pháp: Dùng dấu nhân (*) thay cho dấu trừ và thêm toán tử thiếu
            long maxSize = mediaType == MediaTypeEnum.VIDEO ? 50L * 1024 * 1024 : 10L * 1024 * 1024;

            if (file.getSize() > maxSize) {
                throw new BusinessException(400, "Dung lượng tệp vượt quá giới hạn cho phép.");
            }

            String objectName = folder + "/" + UUID.randomUUID() + "_" + originalFilename;

            // Thực hiện upload stream lên MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioService.upload(
                        bucketName,
                        objectName,
                        inputStream,
                        file.getSize(),
                        contentType
                );
            }
            // Bắt buộc phải có return để trả về URL
            return minioService.getObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload media lên MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMedia(Long roomId, Long mediaId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundExcept("Không tìm thấy phòng với ID: " + roomId));
        RoomImage roomImage = roomImageRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundExcept("không tìm thấy tệp media với ID: " + mediaId));
        if (!roomImage.getRoom().getId().equals(roomId)) {
            throw new ResourceNotFoundExcept("Tệp media không thuộc về phòng này.");
        }
        try {
            String bucketName = minioService.getBucket();

            // 3. Trích xuất objectName từ URL lưu trữ để xóa trên MinIO
            String objectName = extractObjectNameFromUrl(roomImage.getUrl(), bucketName);

            if (objectName != null && !objectName.isEmpty()) {
                minioService.removeObject(bucketName, objectName);
            }

            // 4. Xóa bản ghi trong cơ sở dữ liệu
            roomImageRepository.delete(roomImage);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa media từ MinIO: " + e.getMessage(), e);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public RoomDetailQuoteResponse getDetailAndQuote(Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guestsCount) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundExcept("Không tìm thấy phòng với ID: " + roomId));
        if (Boolean.FALSE.equals(room.getActive()) || room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessException(400, "Phòng hiện không hiển thị công khai hoặc đang bảo trì.");
        }
        List<RoomMediaItem> mediaItems = loadMedia(roomId);
        if (checkIn == null || checkOut == null) {
            return RoomRateMapper.toQuoteResponse(room, mediaItems, null, null, null, null);
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "Ngày nhận phòng không được là ngày trong quá khứ.");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new BusinessException(400, "Ngày nhận phòng phải trước ngày trả phòng.");
        }
        if (guestsCount != null && guestsCount > room.getCapacity()) {
            throw new BusinessException(400, "Số lượng khách vượt quá sức chứa tối đa của phòng (" + room.getCapacity() + " người).");
        }
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<RoomRate> rates = roomRateRepository.findRatesByRoomAndDateRange(roomId, checkIn, checkOut);
        if (rates.isEmpty() || rates.size() < nights) {
            throw new BusinessException( "Chưa cấu hình đầy đủ đơn giá phòng (nightly rate) cho toàn bộ khoảng thời gian lưu trú.");
        }
        BigDecimal subTotal = rates.stream()
                .map(RoomRate::getNightlyRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAndFee = subTotal.multiply(BigDecimal.valueOf(0.1));
        BigDecimal totalPrice = subTotal.add(taxAndFee).setScale(2, RoundingMode.HALF_UP);
        return RoomRateMapper.toQuoteResponse(room, mediaItems, subTotal, taxAndFee, totalPrice, nights);
    }

    private List<RoomMediaItem> loadMedia(Long roomId) {
        return toMediaItems(roomImageRepository.findByRoomIdOrdered(roomId));
    }

    /**
     * Chuyển RoomImage -> RoomMediaItem (id + url + mediaType dạng string).
     */
    private static List<RoomMediaItem> toMediaItems(List<RoomImage> entities) {
        return entities.stream().map(e -> RoomMediaItem.builder()
                .id(e.getId())
                .url(e.getUrl())
                .mediaType(e.getMediaType() == null ? "IMAGE" : e.getMediaType().name())
                .build()).toList();
    }

    private String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null) return null;
        String marker = "/" + bucketName + "/";
        int index = url.indexOf(marker);
        if (index != -1) {
            return url.substring(index + marker.length());
        }
        return null;
    }
}
