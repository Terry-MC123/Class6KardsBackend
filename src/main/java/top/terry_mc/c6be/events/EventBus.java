package top.terry_mc.c6be.events;

import java.lang.reflect.InvocationTargetException;

public class EventBus {
    public static void register(EventListener listener, Class<? extends GameEvent> clazz) {
        addListener(listener, clazz);
    }

    private static void addListener(EventListener listener, Class<? extends GameEvent> clazz) {
        try {
            clazz.getMethod("addListener", EventListener.class).invoke(null, listener);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
    }

    public static void call(GameEvent event) {
        event.getListeners().forEach(listener -> listener.accept(event));
    }
}
