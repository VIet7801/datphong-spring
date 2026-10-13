package com.vn.datphong.repository;

import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import com.vn.datphong.repository.Impl.RoomCustomImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RoomCustomRepository {
    List<Room> advancedSearch(RoomType type, RoomStatus status, String keyword,
                      Long minPrice, Long maxPrice, Integer minCapacity,
                      int page, int size, String sortBy, String sortDir);

    ResponseEntity<Room> advancedSearch(RoomCustomImpl req);
}
