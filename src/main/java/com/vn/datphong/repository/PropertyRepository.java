package com.vn.datphong.repository;

import com.vn.datphong.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    boolean existsBySlug(String slug);

    @Query("select p from Property p where p.owner.id = :ownerId order by p.id desc")
    List<Property> findByOwner(@Param("ownerId") Long ownerId);
}
