package com.vn.datphong.repository;

import com.vn.datphong.entity.RoomRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface RoomRateRepository extends JpaRepository<RoomRate, Long> {
    @Query("""
    SELECT r
    FROM RoomRate r
    WHERE r.roomType.id = :roomTypeId
      AND r.stayDate >= :checkIn
      AND r.stayDate < :checkOut
      AND r.closedToSale = false
""")
    List<RoomRate> findRatesByRoomAndDateRange(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
