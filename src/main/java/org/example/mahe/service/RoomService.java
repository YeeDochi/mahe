package org.example.mahe.service;

import org.example.mahe.dto.BaseGameRoom;
import org.example.mahe.dto.MaheRoom;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final Map<String, MaheRoom> rooms = new ConcurrentHashMap<>();

    public MaheRoom createRoom(String name) {
        MaheRoom room = new MaheRoom(name);
        rooms.put(room.getRoomId(), room);
        return room;
    }

    public BaseGameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<BaseGameRoom> findAll() {
        return new ArrayList<>(rooms.values());
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }
}
