package top.terry_mc.c6be.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import top.terry_mc.c6be.events.EventBus;
import top.terry_mc.c6be.events.EventListener;
import top.terry_mc.c6be.events.GameEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static top.terry_mc.c6be.model.Card.AttributeLevel.*;

@Data
public class Card {
    private static final ConcurrentHashMap<String, Card> registeredCards = new ConcurrentHashMap<>();
    private final String cardId;
    private final CardType cardType;
    public enum CardType {
        SPECIAL, CHARACTER, COMMAND
    }
    @AllArgsConstructor
    public enum AttributeLevel {
        S(3.0F),A(2.0F),B(1.0F),C(0.5F),D(0.2F);
        @Getter
        private final float k;
    }
    private final Map<Subject, AttributeLevel> attributes;
    private final Float defense, attack;
    private final Integer speed;
    private final List<Map.Entry<EventListener, Class<? extends GameEvent>>> listeners;
    private final Integer cost; // for COMMAND cards
    private final String effectId; // optional effect identifier
    public CardAccess toAccess() {
        return new CardAccess(cardId, cardType, attributes);
    }

    public static java.util.List<Card> getAllNonSpecialCards() {
        return registeredCards.values().stream().filter(c -> c.cardType != CardType.SPECIAL).toList();
    }
    private static void register(String id, CardType type, Map<Subject, AttributeLevel> attributes, Float defense, Float attack, Integer speed, List<Map.Entry<EventListener, Class<? extends GameEvent>>> listeners, Integer cost, String effectId) {
        Card card = new Card(id, type, attributes, defense, attack, speed, listeners, cost, effectId);
        if(listeners!=null)
            listeners.forEach(listener -> EventBus.register(listener.getKey(), listener.getValue()));
        registeredCards.put(id, card);
    }

    public static Card getCardById(String id) {
        return registeredCards.get(id);
    }
    //语数英物化政史体
    private static Map<Subject, AttributeLevel> getAttributes(AttributeLevel... attributes) {
        Subject[] subjects = Subject.values();
        if(attributes.length!=subjects.length) return null;
        Map<Subject, AttributeLevel> map = new HashMap<>();
        for(int i = 0; i < subjects.length; i++) {
            map.put(subjects[i], attributes[i]);
        }
        return Map.copyOf(map);
    }

    static {
        register("player1", CardType.SPECIAL, null, 20F, null, null, null, null, null);
        register("player2", CardType.SPECIAL, null, 20F, null, null, null, null, null);

        register("li_an", CardType.CHARACTER, getAttributes(S,B,A,S,S,B,B,A), 8F, 8F, 6, null, null, null);
        register("liu_zhaoyi", CardType.CHARACTER, getAttributes(S,A,S,A,A,S,S,C), 7F, 4F, 4, null, null, null);
        register("zhao_xiaochuan", CardType.CHARACTER, getAttributes(A,B,S,B,B,A,A,B), 6F, 5F, 5, null, null, null);
        register("li_boxuan", CardType.CHARACTER, getAttributes(B,A,A,S,A,B,B,A), 5F, 6F, 6, null, null, null);
        register("liu_shuhan", CardType.CHARACTER, getAttributes(B,B,B,A,S,B,B,C), 6F, 4F, 4, null, null, null);
        register("liu_yuchen", CardType.CHARACTER, getAttributes(B,A,B,B,B,B,B,B)/*不确定*/, 6F, 5F, 4, null, null, null);
        register("tang_haidong", CardType.CHARACTER, getAttributes(B,S,A,S,S,A,A,A), 5F, 6F, 6, null, null, null);
        register("ye_leshan", CardType.CHARACTER, getAttributes(C,A,S,S,S,A,A,C), 5F, 6F, 6, null, null, null);
        register("tai_ger"/*省略一个e*/, CardType.CHARACTER, getAttributes(B,A,A,A,A,B,B,A), 5F, 6F, 6, null, null, null);
        register("chen_zhengxvan", CardType.CHARACTER, getAttributes(B,A,B,A,A,S,S,C), 5F, 4F, 4, null, null, null);
        register("tian_baojun", CardType.CHARACTER, getAttributes(B,B,B,B,B,B,B,S)/*不确定*/, 5F, 6F, 7, null, null, null);
        register("zang_youzhi", CardType.CHARACTER, getAttributes(B,B,B,A,A,B,B,S/*不确定*/), 6F, 6F, 7, null, null, null);
        register("zhao_xianghao", CardType.CHARACTER, getAttributes(C,B,C,A,A,B,C,C), 8F, 6F, 4, null, null, null);
        register("shi_wenjie", CardType.CHARACTER, getAttributes(B,B,B,B,B,B,B,B)/*不确定*/, 4F, 4F, 5, null, null, null);
        register("li_jinhao", CardType.CHARACTER, getAttributes(C,C,C,C,C,C,C,A), 5F, 7F, 7, null, null, null);
        register("niu_yujie", CardType.CHARACTER, getAttributes(C,B,A,C,C,C,C,A), 5F, 6F, 6, null, null, null);
        register("wu_jinmeng", CardType.CHARACTER, getAttributes(C,C,C,B,B,C,C,A), 7F, 8F, 6, null, null, null);
        register("ni_haocheng", CardType.CHARACTER, getAttributes(B,A,A,B,B,B,B,A), 6F, 5F, 5, null, null, null);
        register("zhang_lusu", CardType.CHARACTER, getAttributes(C,B,A,C,C,C,C,A), 5F, 6F, 6, null, null, null);
        register("niu_yujie", CardType.CHARACTER, getAttributes(C,B,A,C,C,C,C,A), 5F, 6F, 6, null, null, null);



    }
}
