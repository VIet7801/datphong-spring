package com.vn.datphong.service;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import jakarta.servlet.http.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface RoomService {
    Page<RoomResponse> list(RoomType type, RoomStatus status, String keyword, Pageable pageable);
    Page<RoomResponse> list(RoomType type, RoomStatus status, String keyword,
                            Long minPrice, Long maxPrice, Integer minCapacity,
                            LocalDate checkIn, LocalDate checkOut,
                            int page, int size, String sortBy, String sortDir);
    RoomResponse create (RoomCreateRequest req);
    RoomResponse getById(Long id);
    RoomResponse update(Long id,RoomUpdateRequest req);
    void delete(Long id);
    String uploadMedia(Long roomId, Part file);
    void deleteMedia(Long roomId, Long mediaId);
}
