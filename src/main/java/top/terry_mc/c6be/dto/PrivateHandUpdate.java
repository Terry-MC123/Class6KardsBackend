package top.terry_mc.c6be.dto;

import top.terry_mc.c6be.model.CardAccess;
import top.terry_mc.c6be.model.PlayerAccess;

import java.util.List;

public record PrivateHandUpdate(PlayerAccess player, List<CardAccess> handCards) implements Packet {
}