package com.vn.datphong.controller;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomDetailQuoteResponse;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import com.vn.datphong.repository.Impl.RoomCustomImpl;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.service.RoomService;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vn.datphong.repository.RoomCustomRepository;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequestMapping("/api/rooms")
@RestController
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<Page<RoomResponse>> listRooms(
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<RoomResponse> response = roomService.list(
                type, status, keyword, minPrice, maxPrice, minCapacity, checkIn, checkOut, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create/rooms")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @PutMapping("/update/rooms/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomUpdateRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @PostMapping("/media/{id}")
    public ResponseEntity<String> uploadMedia(@PathVariable Long id, @Valid @RequestBody Part file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.uploadMedia(id, file));
    }

    @DeleteMapping("/media/{rooId}/{mediaId}")
    public void deleteMedia(@PathVariable Long rooId, @PathVariable Long mediaId) {
         roomService.deleteMedia(rooId, mediaId);
    }
    @GetMapping("/{id}/detail-quote")
    public ResponseEntity<RoomDetailQuoteResponse> getRoomDetailAndQuote(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "1") Integer guestsCount
    ) {
        RoomDetailQuoteResponse response = roomService.getDetailAndQuote(id, checkIn, checkOut, guestsCount);
        return ResponseEntity.ok(response);
    }


}
