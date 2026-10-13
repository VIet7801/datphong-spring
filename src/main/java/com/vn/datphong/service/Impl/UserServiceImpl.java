package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.UsersResquest;
import com.vn.datphong.dto.response.UsersResponse;
import com.vn.datphong.entity.User;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.mapper.UserMapper;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Lazy
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public UsersResponse create(UsersResquest req) {
        if(userRepository.existsById(req.getId())){
            throw new RuntimeException("Id da ton tai ");
        }
        User user = UserMapper.toEntity(req);
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Override
    public UsersResponse getById(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("Id "+id+"khong ton tai"));
        return UserMapper.toResponse(u);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public UsersResponse update(Long id,UsersResquest req) {
        User updated = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("Id: "+id+" khong ton tai"));
        if(req.getFullName()!=null) updated.setFullName(req.getFullName());
        if(req.getEmail()!=null) updated.setEmail(req.getEmail());
        if(req.getPhone()!=null) updated.setPhone(req.getPhone());
        if(req.getGender()!=null) updated.setGender(req.getGender());
        if(req.getAvatar()!=null) updated.setAvatar(req.getAvatar());
        return UserMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
