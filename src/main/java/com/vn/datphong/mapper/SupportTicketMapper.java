package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.SupportTicketCreateRequest;
import com.vn.datphong.dto.response.SupportTicketResponse;
import com.vn.datphong.entity.SupportTicket;
import com.vn.datphong.enums.TicketStatusType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SupportTicketMapper {

    public static SupportTicket toEntity(SupportTicketCreateRequest req) {
        return SupportTicket.builder()
                .ticketType(req.getTicketType())
                .description(req.getDescription())
                .status(TicketStatusType.OPEN)
                .build();
    }

    public static SupportTicketResponse toResponse(SupportTicket e) {
        return SupportTicketResponse.builder()
                .id(e.getId())
                .booking(e.getBooking())
                .createdBy(e.getCreatedBy())
                .againstUser(e.getAgainstUser())
                .ticketType(e.getTicketType())
                .description(e.getDescription())
                .status(e.getStatus())
                .resolutionNote(e.getResolutionNote())
                .resolvedBy(e.getResolvedBy())
                .createdAt(e.getCreatedAt())
                .resolvedAt(e.getResolvedAt())
                .build();
    }
}
