package top.terry_mc.c6be.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.terry_mc.c6be.model.GameRoom;

import java.util.List;

/**
 * There should be a static method called {@code addListener(EventListener)} in all events
 */
@AllArgsConstructor
public abstract class GameEvent {
    @Getter
    private GameRoom room;
    @Getter
    private List<EventListener> listeners;

    public static void addListener(EventListener listener) {
        throw new NoSuchMethodError("This event do not have the method addListener");
    }
}
