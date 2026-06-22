package top.terry_mc.c6be.model;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class GameRoom {
    private final String roomId;
    private Integer roundCnt;
    private Integer classCnt;
    private List<Subject> classes;
    private Player currentTurnPlayer;
    // publicCards 现在表示 5x5 的棋盘，按行主序 (row*5 + col) 存放 25 个格子
    private java.util.List<PublicCard> publicCards;
    // 牌堆
    private java.util.List<Card> deck;
    private final java.util.LinkedHashMap<String, Player> players = new java.util.LinkedHashMap<>(); // preserve insertion order: playerId -> Player
    private GameStatus status;

    public java.util.List<java.util.Map.Entry<CardAccess,Float>> getPublicCardAccesses() {
        return publicCards.stream().map(slot -> {
            CardAccess ca = slot.getCard() == null ? null : slot.getCard().toAccess();
            return new java.util.AbstractMap.SimpleEntry<>(ca, slot.getHp());
        }).collect(Collectors.toList());
    }
}
