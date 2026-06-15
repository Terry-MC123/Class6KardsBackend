package top.terry_mc.c6be.dto;

import top.terry_mc.c6be.model.CardAccess;

import java.util.List;
import java.util.Map;

public record PublicCardsUpdate(List<Map.Entry<CardAccess, Float>> publicCards) implements Packet {
}