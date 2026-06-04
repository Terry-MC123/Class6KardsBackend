package top.terry_mc.c6be.dto;

import top.terry_mc.c6be.model.PlayerAccess;

/**
 * @param nextPlayer 下一个玩家
 */
public record FinishTurnBroadcast(PlayerAccess nextPlayer) implements Packet {
}
