package org.example.mahe.service;

import lombok.RequiredArgsConstructor;
import org.example.mahe.dto.BaseGameRoom;
import org.example.mahe.dto.GameMessage;
import org.example.mahe.dto.Player;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public void join(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;
        if (room.isPlaying()) {
            System.out.println("❌ 입장 거부: 이미 게임 진행 중인 방 (" + roomId + ")");
            return;
        }
        room.enterUser(new Player(message.getSender(), message.getSenderId()));

        message.setType("JOIN");
        message.setContent(message.getSender() + "님이 입장하셨습니다.");
        broadcast(roomId, message);

        GameMessage syncMsg = new GameMessage();
        syncMsg.setType("SYNC");
        syncMsg.setRoomId(roomId);
        syncMsg.setSender("SYSTEM");
        syncMsg.setData(room.getGameSnapshot());
        broadcast(roomId, syncMsg);
    }

    public void handleGameAction(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        GameMessage result = room.handleAction(message);

        if (result != null) {
            broadcast(roomId, result);
        }
    }

    public void chat(String roomId, GameMessage message) {
        broadcast(roomId, message);
    }

    public void exit(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room != null) {
            room.exitUser(message.getSenderId());
            message.setType("EXIT");
            message.setContent(message.getSender() + "님이 퇴장하셨습니다.");
            
            if (room.getUsers().isEmpty()) {
                roomService.deleteRoom(roomId);
            } else {
                broadcast(roomId, message);
            }
        }
    }

    private void broadcast(String roomId, GameMessage message) {
        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }
}
