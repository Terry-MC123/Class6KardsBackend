package top.terry_mc.c6be.events;

import top.terry_mc.c6be.model.GameRoom;

import java.util.ArrayList;
import java.util.List;

public class TurnStartEvent extends GameEvent{
    private static final List<EventListener> listeners = new ArrayList<>();

    public TurnStartEvent(GameRoom room) {
        super(room, listeners);
    }

    public static void addListener(EventListener listener) {
        listeners.add(listener);
    }
}
