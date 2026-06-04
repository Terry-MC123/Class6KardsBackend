package top.terry_mc.c6be.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import top.terry_mc.c6be.dto.*;
import top.terry_mc.c6be.model.ClientPayload;
import top.terry_mc.c6be.service.GameLogicService;

import java.util.List;

@Controller
public class GameWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameLogicService gameLogicService;

    // TODO: all（参照DeepSeek给的帮助）

    @MessageMapping("/join")
    public void handleJoin(@Payload ClientPayload payload) {
        List<Packet> packets = gameLogicService.handleJoin(payload.roomId(), payload.playerId(), payload.data());
        sendPackets(payload.roomId(), packets);
    }

    @MessageMapping("/use")
    public void handleUse(@Payload ClientPayload payload) {
        List<Packet> packets = gameLogicService.handleUse(payload.roomId(), payload.playerId(), payload.data());
        sendPackets(payload.roomId(), packets);
    }

    @MessageMapping("/move")
    public void handleMove(@Payload ClientPayload payload) {
        List<Packet> packets = gameLogicService.handleMove(payload.roomId(), payload.playerId(), payload.data());
        sendPackets(payload.roomId(), packets);
    }

    private void sendPackets(String roomId, List<Packet> packets) {
        String s = "/topic/room/" + roomId + "/";
        for (Packet packet : packets) {
            if (packet instanceof RoundUpdate) {
                messagingTemplate.convertAndSend(s + "state", packet);
            } else if (packet instanceof PublicCardsUpdate) {
                messagingTemplate.convertAndSend(s + "public", packet);
            } else if (packet instanceof ActionBroadcast) {
                messagingTemplate.convertAndSend(s + "action", packet);
            } else if (packet instanceof StartBroadcast) {
                messagingTemplate.convertAndSend(s + "start", packet);
            } else if (packet instanceof EndBroadcast) {
                messagingTemplate.convertAndSend(s + "end", packet);
            } else if (packet instanceof FinishTurnBroadcast) {
                messagingTemplate.convertAndSend(s + "turn", packet);
            } else if (packet instanceof JoinBroadcast) {
                messagingTemplate.convertAndSend(s + "join", packet);
            } else if (packet instanceof PrivateHandUpdate) {
                messagingTemplate.convertAndSendToUser(((PrivateHandUpdate) packet).player().playerId(), "/queue/hand", packet);
            }
        }
    }
}
