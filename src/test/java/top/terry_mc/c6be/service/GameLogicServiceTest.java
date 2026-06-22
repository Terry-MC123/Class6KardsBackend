package top.terry_mc.c6be.service;

import org.junit.jupiter.api.Test;
import top.terry_mc.c6be.dto.ActionBroadcast;
import top.terry_mc.c6be.model.Card;
import top.terry_mc.c6be.model.GameRoom;
import top.terry_mc.c6be.model.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GameLogicServiceTest {
    @Test
    public void testGameStartAndDeal() {
        RoomService roomService = new RoomService();
        GameLogicService svc = new GameLogicService(roomService);
        var p1Packets = svc.handleJoin("r1","p1","Alice");
        assertNotNull(p1Packets);
        var p2Packets = svc.handleJoin("r1","p2","Bob");
        assertNotNull(p2Packets);
        GameRoom room = roomService.getRoom("r1");
        assertNotNull(room);
        assertEquals(25, room.getPublicCards().size());
        assertNotNull(room.getDeck());
        // each player should have 5 cards (4 initial + 1 from first turn start)
        assertEquals(2, room.getPlayers().size());
        assertEquals(5, room.getPlayers().get("p1").getHandCards().size());
        assertEquals(5, room.getPlayers().get("p2").getHandCards().size());
    }

    @Test
    public void testDeployAndMove() {
        RoomService roomService = new RoomService();
        GameLogicService svc = new GameLogicService(roomService);
        svc.handleJoin("r2","p1","Alice");
        svc.handleJoin("r2","p2","Bob");
        GameRoom room = roomService.getRoom("r2");
        Player alice = room.getPlayers().get("p1");
        // give Alice a known card in hand
        Card card = Card.getCardById("li_an");
        alice.getHandCards().add(card);
        var cardAccess = card.toAccess();
        ActionBroadcast.ActionData useData = new ActionBroadcast.ActionData(cardAccess, 1, null, null);
        var packets = svc.handleUse("r2","p1", useData);
        // assert public card at index 1 is li_an
        assertEquals("li_an", room.getPublicCards().get(1).getCard().getCardId());
        // move Alice's unit from 1 to 6 via path
        ActionBroadcast.ActionData moveData = new ActionBroadcast.ActionData(cardAccess, 1, 6, List.of(1,6));
        svc.handleMove("r2","p1", moveData);
        assertEquals("li_an", room.getPublicCards().get(6).getCard().getCardId());
    }
}

