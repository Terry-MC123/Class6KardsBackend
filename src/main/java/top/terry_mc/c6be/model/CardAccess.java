package top.terry_mc.c6be.model;

import java.util.Map;

public record CardAccess(String cardId, Card.CardType cardType, Map<Subject, Card.AttributeLevel> attributes) {
}
