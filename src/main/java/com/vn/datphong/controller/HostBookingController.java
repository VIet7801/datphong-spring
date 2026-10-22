package com.vn.datphong.controller;

import com.vn.datphong.dto.request.HostBookingNoteRequest;
import com.vn.datphong.dto.response.HostBookingResponse;
import com.vn.datphong.dto.response.PageResponse;
import com.vn.datphong.service.HostBookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/host/bookings")
public class HostBookingController {
    private final HostBookingService service;

    public HostBookingController(HostBookingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<HostBookingResponse> list(@RequestParam(required = false) Long propertyId, @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(propertyId, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    public HostBookingResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PatchMapping("/{id}/note")
    public HostBookingResponse note(@PathVariable Long id, @Valid @RequestBody HostBookingNoteRequest req) {
        return service.updateNote(id, req);
    }

    @PostMapping("/{id}/check-in")
    public HostBookingResponse checkIn(@PathVariable Long id) {
        return service.checkIn(id);
    }

    @PostMapping("/{id}/check-out")
    public HostBookingResponse checkOut(@PathVariable Long id) {
        return service.checkOut(id);
    }
}
