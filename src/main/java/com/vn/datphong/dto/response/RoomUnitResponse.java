package com.vn.datphong.dto.response;

import com.vn.datphong.enums.RoomUnitStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUnitResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private Long roomTypeId;
    private String roomTypeName;
    private String code;
    private RoomUnitStatus status;
    private String floor;
    private String notes;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
