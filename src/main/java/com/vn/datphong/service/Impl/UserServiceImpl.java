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
import com.vn.datphong.service.AuditLogWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import java.util.List;

@Service
// Mặc định các thao tác quản lý user chỉ dành cho ADMIN.
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserServiceImpl implements UserService {
    @Lazy
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogWriter auditLogWriter;
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

    // Replace in: src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java
    @Override
    //kiểm tra ADMIN hoặc chủ hồ sơ.
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @userAuthorization.isOwner(#id, authentication)")
    @Transactional(propagation = Propagation.REQUIRED)
    public UsersResponse update(Long id, UsersResquest req) {
        //findById tải User cần sửa vào transaction
        User updated = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundExcept("Id: " + id + " không tồn tại")
                );
        //be4 + after state khởi tạo đang rỗng
        Map<String, Object> beforeState = new LinkedHashMap<>();
        Map<String, Object> afterState = new LinkedHashMap<>();
        //Nếu khác nhau, ghi giá trị trước/sau rồi gọi setter cập nhật Entity.
        //Nếu có thay đổi, sinh operationId và gọi Writer
        setAndTrack(
                "fullName", updated.getFullName(), req.getFullName(),
                updated::setFullName, beforeState, afterState
        );
        setAndTrack(
                "email", updated.getEmail(), req.getEmail(),
                updated::setEmail, beforeState, afterState
        );
        setAndTrack(
                "phone", updated.getPhone(), req.getPhone(),
                updated::setPhone, beforeState, afterState
        );
        setAndTrack(
                "gender", updated.getGender(), req.getGender(),
                updated::setGender, beforeState, afterState
        );
        setAndTrack(
                "avatar", updated.getAvatar(), req.getAvatar(),
                updated::setAvatar, beforeState, afterState
        );

        if (!beforeState.isEmpty()) {
            String operationId = UUID.randomUUID().toString();
            auditLogWriter.userUpdated(
                    updated.getId(), beforeState, afterState, operationId
            );
        }

        return UserMapper.toResponse(updated);
    }
    //so sánh giá trị cũ với dữ liệu mới.
    private <T> void setAndTrack(
            String field,
            T oldValue,
            T newValue,
            Consumer<T> setter,
            Map<String, Object> beforeState,
            Map<String, Object> afterState
    ) {
        //
        if (newValue == null || Objects.equals(oldValue, newValue)) {
            return;
        }

        beforeState.put(field, auditValue(field, oldValue));
        afterState.put(field, auditValue(field, newValue));
        setter.accept(newValue);
    }

    private Object auditValue(String field, Object value) {
        if (value == null) {
            return null;
        }

        return switch (field) {
            case "email", "phone", "avatar" -> "[REDACTED]";
            default -> value;
        };
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
