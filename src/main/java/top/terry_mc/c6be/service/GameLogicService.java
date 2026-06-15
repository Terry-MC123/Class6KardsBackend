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
        Player player = new Player(playerId, playerName, null);
        if (room == null) {
            room = roomService.createRoom(roomId, player);
        } else {
            roomService.addPlayer(roomId, player);
            // TODO: 若返回false返回错误事件（先暂时不需要实现）
        }
        packets.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        packets.add(new JoinBroadcast(player.toAccess()));
        if(room.getPlayers().size() == 2) {
            gameStart(room, packets::add);
        }
        return packets;
    }

    private void gameStart(GameRoom room, Consumer<Packet> addPacket) {
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        Player first = list.getFirst();
        Player second = list.getLast();
        room.setClasses(generateClasses());
        room.setClassCnt(0);
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
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
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
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
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
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        list.remove(player);
        packets.add(new FinishTurnBroadcast(list.getFirst().toAccess()));
        newTurn(room, list.getFirst(), packets::add);
        if(room.getClassCnt()==room.getClasses().size()) {
            packets.add(new EndBroadcast(getWinningPlayerAccess(room)));
            room.setStatus(GameStatus.FINISHED);
        }
        return packets;
    }

    private PlayerAccess getWinningPlayerAccess(GameRoom room) {
        List<Player> players = new ArrayList<>(room.getPlayers().values());
        Float hp1 = getPlayerHp(room,players.getFirst()), hp2 = getPlayerHp(room,players.getLast());
        if (hp1 > hp2) {
            return players.getFirst().toAccess();
        } else if (hp1 < hp2) {
            return players.getLast().toAccess();
        }
        return null;
    }

    private Float getPlayerHp(GameRoom room, Player player) {
        return room.getPublicCardAccesses().get(player.getPublicCardId()).getValue();
    }

    public void newTurn(GameRoom room, Player player, Consumer<Packet> addPacket) {
        Player previous = room.getCurrentTurnPlayer();
        room.setCurrentTurnPlayer(player);
        if (room.getRoundCnt() == null || room.getRoundCnt() == 0) {
            room.setRoundCnt(1);
            addPacket.accept(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        } else {
            List<Player> list = new ArrayList<>(room.getPlayers().values());
            if (list.size() >= 2 && previous != null) {
                Player first = list.get(0);
                Player second = list.get(1);
                if (previous.equals(second) && player.equals(first)) {
                    int nextRound = room.getRoundCnt() + 1;
                    if (nextRound > 3) {
                        room.setRoundCnt(1);
                        if (room.getClassCnt() == null) room.setClassCnt(0);
                        if (room.getClasses() != null && !room.getClasses().isEmpty()) {
                            room.setClassCnt(room.getClassCnt() + 1);
                        }
                    } else {
                        room.setRoundCnt(nextRound);
                    }
                    addPacket.accept(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
                }
            }
        }
        EventBus.call(new TurnStartEvent(room, addPacket));
    }
}
