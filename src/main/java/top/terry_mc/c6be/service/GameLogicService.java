package top.terry_mc.c6be.service;

import org.springframework.stereotype.Service;
import top.terry_mc.c6be.dto.*;
import top.terry_mc.c6be.events.EventBus;
// ...existing imports...
import top.terry_mc.c6be.events.TurnStartEvent;
import top.terry_mc.c6be.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

// TODO: 逻辑中加入所有的events（events包内的，不是广播事件）
@Service
public class GameLogicService {
    private final RoomService roomService;

    public GameLogicService(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * 处理玩家加入
     * @return 事件列表
     */
    public List<Packet> handleJoin(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if(!(data instanceof String playerName)) return packets;
        // 分配玩家的 publicCardId（总部格子索引）：先手放在上方中间（索引 2），后手放在下方中间（索引 22）
        int publicCardId = room == null ? 2 : 22;
        Player player = new Player(playerId, playerName, publicCardId);
        if (room == null) {
            room = roomService.createRoom(roomId, player);
        } else {
            roomService.addPlayer(roomId, player);
        }
        packets.add(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        packets.add(new JoinBroadcast(player.toAccess()));
        if(room.getPlayers().size() == 2) {
            gameStart(room, packets::add);
        }
        return packets;
    }

    private void gameStart(GameRoom room, Consumer<Packet> addPacket) {
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        Player first = list.get(0);
        Player second = list.get(1);
        room.setClasses(generateClasses());
        room.setClassCnt(0);
        // 生成并洗牌
        java.util.List<Card> deck = new ArrayList<>(Card.getAllNonSpecialCards());
        Collections.shuffle(deck);
        room.setDeck(deck);
        // 初始化 5x5 棋盘
        java.util.List<PublicCard> board = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) board.add(new PublicCard(null, 0F, null, true, false));
        // 放置两位玩家的 HP 卡（player1/player2）到 HQ 位置
        Card p1 = Card.getCardById("player1");
        Card p2 = Card.getCardById("player2");
        board.set(first.getPublicCardId(), new PublicCard(p1, p1.getDefense(), first.getPublicCardId(), true, false));
        board.set(second.getPublicCardId(), new PublicCard(p2, p2.getDefense(), second.getPublicCardId(), true, false));
        room.setPublicCards(board);
        // 在每节课开始前（第一个 class 开始），应用课程效果（classCnt 初始为 0）
        if (room.getClasses() != null && room.getClassCnt() != null) {
            applyCurrentClassEffects(room, addPacket);
        }

        // 发牌
        drawCards(room, first, 4);
        drawCards(room, second, 4);

        addPacket.accept(new StartBroadcast(room.getClasses()));
        newTurn(room, first, addPacket);
        room.setStatus(GameStatus.PLAYING);
    }

    private void drawCards(GameRoom room, Player player, int count) {
        if (room.getDeck() == null) return;
        for (int i = 0; i < count; i++) {
            java.util.List<Card> deck = room.getDeck();
            if (deck.isEmpty()) break;
            Card c = deck.remove(deck.size() - 1);
            player.getHandCards().add(c);
        }
    }

    private void applyCurrentClassEffects(GameRoom room, Consumer<Packet> addPacket) {
        if (room.getClasses() == null || room.getClasses().isEmpty() || room.getClassCnt() == null) return;
        int idx = room.getClassCnt();
        if (idx < 0 || idx >= room.getClasses().size()) return;
        Subject subject = room.getClasses().get(idx);
        // S:3.0 A:2.0 B:1.0 C:0.5 D:0.25
        room.getPublicCards().forEach(slot -> {
            if (slot.getCard() != null && slot.getCard().getCardType() == Card.CardType.CHARACTER) {
                Card card = slot.getCard();
                Card.AttributeLevel level = card.getAttributes().get(subject);
                if (level == null) level = Card.AttributeLevel.B;
                float k = level.getK();
                slot.setCurrAttack(card.getAttack() == null ? 0F : card.getAttack() * k);
                slot.setCurrDefense(card.getDefense() == null ? 0F : card.getDefense() * k);
                slot.setCurrSpeed(card.getSpeed() == null ? 0 : Math.max(0, Math.round(card.getSpeed() * k)));
            }
        });
        // 通知前端更新公示区
        addPacket.accept(new PublicCardsUpdate(room.getPublicCardAccesses()));
    }

    private List<Subject> generateClasses() {
        List<Subject> list = Arrays.asList(Subject.values());
        Collections.shuffle(list);
        return List.copyOf(list);
    }

    /**
     * 处理使用卡牌（USE）
     * @return 事件列表
     */
    public List<Packet> handleUse(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || !(data instanceof ActionBroadcast.ActionData actionData)) return packets;
        Player player = room.getPlayers().get(playerId);
        // debug: print current turn and acting player ids
        if (room.getCurrentTurnPlayer() == null) {
            System.out.println("DEBUG: currentTurnPlayer is null");
        } else {
            System.out.println("DEBUG: currentTurnPlayer=" + room.getCurrentTurnPlayer().getPlayerId() + ", acting=" + playerId);
        }
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
        // 处理部署（如果是 CHARACTER 且指定了 publicCardId）
        if (actionData.card() != null && actionData.publicCardId() != null) {
            Card card = Card.getCardById(actionData.card().cardId());
            int target = actionData.publicCardId();
            System.out.println("DEBUG handleUse: roomId="+roomId+" player="+playerId+" target="+target+" accessCardId="+(actionData.card()==null?"null":actionData.card().cardId())+" cardObj="+(card==null?"null":card.getCardId()));
            if (card != null && card.getCardType() == Card.CardType.CHARACTER) {
                // 校验：目标格为空且在我方可部署区域（靠近我方总部的两行）
                PublicCard slot = room.getPublicCards().get(target);
                if (slot.getCard() == null) {
                    int row = target / 5;
                    int myRow = player.getPublicCardId() / 5;
                    boolean isTopPlayer = myRow < 2; // top players have HQ in top area
                    boolean deployAllowed = isTopPlayer ? row <= 1 : row >= 3;
                    if (deployAllowed) {
                        // 放置到格子上并允许本回合行动（测试期望可立即行动）
                        room.getPublicCards().set(target, new PublicCard(card, card.getDefense(), player.getPublicCardId(), true, false));
                        System.out.println("DEBUG after set: slot at " + target + " card=" + (room.getPublicCards().get(target).getCard()==null?"null":room.getPublicCards().get(target).getCard().getCardId()));
                        player.getHandCards().removeIf(c -> c.getCardId().equals(card.getCardId()));
                    }
                }
            } else if (card != null && card.getCardType() == Card.CardType.COMMAND) {
                // Command 卡：校验花费并执行 effectId（若有）
                Integer cost = card.getCost();
                if (cost == null) cost = 0;
                if (player.getCommandPoint() >= cost) {
                    player.setCommandPoint(player.getCommandPoint() - cost);
                    // 从手牌移除
                    player.getHandCards().removeIf(c -> c.getCardId().equals(card.getCardId()));
                    // 执行 effect
                    if (card.getEffectId() != null) {
                        packets.addAll(EffectEngine.applyEffectString(room, player, card.getEffectId(), actionData.targetPublicCardId()));
                    }
                }
            }
        }
        packets.add(new ActionBroadcast(player.toAccess(), "USE", actionData)); // 播动画
        if (actionData.publicCardId() != null) packets.add(new PublicCardsUpdate(room.getPublicCardAccesses()));
        packets.add(new PrivateHandUpdate(player.toAccess(), player.getHandCardAccesses()));
        return packets;
    }

