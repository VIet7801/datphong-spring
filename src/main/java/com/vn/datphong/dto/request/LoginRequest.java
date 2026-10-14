package com.vn.datphong.dto.request;

import jakarta.validation.constraints.NotBlank;
<<<<<<< HEAD
import lombok.*;
=======
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;
}
