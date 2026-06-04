package top.terry_mc.c6be.events;

import top.terry_mc.c6be.dto.Packet;
import top.terry_mc.c6be.model.GameRoom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Called after starting the turn
 */
public class TurnStartEvent extends GameEvent{
    private static final List<EventListener> listeners = new ArrayList<>();

    public TurnStartEvent(GameRoom room, Consumer<Packet> addPacket) {
        super(room, listeners, addPacket);
    }

    public static void addListener(EventListener listener) {
        listeners.add(listener);
    }
}
