package com.vn.datphong.service;

import com.vn.datphong.dto.request.UsersResquest;
import com.vn.datphong.dto.response.UsersResponse;
import com.vn.datphong.entity.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    UsersResponse create(UsersResquest req);
    UsersResponse getById (Long id);
    UsersResponse update (Long id,UsersResquest req);
    void delete (Long id);

}
