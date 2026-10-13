package com.vn.datphong.repository.Impl;

import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import com.vn.datphong.repository.RoomCustomRepository;
import com.vn.datphong.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
@Component
public class RoomCustomImpl implements RoomCustomRepository {
    @PersistenceContext
    public EntityManager entityManager;

    @Override
    public List<Room> advancedSearch(RoomType type, RoomStatus status, String keyword, Long minPrice, Long maxPrice, Integer minCapacity, int page, int size, String sortBy, String sortDir) {
        StringBuilder hql = new StringBuilder("from rooms r where r.active = true");
        if (type != null) hql.append(" and r.type = :type");
        if (status != null) hql.append(" and r.status = :status");
        if (keyword != null && !keyword.isBlank())
            hql.append(" and (lower(r.name) like :kw or lower(r.code) like :kw)");
        if (minPrice != null) hql.append(" and r.pricePerNight >= :minPrice");
        if (maxPrice != null) hql.append(" and r.pricePerNight <= :maxPrice");
        if (minCapacity != null) hql.append(" and r.capacity >= :minCapacity");
        boolean desc = !"asc".equalsIgnoreCase(sortDir);
        String col = switch (sortBy == null ? "" : sortBy) {
            case "pricePerNight" -> "r.pricePerNight";
            case "name" -> "r.name";
            case "createdAt" -> "r.createdAt";
            default -> "r.id";
        };
        hql.append(" order by ").append(col).append(desc ? " desc" : " asc");
        var q = entityManager.createQuery(hql.toString(), Room.class);
        if (type != null) q.setParameter("type", type);
        if (status != null) q.setParameter("status", status);
        if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
        if (minPrice != null) q.setParameter("minPrice", minPrice);
        if (maxPrice != null) q.setParameter("maxPrice", maxPrice);
        if (minCapacity != null) q.setParameter("minCapacity", minCapacity);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }

    @Override
    public ResponseEntity<Room> advancedSearch(RoomCustomImpl req) {
        return null;
    }
}
