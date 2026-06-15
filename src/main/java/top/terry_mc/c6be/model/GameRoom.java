package top.terry_mc.c6be.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public class GameRoom {
    private final String roomId;
    private Integer roundCnt;
    private Integer classCnt;
    private List<Subject> classes;
    private Player currentTurnPlayer;
    private List<Map.Entry<Card,Float>> publicCards;
    private final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>(); // playerId -> Player
    private GameStatus status;

    public List<Map.Entry<CardAccess,Float>> getPublicCardAccesses() {
        return publicCards.stream().map(entry -> Map.entry(entry.getKey().toAccess(), entry.getValue())).collect(Collectors.toList());
    }
}
