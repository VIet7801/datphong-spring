package com.vn.datphong.controller;

import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.repository.Impl.RoomCustomImpl;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vn.datphong.repository.RoomCustomRepository;

import java.util.List;

@Slf4j
@RequestMapping("/Room")
@RestController
@RequiredArgsConstructor
public class RoomController {
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    @Autowired
    private final RoomCustomRepository roomCustomRepository;
    @GetMapping("/findAll")
    public ResponseEntity<List<Room>> findAll(){
        log.info("Find All API");
        List<Room> listroom = roomRepository.findAll();
        return ResponseEntity.ok(listroom);
    }
    @GetMapping("/advancedSearch")
    public ResponseEntity<Room> advancedSearch(@RequestBody RoomCustomImpl req){
       return  roomCustomRepository.advancedSearch(req);
    }
    @PutMapping("/update/{id}")
    public RoomResponse update(@PathVariable Long id, @RequestBody RoomUpdateRequest req){
        return  roomService.update(id,req);
    }
}
