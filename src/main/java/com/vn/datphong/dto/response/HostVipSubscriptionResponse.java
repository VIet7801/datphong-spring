package com.vn.datphong.dto.response;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.VipSubStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostVipSubscriptionResponse {
    private Long id;
    private User host;
    private HostVipPackageResponse vipPackage;
    private LocalDate startDate;
    private LocalDate endDate;
    private VipSubStatusType status;
    private String paymentRef;
}
