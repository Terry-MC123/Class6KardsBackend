package top.terry_mc.c6be.model;

import top.terry_mc.c6be.dto.ActionBroadcast;

/**
 * @param type JOIN/USE/MOVE/FINISH
 * @param data <br/>JOIN - {@link String}<br/>
 *             USE - {@link ActionBroadcast.ActionData}<br/>
 *             MOVE - {@link ActionBroadcast.ActionData}<br/>
 *             FINISH - {@code null}<br/>
 */
public record ClientPayload(String type, String roomId, String playerId, Object data) {
}