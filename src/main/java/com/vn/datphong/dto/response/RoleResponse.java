package com.vn.datphong.dto.response;

import lombok.*;
import org.hibernate.annotations.BatchSize;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;

}
