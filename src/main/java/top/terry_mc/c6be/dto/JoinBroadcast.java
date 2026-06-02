package top.terry_mc.c6be.dto;

import lombok.AllArgsConstructor;
import top.terry_mc.c6be.model.PlayerAccess;

public record JoinBroadcast(PlayerAccess joiningPlayer) {
}
