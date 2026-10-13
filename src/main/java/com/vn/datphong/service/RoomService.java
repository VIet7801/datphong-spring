package com.vn.datphong.service;

import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;

public interface RoomService {
    RoomResponse create (RoomCreateRequest req);
    RoomResponse getById(Long id);
    RoomResponse update(Long id,RoomUpdateRequest req);
    void delete(Long id);
}
