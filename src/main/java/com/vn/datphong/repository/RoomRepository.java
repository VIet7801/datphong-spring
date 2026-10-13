package com.vn.datphong.repository;

import com.vn.datphong.entity.Room;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface RoomRepository extends JpaRepository<Room,Long> {
    boolean existsByCode(String code);
    Optional<Room> findByCode(String code);
    List<Room> findAll();


}
