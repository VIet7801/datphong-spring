package com.vn.datphong.repository.Impl;

import com.vn.datphong.entity.Role;
import com.vn.datphong.repository.RoleCustomInterface;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RoleCustomImpl implements RoleCustomInterface {
    private EntityManager entityManager;
    @Override
    public List<Role> findAll() {
        return entityManager.createNativeQuery("select * from Role ",Role.class).getResultList();
    }
}
