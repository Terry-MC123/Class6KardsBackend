package top.terry_mc.c6be.service;

import org.springframework.stereotype.Service;
import top.terry_mc.c6be.dto.*;
import top.terry_mc.c6be.model.*;

import java.util.ArrayList;
import java.util.List;

// TODO: 逻辑中加入所有的events（events包内的，不是广播事件）
@Service
public class GameLogicService {
    private final RoomService roomService;

    public GameLogicService(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * 处理玩家加入
     * @return 事件列表
     */
    public List<Object> handleJoin(String roomId, String newPlayerId, Object data) {
        List<Object> events = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if(!(data instanceof String newPlayerName)) return events;
        Player newPlayer = new Player(newPlayerId, newPlayerName);
        if (room == null) {
            room = roomService.createRoom(roomId, newPlayer);
        } else {
            roomService.addPlayer(roomId, newPlayer);
            // TODO: 若返回false返回错误事件
        }
        events.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        events.add(new JoinBroadcast(newPlayer.toAccess()));
        if(room.getPlayers().size() == 2) {
            events.add(new StartBroadcast(room.getClasses()));
            gameStart(room);
        }
        return events;
    }

    private void gameStart(GameRoom room) {
        List<Player> list = room.getPlayers().values().stream().toList();
        Player first = list.getFirst();
        Player second = list.getLast();
        // TODO: 开始游戏（生成牌、给血量啥的）
        newTurn(room, first);
        room.setStatus(GameStatus.PLAYING);
    }

    /**
     * 处理使用卡牌（USE）
     * @return 事件列表
     */
    public List<Object> handleUse(String roomId, String playerId, Object data) {
        List<Object> events = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) return events;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayerId().equals(player.getPlayerId())) return events;
        if(!(data instanceof ActionBroadcast.ActionData actionData)) return events;
        // TODO: 校验 & 执行卡牌效果
        // 播动画
        events.add(new ActionBroadcast(player.toAccess(), "USE", actionData));
        if (actionData.publicCardId() != null) events.add(new PublicCardsUpdate(room.getPublicCardAccesses()));
        events.add(new PrivateHandUpdate(player.getHandCardAccesses()));
        return events;
    }

    /**
     * 处理移动卡牌（MOVE）
     * @return 事件列表
     */
    public List<Object> handleMove(String roomId, String playerId, Object data) {
        List<Object> events = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) return events;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayerId().equals(player.getPlayerId())) return events;
        if(!(data instanceof ActionBroadcast.ActionData actionData)) return events;
        // TODO: 校验 & 执行移动逻辑
        // 播动画
        events.add(new ActionBroadcast(player.toAccess(), "MOVE", actionData));
        //一定产生update（即使不动也要update）
        events.add(new PublicCardsUpdate(room.getPublicCardAccesses()));
        return events;
    }

    /**
     * 处理回合结束
     * @return 事件列表
     */
    public List<Object> handleFinish(String roomId, String playerId, Object data) {
        List<Object> events = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null) return events;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayerId().equals(player.getPlayerId())) return events;
        if (data!=null) return events;
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        list.remove(player);
        events.add(new FinishTurnBroadcast(list.getFirst().toAccess()));
        newTurn(room, list.getFirst());
        // TODO: if(新的回合) events.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        // TODO: if(结束) events.add(new EndBroadcast(...));
        return events;
    }

    public void newTurn(GameRoom room, Player player) {
        // TODO: 开新的turn
    }
}
