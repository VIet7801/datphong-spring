package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.RoleRequest;
import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Role;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import com.vn.datphong.repository.RoleRepository;
import com.vn.datphong.service.RoleService;
import com.vn.datphong.service.RoomService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleRepository roleRepository;


    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public Role add(RoleRequest req) {
        if (roleRepository.existsByCode(req.getCode())) {
            throw new RuntimeException("Loi khong tim thay");
        }
        Role role = new Role();
        role.setCode(req.getCode());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        return roleRepository.save(role);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Role update(Long id, RoleRequest req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay id"));
        role.setCode(req.getCode());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        return roleRepository.save(role);
    }




    @Override
    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    public Role getbyId(Long id) {
        log.info("Đang tìm Role với id = {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Khong tim thay id"));

        log.info("Tìm thấy Role = {}", role);

        return role;
    }
}
