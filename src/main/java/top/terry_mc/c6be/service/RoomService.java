package top.terry_mc.c6be.service;

import org.springframework.stereotype.Service;
import top.terry_mc.c6be.model.GameRoom;
import top.terry_mc.c6be.model.GameStatus;
import top.terry_mc.c6be.model.Player;
import top.terry_mc.c6be.model.Subject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();//roomId -> room
    private final Random random = new Random();
    public GameRoom createRoom(String roomId, Player creator) {
        GameRoom room = new GameRoom(roomId);
        room.setClassCnt(0);
        room.setRoundCnt(0);
        room.setClasses(generateClasses());
        room.getPlayers().put(creator.getPlayerId(), creator);
        room.setStatus(GameStatus.WAITING);
        rooms.put(roomId, room);
        return room;
    }
    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }
    public boolean addPlayer(String roomId, Player player) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.getStatus() == GameStatus.WAITING) {
            room.getPlayers().put(player.getPlayerId(), player);
            return true;
        }
        return false;
    }
    private List<Subject> generateClasses() {
        List<Subject> list = Arrays.asList(Subject.values());
        Collections.shuffle(list, random);
        return List.copyOf(list);
    }

}
