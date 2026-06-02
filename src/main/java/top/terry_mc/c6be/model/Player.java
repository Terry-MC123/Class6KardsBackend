package top.terry_mc.c6be.model;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class Player {
    private final String playerId;
    private final String name;
    private int hp;
    private List<Card> handCards;
    public PlayerAccess toAccess() {
        return new PlayerAccess(playerId,name,hp);
    }
    public List<CardAccess> getHandCardAccesses() {
        return handCards.stream().map(Card::toAccess).collect(Collectors.toList());
    }
}
