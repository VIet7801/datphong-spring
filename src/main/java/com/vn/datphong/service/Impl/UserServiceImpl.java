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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// Mặc định các thao tác quản lý user chỉ dành cho ADMIN.
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserServiceImpl implements UserService {
    @Lazy
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<UsersResponse> findAll() {
        // Chỉ đến được thân hàm sau khi proxy kiểm tra @PreAuthorize ở cấp class thành công.
        // 1. Repository truy vấn database và trả về danh sách entity User.
        return userRepository.findAll()
                // 2. Tạo Stream để xử lý lần lượt từng User trong danh sách.
                .stream()
                // 3. Chuyển mỗi User thành UsersResponse bằng mapper.
                // UserMapper::toResponse == user -> UserMapper.toResponse(user).
                // DTO chỉ chứa các trường được phép trả về, không có password và twoFactorSecret.
                .map(UserMapper::toResponse)
                // 4. Thu các DTO thành List<UsersResponse> và trả cho controller.
                // Controller bọc danh sách trong ApiResponse.success(...) để trả JSON cho client.
                .toList();
    }

    @Override
    public UsersResponse create(UsersResquest req) {
        User user = UserMapper.toEntity(req);
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Override
    // ADMIN được xem mọi hồ sơ; người dùng khác chỉ được xem chính mình.
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @userAuthorization.isOwner(#id, authentication)")
    public UsersResponse getById(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundExcept("Id "+id+"khong ton tai"));
        return UserMapper.toResponse(u);
    }

    @Override
    // Kiểm tra quyền trước khi đọc hoặc cập nhật dữ liệu của user theo ID.
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @userAuthorization.isOwner(#id, authentication)")
    @Transactional(propagation = Propagation.REQUIRED)
    public UsersResponse update(Long id,UsersResquest req) {
        // Quyền đã được kiểm tra trước khi vào hàm. Hibernate theo dõi entity trong transaction;
        // các field thay đổi được ghi xuống DB khi transaction commit, không phải ngay lúc set(...).
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
