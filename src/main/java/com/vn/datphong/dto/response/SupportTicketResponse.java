package com.vn.datphong.dto.response;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.TicketStatusType;
import com.vn.datphong.enums.TicketTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponse {
    private Long id;
    private Booking booking;
    private User createdBy;
    private User againstUser;
    private TicketTypeEnum ticketType;
    private String description;
    private TicketStatusType status;
    private String resolutionNote;
    private User resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
