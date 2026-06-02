package top.terry_mc.c6be.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import top.terry_mc.c6be.service.GameLogicService;
import top.terry_mc.c6be.service.RoomService;

@Controller
public class GameWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameLogicService gameLogicService;

    @Autowired
    private RoomService roomService;
    // TODO: all（参照DeepSeek给的帮助）
}
