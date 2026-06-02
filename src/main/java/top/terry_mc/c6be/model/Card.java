package top.terry_mc.c6be.model;

import lombok.Data;
import top.terry_mc.c6be.events.EventListener;

import java.util.List;

// TODO: 卡牌注册
@Data
public class Card {
    private final String cardId;
    private final CardType cardType;
    public enum CardType {
        CHARACTER, COMMAND
    }
    //TODO: 其他属性塞里面

    private List<EventListener> listeners;

    public CardAccess toAccess() {
        return new CardAccess(cardId, cardType);
    }
}
