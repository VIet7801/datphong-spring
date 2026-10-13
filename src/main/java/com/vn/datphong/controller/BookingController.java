package com.vn.datphong.controller;

import com.vn.datphong.dto.request.BookingRequest;
import com.vn.datphong.dto.response.ApiResponse;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.entity.Booking;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/Booking")
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    private final BookingRepository bookingRepository;

    @GetMapping("/findAll")
    public ApiResponse<ResponseEntity<List<Booking>>> findAll(){
        List<Booking> listbooking = bookingRepository.findAll();
        return ApiResponse.success(ResponseEntity.ok(listbooking));
    }
    @GetMapping("/details/{id}")
    public ApiResponse<BookingResponse> getById( @PathVariable Long id){
        BookingResponse b = bookingService.getById(id);
        return ApiResponse.success(b);
    }
    @PostMapping("/add")
    public  ApiResponse<BookingResponse> add(@Valid @RequestBody BookingRequest req){
        BookingResponse b = bookingService.create(req);
        return  ApiResponse.success(b);
    }
    @PutMapping("/update/{id}")
    public  ApiResponse<BookingResponse> update (@PathVariable Long id,@Valid  @RequestBody BookingRequest req){
        BookingResponse updated = bookingService.update(id,req);
        return ApiResponse.success(updated);
    }
    @DeleteMapping("/delete/{id}")
    public void delete(@Valid @PathVariable Long id ){
        bookingService.delete(id);
    }

}
