package com.vn.datphong.dto.request;

import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequest {

    private String code;

    private String name;

    private RoomType type;

    private RoomStatus status;

    private Integer capacity;

    private Long pricePerNight;

    private String description;

}
