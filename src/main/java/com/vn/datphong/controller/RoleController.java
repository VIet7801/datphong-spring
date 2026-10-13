package com.vn.datphong.controller;

import com.vn.datphong.config.RedisConfig;
import com.vn.datphong.dto.request.RoleRequest;
import com.vn.datphong.dto.response.RoleResponse;
import com.vn.datphong.entity.Role;
import com.vn.datphong.repository.RoleRepository;
import com.vn.datphong.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/Role")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/findAll")
    public ResponseEntity<List<Role>> findAll() {
        List<Role> listrole = roleService.findAll();
        return ResponseEntity.ok(listrole);
    }

    @PostMapping("/add")
    public Role add(@Valid @RequestBody RoleRequest req) {
        return roleService.add(req);
    }

    @PutMapping("/update/{id}")
    public Role update(@PathVariable Long id,@Valid @RequestBody RoleRequest req) {return roleService.update(id, req);}

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }

    @GetMapping("/detail/{id}")
    public Role getbyId(@PathVariable Long id) {
        return roleService.getbyId(id);
    }

}
