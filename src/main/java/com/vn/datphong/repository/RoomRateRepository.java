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

    // Gồm cả đêm đóng bán: dùng để phân biệt "chưa cấu hình giá" với "đóng bán".
    @Query("""
            SELECT r FROM RoomRate r
            WHERE r.roomType.id = :roomTypeId AND r.stayDate >= :from AND r.stayDate < :to
            ORDER BY r.stayDate
            """)
    List<RoomRate> findAllInRange(@Param("roomTypeId") Long roomTypeId, @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(r) FROM RoomRate r
            WHERE r.roomType.id = :roomTypeId AND r.stayDate >= :from AND r.closedToSale = false
            """)
    long countOpenFrom(@Param("roomTypeId") Long roomTypeId, @Param("from") LocalDate from);
}