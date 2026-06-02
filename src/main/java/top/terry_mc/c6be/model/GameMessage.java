package top.terry_mc.c6be.model;

/**
 * @param type JOIN USE MOVE FINISH
 */
public record GameMessage(String type, String roomId, String playerId, Object data) {
}
