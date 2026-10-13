package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "resources",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resource_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}