package top.terry_mc.c6be.events;

import lombok.Getter;
import top.terry_mc.c6be.model.Card;
import top.terry_mc.c6be.model.GameRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Called before the moving
 */
public class MoveCardEvent extends GameEvent{
    private static final List<EventListener> listeners = new ArrayList<>();

    @Getter
    private final Card card;
    @Getter
    private final int targetPublicCardId;

    public MoveCardEvent(GameRoom room, Card card, int targetPublicCardId) {
        super(room, listeners);
        this.card = card;
        this.targetPublicCardId = targetPublicCardId;
    }

    public static void addListener(EventListener listener) {
        listeners.add(listener);
    }
}
