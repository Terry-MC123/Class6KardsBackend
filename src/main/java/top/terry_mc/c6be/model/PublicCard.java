package top.terry_mc.c6be.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 表示棋盘上的一个格子，可以为空（card==null）
 */
@Data
@AllArgsConstructor
public class PublicCard {
    private Card card; // 可以为 null 表示空格
    private Float hp; // 当前血量，若 card==null 则可为 0
    private Integer ownerPlayerPublicId; // 持有者的 publicCardId (玩家 HQ 索引)，特殊卡（player1/player2）可设置为对应玩家索引
    private boolean canAct; // 本回合是否可以行动（部署回合设为 false）
    private boolean hasMoved; // 本回合是否已移动
    // 当前（受课程影响的）属性，若 card==null 或 SPECIAL 则为 null
    private Float currAttack;
    private Float currDefense;
    private Integer currSpeed;
    private Integer remainingMove;
    public PublicCard(Card card, Float hp, Integer ownerPlayerPublicId, boolean canAct, boolean hasMoved) {
        this.card = card;
        this.hp = hp;
        this.ownerPlayerPublicId = ownerPlayerPublicId;
        this.canAct = canAct;
        this.hasMoved = hasMoved;
        if (card != null && card.getCardType() == Card.CardType.CHARACTER) {
            this.currAttack = card.getAttack();
            this.currDefense = card.getDefense();
            this.currSpeed = card.getSpeed();
        } else {
            this.currAttack = null;
            this.currDefense = null;
            this.currSpeed = null;
            this.remainingMove = 0;
        }
    }
    public void resetForNewTurn() {
        if (this.currSpeed != null) this.remainingMove = this.currSpeed;
        this.hasMoved = false;
        if (this.card != null) this.canAct = true;
    }
}

