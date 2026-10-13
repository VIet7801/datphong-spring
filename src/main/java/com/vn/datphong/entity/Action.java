package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "actions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_action_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Action {

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