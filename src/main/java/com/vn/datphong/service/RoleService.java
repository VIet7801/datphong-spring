package com.vn.datphong.service;


import com.vn.datphong.dto.request.RoleRequest;
import com.vn.datphong.dto.response.RoleResponse;
import com.vn.datphong.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> findAll();
    Role add(RoleRequest req);
    Role update (Long id, RoleRequest req);
    void delete (Long id);
    Role getbyId (Long id);
}
