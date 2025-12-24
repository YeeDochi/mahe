package org.example.mahe.controller;

import lombok.RequiredArgsConstructor;
import org.example.mahe.dto.BaseGameRoom;
import org.example.mahe.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    public List<BaseGameRoom> findAllRooms() {
        return roomService.findAll();
    }

    @PostMapping
    public BaseGameRoom createRoom(@RequestParam String name) {
        return roomService.createRoom(name);
    }

    @GetMapping("/{roomId}")
    public BaseGameRoom getRoom(@PathVariable String roomId) {
        return roomService.findRoom(roomId);
    }
}
