package top.terry_mc.c6be.service;

import top.terry_mc.c6be.dto.Packet;
import top.terry_mc.c6be.dto.PublicCardsUpdate;
import top.terry_mc.c6be.model.GameRoom;
import top.terry_mc.c6be.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 简单的效果引擎：支持 effectId 类似 "damage:3" 或 "heal:2"，作用于目标 publicCardId
 */
public class EffectEngine {
    private static final Map<String, BiFunction<EffectContext, Integer, List<Packet>>> registry = new HashMap<>();

    static {
        // damage:{n}
        registry.put("damage", (ctx, amount) -> {
            List<Packet> out = new ArrayList<>();
            if (ctx.targetPublicCardId == null) return out;
            int tid = ctx.targetPublicCardId;
            var slot = ctx.room.getPublicCards().get(tid);
            if (slot.getCard() == null) return out;
            float hp = slot.getHp() - amount;
            slot.setHp(hp);
            out.add(new PublicCardsUpdate(ctx.room.getPublicCardAccesses()));
            return out;
        });
        // heal:{n}
        registry.put("heal", (ctx, amount) -> {
            List<Packet> out = new ArrayList<>();
            if (ctx.targetPublicCardId == null) return out;
            int tid = ctx.targetPublicCardId;
            var slot = ctx.room.getPublicCards().get(tid);
            if (slot.getCard() == null) return out;
            float hp = slot.getHp() + amount;
            slot.setHp(hp);
            out.add(new PublicCardsUpdate(ctx.room.getPublicCardAccesses()));
            return out;
        });
    }

    public static List<Packet> applyEffectString(GameRoom room, Player player, String effectStr, Integer targetPublicCardId) {
        if (effectStr == null || effectStr.isEmpty()) return List.of();
        String[] parts = effectStr.split(":");
        String key = parts[0];
        int amount = 1;
        if (parts.length > 1) {
            try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
        }
        BiFunction<EffectContext, Integer, List<Packet>> fn = registry.get(key);
        if (fn == null) return List.of();
        return fn.apply(new EffectContext(room, player, targetPublicCardId), amount);
    }

    public static class EffectContext {
        public final GameRoom room;
        public final Player player;
        public final Integer targetPublicCardId;
        public EffectContext(GameRoom room, Player player, Integer targetPublicCardId) {
            this.room = room; this.player = player; this.targetPublicCardId = targetPublicCardId;
        }
    }
}

