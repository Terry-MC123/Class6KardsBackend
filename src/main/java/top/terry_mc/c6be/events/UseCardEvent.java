package top.terry_mc.c6be.events;

import lombok.Getter;
import top.terry_mc.c6be.dto.ActionBroadcast;
import top.terry_mc.c6be.model.Card;
import top.terry_mc.c6be.model.GameRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Called before the using
 */
public class UseCardEvent extends GameEvent{
    private static final List<EventListener> listeners = new ArrayList<>();

    @Getter
    private final Card card;

    public UseCardEvent(GameRoom room, Card card) {
        super(room, listeners);
        this.card = card;
    }

    public static void addListener(EventListener listener) {
        listeners.add(listener);
    }
}
