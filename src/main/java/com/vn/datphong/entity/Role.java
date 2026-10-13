package com.vn.datphong.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Role {

       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(nullable = false, unique = true, length = 50)
       private String code;

       @Column(nullable = false, length = 100)
       private String name;

       @Column(length = 255)
       private String description;

       @ManyToMany(fetch = FetchType.LAZY)
       @JoinTable(
               name = "role_permissions",
               joinColumns = @JoinColumn(name = "role_id"),
               inverseJoinColumns = @JoinColumn(name = "permission_id"),
               uniqueConstraints = {
                       @UniqueConstraint(
                               name = "uk_role_permission",
                               columnNames = {"role_id", "permission_id"}
                       )
               }
       )
       @Builder.Default
       private Set<Permission> permissions = new HashSet<>();
}