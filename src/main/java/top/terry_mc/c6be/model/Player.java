package top.terry_mc.c6be.model;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class Player {
    private final String playerId;
    private final String name;
    private final Integer publicCardId;
    private java.util.List<Card> handCards = new java.util.ArrayList<>();
    private Integer commandPointCap = 1;
    private Integer commandPoint = 1;
    public PlayerAccess toAccess() {
        return new PlayerAccess(playerId,name,publicCardId);
    }
    public List<CardAccess> getHandCardAccesses() {
        return handCards.stream().map(Card::toAccess).collect(Collectors.toList());
    }
}
