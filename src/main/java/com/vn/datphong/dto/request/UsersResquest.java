package com.vn.datphong.dto.request;

import com.vn.datphong.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersResquest {
    private Long id;

    private String fullName;

    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email không đúng định dạng"
    )
    private String email;


    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
    )
    private String phone;

    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "Username chỉ được chứa chữ cái, số, dấu chấm và dấu gạch dưới"
    )
    private String username;

    private String password;

    private Boolean gender;

    private String avatar;

    private UserStatus status;

    private Boolean active;

    private Set<@NotNull Long> roleId;

}
