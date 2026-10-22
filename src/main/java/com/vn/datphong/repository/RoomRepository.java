package com.vn.datphong.repository;

import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.entity.RoomImage;
import com.vn.datphong.enums.MediaTypeEnum;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    boolean existsByCode(String code);

    /** Chủ sở hữu (owner_id) của property chứa phòng, dùng cho kiểm tra owner scope ở mọi thao tác ghi. */
    @Query("select r.property.owner.id from Room r where r.id = :roomId")
    Long findOwnerIdByRoomId(@Param("roomId") Long roomId);

    boolean existsByPropertyIdAndCodeIgnoreCase(Long propertyId, String code);

    java.util.List<Room> findByPropertyIdOrderByIdAsc(Long propertyId);

    /**
     * Khóa hàng phòng (PESSIMISTIC_WRITE) để tuần tự hóa mọi booking cùng một phòng vật lý. Timeout ngắn để
     * không treo request; hết thời gian chờ ném PessimisticLockingFailureException -> 409 RESOURCE_BUSY.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select r from Room r where r.id = :id")
    java.util.Optional<Room> findByIdForUpdate(@Param("id") Long id);
    Optional<Room> findByCode(String code);
    List<Room> findAll();


    @Query("""
        SELECT r
        FROM Room r
        WHERE (:type IS NULL OR r.type = :type)
          AND (:status IS NULL OR r.status = :status)
          AND (
              :keyword IS NULL
              OR TRIM(:keyword) = ''
              OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        """)
    Page<RoomResponse> search(
            @Param("type") RoomType type,
            @Param("status") RoomStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT r FROM Room r WHERE " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:keyword IS NULL OR r.name LIKE %:keyword% OR r.code LIKE %:keyword% OR r.description LIKE %:keyword%) AND " +
            "(:minPrice IS NULL OR r.pricePerNight >= :minPrice) AND " +
            "(:maxPrice IS NULL OR r.pricePerNight <= :maxPrice) AND " +
            "(:minCapacity IS NULL OR r.capacity >= :minCapacity) AND " +
            "(:checkIn IS NULL OR :checkOut IS NULL OR r.id NOT IN (" +
            "   SELECT b.room.id FROM Booking b " +
            "   WHERE b.status IN ('CONFIRMED', 'CHECKED_IN', 'HOLD') " +
            "   AND b.checkinDate < :checkOut AND b.checkoutDate > :checkIn" +
            "))")
    Page<Room> searchRoomsWithAvailability(
            @Param("type") RoomType type,
            @Param("status") RoomStatus status,
            @Param("keyword") String keyword,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minCapacity") Integer minCapacity,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            Pageable pageable
    );
}



