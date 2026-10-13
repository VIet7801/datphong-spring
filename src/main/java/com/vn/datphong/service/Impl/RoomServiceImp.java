package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.mapper.RoomMapper;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomServiceImp implements RoomService {
    @Autowired
    private  RoomRepository roomRepository;
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public RoomResponse create(RoomCreateRequest req) {
        if ( roomRepository.existsByCode(req.getCode())){
            throw new RuntimeException("Room da ton tai: " + req.getCode());
        }
        Room entity = RoomMapper.toEntity(req); // Request -> Entity
        Room saved = roomRepository.save(entity); // Lưu Entity vào DB
        return RoomMapper.toResponse(saved);// Entity -> Response
    }
    @Override
    public RoomResponse getById(Long id) {
        Room r = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ID khong ton tai"));
        return RoomMapper.toResponse(r);
    }

    @Override
    public RoomResponse update(Long id,RoomUpdateRequest req) {
        Room r = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
        if (req.getName() != null) r.setName(req.getName());
        if (req.getType() != null) r.setType(req.getType());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        if (req.getCapacity() != null) r.setCapacity(req.getCapacity());
        if (req.getPricePerNight() != null) r.setPricePerNight(req.getPricePerNight());
        if (req.getActive() != null) r.setActive(req.getActive());
        Room merged = roomRepository.save(r);
        return RoomMapper.toResponse(merged);
    }

    @Override
    public void delete(Long id) {
        Room room=roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
        room.setStatus(RoomStatus.OUT_OF_SERVICE);
        roomRepository.save(room);
    }

}
