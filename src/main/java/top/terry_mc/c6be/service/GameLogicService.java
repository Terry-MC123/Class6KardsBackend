package top.terry_mc.c6be.service;

import org.springframework.stereotype.Service;
import top.terry_mc.c6be.dto.*;
import top.terry_mc.c6be.events.EventBus;
import top.terry_mc.c6be.events.MoveCardEvent;
import top.terry_mc.c6be.events.TurnStartEvent;
import top.terry_mc.c6be.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
    public List<Packet> handleJoin(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if(!(data instanceof String playerName)) return packets;
        Player player = new Player(playerId, playerName);
        if (room == null) {
            room = roomService.createRoom(roomId, player);
        } else {
            roomService.addPlayer(roomId, player);
            // TODO: 若返回false返回错误事件
        }
        packets.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        packets.add(new JoinBroadcast(player.toAccess()));
        if(room.getPlayers().size() == 2) {
            gameStart(room, packets::add);
        }
        return packets;
    }

    private void gameStart(GameRoom room, Consumer<Packet> addPacket) {
        List<Player> list = room.getPlayers().values().stream().toList();
        Player first = list.getFirst();
        Player second = list.getLast();
        room.setClasses(generateClasses());
        // TODO: 开始游戏（生成牌、给血量啥的）
        addPacket.accept(new StartBroadcast(room.getClasses()));
        newTurn(room, first, addPacket);
        room.setStatus(GameStatus.PLAYING);
    }

    private List<Subject> generateClasses() {
        List<Subject> list = Arrays.asList(Subject.values());
        Collections.shuffle(list);
        return List.copyOf(list);
    }

    /**
     * 处理使用卡牌（USE）
     * @return 事件列表
     */
    public List<Packet> handleUse(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || !(data instanceof ActionBroadcast.ActionData actionData)) return packets;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayer().equals(player)) return packets;
        // TODO: 校验 & 执行卡牌效果
        packets.add(new ActionBroadcast(player.toAccess(), "USE", actionData)); // 播动画
        if (actionData.publicCardId() != null) packets.add(new PublicCardsUpdate(room.getPublicCardAccesses()));
        packets.add(new PrivateHandUpdate(player.toAccess(), player.getHandCardAccesses()));
        return packets;
    }

    /**
     * 处理移动卡牌（MOVE）
     * @return 事件列表
     */
    public List<Packet> handleMove(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || !(data instanceof ActionBroadcast.ActionData actionData)) return packets;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayer().equals(player)) return packets;
        // TODO: 校验 & 执行移动逻辑
        packets.add(new ActionBroadcast(player.toAccess(), "MOVE", actionData)); // 播动画
        packets.add(new PublicCardsUpdate(room.getPublicCardAccesses())); //一定产生update（即使不动也要update）
        return packets;
    }

    /**
     * 处理回合结束
     * @return 事件列表
     */
    public List<Packet> handleFinish(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || data != null) return packets;
        Player player = room.getPlayers().get(playerId);
        if (player == null || player.getHp() <= 0 || !room.getCurrentTurnPlayer().equals(player)) return packets;
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        list.remove(player);
        packets.add(new FinishTurnBroadcast(list.getFirst().toAccess()));
        newTurn(room, list.getFirst(), packets::add);
        // TODO: if(新的回合) packets.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        // TODO: if(结束) packets.add(new EndBroadcast(...));
        return packets;
    }

    public void newTurn(GameRoom room, Player player, Consumer<Packet> addPacket) {
        room.setCurrentTurnPlayer(player);
        // TODO: 开新的turn
        EventBus.call(new TurnStartEvent(room, addPacket));
    }
}