    /**
     * 处理移动卡牌（MOVE）
     * @return 事件列表
     */
    public List<Packet> handleMove(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || !(data instanceof ActionBroadcast.ActionData actionData)) return packets;
        Player player = room.getPlayers().get(playerId);
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
        // 校验并执行移动/攻击逻辑：支持 path（逐格路径）及直接指定 targetPublicCardId
        if (actionData.card() != null && actionData.publicCardId() != null) {
            int src = actionData.publicCardId();
            System.out.println("DEBUG handleMove: roomId="+roomId+" player="+playerId+" src="+src+" path="+actionData.path()+" targetPublicCardId="+actionData.targetPublicCardId());
            PublicCard srcSlot = room.getPublicCards().get(src);
            if (srcSlot.getCard() == null || srcSlot.getOwnerPlayerPublicId() == null || !srcSlot.getOwnerPlayerPublicId().equals(player.getPublicCardId())) {
                // 非我方角色或空格，忽略
            } else if (!srcSlot.isCanAct() || srcSlot.isHasMoved()) {
                // 不能行动或已移动
            } else {
                Card moving = srcSlot.getCard();
                int speed = srcSlot.getCurrSpeed() != null ? srcSlot.getCurrSpeed() : (moving.getSpeed() == null ? 0 : moving.getSpeed());
                // path 优先
                java.util.List<Integer> path = actionData.path();
                if (path != null && path.size() >= 2) {
                    // 校验路径首位为 src
                    if (!path.get(0).equals(src)) {
                        // invalid
                    } else {
                        int clicks = path.size() - 1;
                        if (clicks <= speed && clicks <= (srcSlot.getRemainingMove() == null ? speed : srcSlot.getRemainingMove())) {
                            // 逐格遍历
                            boolean stopped = false;
                            int finalPos = src;
                            for (int i = 1; i < path.size(); i++) {
                                int pos = path.get(i);
                                PublicCard s = room.getPublicCards().get(pos);
                                // 若路径中有友方角色，则该路径非法，停止
                                if (s.getCard() != null && s.getOwnerPlayerPublicId() != null && s.getOwnerPlayerPublicId().equals(player.getPublicCardId())) {
                                    stopped = true; break;
                                }
                                if (s.getCard() != null) {
                                    // 遇到敌方，攻击该敌方并停在前一格
                                    int prev = path.get(i-1);
                                    PublicCard prevSlot = room.getPublicCards().get(prev);
                                    // 执行攻击
                                    float atk = srcSlot.getCurrAttack() == null ? 0F : srcSlot.getCurrAttack();
                                    float tgtHp = s.getHp() - atk;
                                    s.setHp(tgtHp);
                                    // 反击
                                    if (tgtHp > 0 && s.getCard().getCardType() == Card.CardType.CHARACTER) {
                                        float counter = s.getCurrAttack() == null ? 0F : s.getCurrAttack();
                                        float srcHp = srcSlot.getHp() - counter;
                                        srcSlot.setHp(srcHp);
                                        if (srcHp <= 0) {
                                            room.getPublicCards().set(src, new PublicCard(null, 0F, null, true, false));
                                        } else {
                                            room.getPublicCards().set(src, srcSlot);
                                        }
                                    } else if (tgtHp <= 0) {
                                        room.getPublicCards().set(pos, new PublicCard(null, 0F, null, true, false));
                                    }
                                    finalPos = prev;
                                    stopped = true;
                                    break;
                                } else {
                                    finalPos = pos;
                                }
                            }
                             if (!stopped) {
                                // 移动到 finalPos
                                if (finalPos != src) {
                                    PublicCard newDst = new PublicCard(moving, srcSlot.getHp(), srcSlot.getOwnerPlayerPublicId(), false, true);
                                    newDst.setCurrAttack(srcSlot.getCurrAttack());
                                    newDst.setCurrDefense(srcSlot.getCurrDefense());
                                    newDst.setCurrSpeed(srcSlot.getCurrSpeed());
                                    room.getPublicCards().set(finalPos, newDst);
                                    room.getPublicCards().set(src, new PublicCard(null, 0F, null, true, false));
                                    System.out.println("DEBUG moved to finalPos="+finalPos+" card="+(room.getPublicCards().get(finalPos).getCard()==null?"null":room.getPublicCards().get(finalPos).getCard().getCardId()));
                                }
                            } else {
                                // 停止在 finalPos（已处理攻击/反击）
                                if (finalPos != src && room.getPublicCards().get(finalPos).getCard() == null) {
                                    // 将攻击者放到前一格
                                    PublicCard newDst = new PublicCard(moving, srcSlot.getHp(), srcSlot.getOwnerPlayerPublicId(), false, true);
                                    newDst.setCurrAttack(srcSlot.getCurrAttack());
                                    newDst.setCurrDefense(srcSlot.getCurrDefense());
                                    newDst.setCurrSpeed(srcSlot.getCurrSpeed());
                                    room.getPublicCards().set(finalPos, newDst);
                                    room.getPublicCards().set(src, new PublicCard(null, 0F, null, true, false));
                                    System.out.println("DEBUG moved (stopped) to finalPos="+finalPos+" card="+(room.getPublicCards().get(finalPos).getCard()==null?"null":room.getPublicCards().get(finalPos).getCard().getCardId()));
                                }
                            }
                            // 扣除移动点
                            int rem = srcSlot.getRemainingMove() == null ? speed : srcSlot.getRemainingMove();
                            srcSlot.setRemainingMove(Math.max(0, rem - clicks));
                            srcSlot.setHasMoved(true);
                        }
                    }
                } else if (actionData.targetPublicCardId() != null) {
                    int dst = actionData.targetPublicCardId();
                    PublicCard dstSlot = room.getPublicCards().get(dst);
                    int sr = src / 5, sc = src % 5, dr = dst / 5, dc = dst % 5;
                    int dist = Math.abs(sr - dr) + Math.abs(sc - dc);
                    if (dist <= speed && !srcSlot.isHasMoved() && srcSlot.isCanAct()) {
                        if (dstSlot.getCard() == null) {
                            // 普通移动（复制当前属性）
                            PublicCard newDst = new PublicCard(moving, srcSlot.getHp(), srcSlot.getOwnerPlayerPublicId(), false, true);
                            newDst.setCurrAttack(srcSlot.getCurrAttack());
                            newDst.setCurrDefense(srcSlot.getCurrDefense());
                            newDst.setCurrSpeed(srcSlot.getCurrSpeed());
                            room.getPublicCards().set(dst, newDst);
                            room.getPublicCards().set(src, new PublicCard(null, 0F, null, true, false));
                        } else {
                            // 攻击目标
                            Card target = dstSlot.getCard();
                            float atk = srcSlot.getCurrAttack() == null ? 0F : srcSlot.getCurrAttack();
                            float tgtHp = dstSlot.getHp() - atk;
                            dstSlot.setHp(tgtHp);
                            // 若目标仍然存活且不是 player 卡，则反击一次（使用目标的当前攻击）
                            if (tgtHp > 0 && target.getCardType() == Card.CardType.CHARACTER) {
                                float counterAtk = dstSlot.getCurrAttack() == null ? 0F : dstSlot.getCurrAttack();
                                float srcHp = srcSlot.getHp() - counterAtk;
                                srcSlot.setHp(srcHp);
                                if (srcHp <= 0) {
                                    room.getPublicCards().set(src, new PublicCard(null, 0F, null, true, false));
                                } else {
                                    // 更新原格的 hp
                                    room.getPublicCards().set(src, srcSlot);
                                }
                            } else if (tgtHp <= 0) {
                                // 目标死亡
                                room.getPublicCards().set(dst, new PublicCard(null, 0F, null, true, false));
                            }
                        }
                        // 标记移动
                        srcSlot.setHasMoved(true);
                        srcSlot.setRemainingMove(Math.max(0, (srcSlot.getRemainingMove()==null?speed:srcSlot.getRemainingMove()) - dist));
                    }
                }
            }
        }
        packets.add(new ActionBroadcast(player.toAccess(), "MOVE", actionData)); // 播动画
        packets.add(new PublicCardsUpdate(room.getPublicCardAccesses())); //一定产生update（即使不动也要update）
        // 检查是否有玩家HQ血量降到不大于0，若是则结束游戏
        PlayerAccess winner = checkAndHandleGameEnd(room);
        if (winner != null) {
            packets.add(new EndBroadcast(winner));
        }
        return packets;
    }

    /**
     * 处理回合结束
     * @return 事件列表
     */
    public List<Packet> handleFinish(String roomId, String playerId, Object data) {
        List<Packet> packets = new ArrayList<>();
        GameRoom room = roomService.getRoom(roomId);
        if (room == null || data != null) return packets;
        Player player = room.getPlayers().get(playerId);
        if (!room.getCurrentTurnPlayer().equals(player)) return packets;
        List<Player> list = new ArrayList<>(room.getPlayers().values());
        list.remove(player);
        packets.add(new FinishTurnBroadcast(list.get(0).toAccess()));
        newTurn(room, list.get(0), packets::add);
        if(room.getClassCnt()==room.getClasses().size()) {
            packets.add(new EndBroadcast(getWinningPlayerAccess(room)));
            room.setStatus(GameStatus.FINISHED);
        }
        return packets;
    }

    private PlayerAccess checkAndHandleGameEnd(GameRoom room) {
        if (room.getPlayers().size() < 2) return null;
        java.util.List<Player> players = new ArrayList<>(room.getPlayers().values());
        Player p1 = players.get(0), p2 = players.get(1);
        Float hp1 = getPlayerHp(room, p1), hp2 = getPlayerHp(room, p2);
        if (hp1 <= 0 && hp2 <= 0) {
            room.setStatus(GameStatus.FINISHED);
            return null; // tie -> no winner
        } else if (hp1 <= 0) {
            room.setStatus(GameStatus.FINISHED);
            return p2.toAccess();
        } else if (hp2 <= 0) {
            room.setStatus(GameStatus.FINISHED);
            return p1.toAccess();
        }
        return null;
    }

    private PlayerAccess getWinningPlayerAccess(GameRoom room) {
        List<Player> players = new ArrayList<>(room.getPlayers().values());
        Float hp1 = getPlayerHp(room,players.get(0)), hp2 = getPlayerHp(room,players.get(1));
        if (hp1 > hp2) {
            return players.get(0).toAccess();
        } else if (hp1 < hp2) {
            return players.get(1).toAccess();
        }
        return null;
    }

    private Float getPlayerHp(GameRoom room, Player player) {
        java.util.Map.Entry<CardAccess, Float> e = room.getPublicCardAccesses().get(player.getPublicCardId());
        return e == null || e.getKey() == null ? 0F : e.getValue();
    }

    public void newTurn(GameRoom room, Player player, Consumer<Packet> addPacket) {
        Player previous = room.getCurrentTurnPlayer();
        room.setCurrentTurnPlayer(player);
        if (room.getRoundCnt() == null || room.getRoundCnt() == 0) {
            room.setRoundCnt(1);
            addPacket.accept(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
        } else {
            List<Player> list = new ArrayList<>(room.getPlayers().values());
            if (list.size() >= 2 && previous != null) {
                Player first = list.get(0);
                Player second = list.get(1);
                if (previous.equals(second) && player.equals(first)) {
                    int nextRound = room.getRoundCnt() + 1;
                    if (nextRound > 3) {
                        room.setRoundCnt(1);
                        if (room.getClassCnt() == null) room.setClassCnt(0);
                        if (room.getClasses() != null && !room.getClasses().isEmpty()) {
                            room.setClassCnt(room.getClassCnt() + 1);
                            // 新的一节课开始，应用课程效果
                            applyCurrentClassEffects(room, addPacket);
                        }
                    } else {
                        room.setRoundCnt(nextRound);
                    }
                    addPacket.accept(new RoundUpdate(room.getRoundCnt(), room.getClassCnt()));
                }
            }
        }
        // 回合开始：双方指挥点上限+1并填满，同时各摸一张牌；并将每个格子的 canAct/hasMoved 重置（角色在部署回合无法行动）
        room.getPlayers().values().forEach(p -> {
            p.setCommandPointCap(p.getCommandPointCap() + 1);
            p.setCommandPoint(p.getCommandPointCap());
            drawCards(room, p, 1);
            addPacket.accept(new PrivateHandUpdate(p.toAccess(), p.getHandCardAccesses()));
        });
        room.getPublicCards().forEach(slot -> {
            if (slot.getCard() != null) {
                slot.resetForNewTurn();
            }
        });
        EventBus.call(new TurnStartEvent(room, addPacket));
    }
}
