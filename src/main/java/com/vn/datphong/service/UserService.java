package com.vn.datphong.service;

import com.vn.datphong.dto.request.UsersResquest;
import com.vn.datphong.dto.response.UsersResponse;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface UserService {
    List<UsersResponse> findAll();
    UsersResponse create(UsersResquest req);
    UsersResponse getById (Long id);
    UsersResponse update (Long id,UsersResquest req);
    void delete (Long id);

}
