package com.vn.datphong.dto.request;


import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUpdateRequest {
    @Size(max = 150)
    private String name;

    private RoomType type;

    private RoomStatus status;

    @Min(1)
    @Max(20)
    private Integer capacity;

    @Min(0)
    private Long pricePerNight;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private Boolean active;
}
