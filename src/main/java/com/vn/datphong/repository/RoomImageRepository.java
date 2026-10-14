package com.vn.datphong.repository;

import com.vn.datphong.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    @Query("SELECT img FROM RoomImage img WHERE img.room.id = :roomId ORDER BY img.id DESC")
    List<RoomImage> findByRoomIdOrdered(@Param("roomId") Long roomId);
}
