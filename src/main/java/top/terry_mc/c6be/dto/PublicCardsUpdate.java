package top.terry_mc.c6be.dto;

import top.terry_mc.c6be.model.CardAccess;

import java.util.List;

public record PublicCardsUpdate(List<CardAccess> publicCards) {
}