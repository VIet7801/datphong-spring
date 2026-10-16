package com.vn.datphong.controller;

import com.vn.datphong.dto.request.UsersResquest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.UsersResponse;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/findAll")
    public ApiResponse<List<UsersResponse>> findAll() {
        // Filter chain đã xác thực request trước khi MVC chọn endpoint này.
        // Lời gọi service đi qua proxy kiểm tra @PreAuthorize trước khi chạy thân hàm findAll().
        // Service trả DTO; ApiResponse chỉ bọc dữ liệu, Spring MVC mới chuyển thành JSON gửi về FE.
        return ApiResponse.success(userService.findAll());
    }

    @PostMapping("/add") /*Create*/
    public ApiResponse<UsersResponse> add(@Valid @RequestBody UsersResquest req) {
        UsersResponse user = userService.create(req);
        return ApiResponse.success(user);
    }

    @DeleteMapping("/delete/{id}")
    public void delete( @PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<UsersResponse> findById(@PathVariable Long id) {
        // ID lấy từ URL; service kiểm tra quyền ADMIN hoặc chính chủ trước khi đọc hồ sơ.
        UsersResponse userR = userService.getById(id);
        return ApiResponse.success(userR);
    }
    @PutMapping("/update/{id}")
    public ApiResponse<UsersResponse> update ( @PathVariable Long id,@Valid @RequestBody UsersResquest req){
        if(userService.getById(id) == null) throw new ResourceNotFoundExcept("Id khong ton tai");
        UsersResponse updated = userService.update(id, req);
        return ApiResponse.success(updated);
    }
}
