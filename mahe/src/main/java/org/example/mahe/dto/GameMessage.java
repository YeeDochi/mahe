package org.example.mahe.dto;

import lombok.*;

import java.util.Map;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type;
    private String roomId;
    private String sender;
    private String senderId;
    private String content;
    private Map<String, Object> data;
}
