package com.vn.datphong.entity;

import com.vn.datphong.entity.BaseEntity;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.HostStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "host_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "host_status", nullable = false)
    @Builder.Default
    private HostStatus hostStatus = HostStatus.PENDING;

    @Column(name = "auto_booking_default", nullable = false)
    @Builder.Default
    private Boolean autoBookingDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}