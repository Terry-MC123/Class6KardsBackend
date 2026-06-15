package top.terry_mc.c6be.dto;

import jakarta.annotation.Nullable;
import top.terry_mc.c6be.model.CardAccess;
import top.terry_mc.c6be.model.PlayerAccess;

/**
 * @param actionType USE MOVE
 */
public record ActionBroadcast(PlayerAccess player, String actionType, ActionData data) implements Packet {
    /**
     * @param publicCardId       USE：要去的这一张publicCardId；MOVE：动的这一张publicCardId；null就是这牌没了
     * @param targetPublicCardId move用的目标牌位置
     */
    public record ActionData(CardAccess card, @Nullable Integer publicCardId, @Nullable Integer targetPublicCardId) {
    }
}
