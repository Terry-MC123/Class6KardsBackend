添加卡牌指南（详细）

目的
----
本文件详细说明如何在代码库中添加/定义卡牌（CHARACTER / COMMAND / SPECIAL），包括属性、学科等级顺序、事件监听（listeners）、指令效果（effectId）、以及如何测试和发布。目的是让其他开发者能够“事无巨细”地按此格式安全添加新卡并验证行为。

相关文件位置（重要）
----
- 卡牌注册与模型：
  - src/main/java/top/terry_mc/c6be/model/Card.java
- 棋盘格子模型：
  - src/main/java/top/terry_mc/c6be/model/PublicCard.java
- 游戏逻辑（会读取所有已注册卡并构建牌堆）：
  - src/main/java/top/terry_mc/c6be/service/GameLogicService.java
- 效果引擎（解析 effectId）：
  - src/main/java/top/terry_mc/c6be/service/EffectEngine.java
- 测试：
  - src/test/java/top/terry_mc/c6be/service/GameLogicServiceTest.java

注册函数签名（内部 private register）
----
在 Card.java 中有一个私有静态方法 register(...)，其参数如下（按顺序）：

register(String id,
         CardType type,
         Map<Subject, AttributeLevel> attributes,
         Float defense,
         Float attack,
         Integer speed,
         List<Map.Entry<EventListener, Class<? extends GameEvent>>> listeners,
         Integer cost,
         String effectId)

字段解释：
- id: 卡牌唯一字符串 id（例："li_an"）。
- type: CardType.SPECIAL / CardType.CHARACTER / CardType.COMMAND。
- attributes: 学科属性 Map（使用 Card.getAttributes(...) 直接生成，详见下）。仅 CHARACTER 需要；COMMAND/SPECIAL 可为 null。
- defense: 该卡的防御值或初始 HP（Float）。对 HQ（SPECIAL）代表玩家初始 HP；对 CHARACTER 为放置时的血量。
- attack: 攻击力（Float）。CHARACTER 常用。
- speed: 移动速度（Integer）。CHARACTER 常用。
- listeners: 可选监听器列表（用于把事件监听器注册到 EventBus），若无监听器传 null。
- cost: 指令卡（COMMAND）的指挥点花费（Integer）。CHARACTER/SPECIAL 可为 null。
- effectId: 指令卡的效果标识（String），由 EffectEngine 解析（如 "damage:3"）。

学科属性书写（顺序非常重要）
----
使用 Card.getAttributes(AttributeLevel... attributes) 辅助方法生成属性 Map。注意：该方法要求传入的 AttributeLevel 数量必须等于 Subject 枚举长度（8 个）。

Subject 枚举（顺序）与对应中文：
- CHINESE  — "语"
- MATHS    — "数"
- ENGLISH  — "英"
- PHYSICS  — "物"
- CHEMISTRY— "化"
- POLITICS — "政"
- HISTORY  — "史"
- PE       — "体"

因此调用顺序（示例）：
getAttributes(AttributeLevel.S, AttributeLevel.B, AttributeLevel.A, AttributeLevel.B, AttributeLevel.B, AttributeLevel.A, AttributeLevel.B, AttributeLevel.C)
对应上面的 8 门课从左到右。

AttributeLevel 枚举及含义：
- S（系数 3.0），A（2.0），B（1.0），C（0.5），D（0.2）。
在课程（class）开始时，角色的原始数值（attack/defense/speed）会乘以当前课程对应的系数并存入格子的 curr* 字段。

如何在 Card.java 中添加卡（直接写法）
----
打开 src/main/java/top/terry_mc/c6be/model/Card.java，找到 static { ... } 初始化块，在 register(...) 的已有列表附近加入新的 register 调用。

示例 1：添加角色卡（CHARACTER）
```java
register("new_hero", CardType.CHARACTER,
         getAttributes(Card.AttributeLevel.S, Card.AttributeLevel.B, Card.AttributeLevel.A,
                       Card.AttributeLevel.B, Card.AttributeLevel.B, Card.AttributeLevel.A,
                       Card.AttributeLevel.B, Card.AttributeLevel.C),
         6F,    // defense (HP when placed)
         7F,    // attack
         5,     // speed
         null,  // listeners
         null,  // cost
         null   // effectId
);
```
说明：把这段代码直接放到 Card.java 的 static 块中合适位置（保持可读性），然后保存并编译。

示例 2：添加指令卡（COMMAND）——带花费与效果
```java
register("fire_strike", CardType.COMMAND,
         null,  // attributes
         null,  // defense
         null,  // attack
         null,  // speed
         null,  // listeners
         2,     // cost：消耗 2 点指挥点
         "damage:3" // effectId：对目标 publicCardId 造成 3 点伤害
);
```
注意：使用该卡时客户端需在 ActionData.targetPublicCardId 中提供目标格索引（targetPublicCardId），否则 EffectEngine 不会执行目标操作。

示例 3：添加特殊卡（SPECIAL）用于玩家 HQ
```java
register("player3", CardType.SPECIAL, null, 20F, null, null, null, null, null);
```

监听器（listeners）示例
----
若卡需要在事件触发时执行自定义逻辑，可提供 listeners 参数（List<Map.Entry<EventListener, Class<? extends GameEvent>>>）。EventBus 会在 register 时批量注册这些监听器。

示例（卡注册时注册一个 UseCardEvent 的监听器）：
```java
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import top.terry_mc.c6be.events.UseCardEvent;

List<Map.Entry<EventListener, Class<? extends GameEvent>>> listeners = new ArrayList<>();
listeners.add(new AbstractMap.SimpleEntry<>( (evt) -> {
    UseCardEvent e = (UseCardEvent) evt;
    // 自定义逻辑，例如：如果某张卡被使用则打印/产生 packet
    System.out.println("UseCardEvent: card used: " + e.getCard().getCardId());
}, UseCardEvent.class));

register("watcher_card", CardType.CHARACTER, getAttributes(...), 5F, 4F, 3, listeners, null, null);
```
说明：EventListener 是一个 Consumer<GameEvent>，在回调中你可以把 event 转型为具体事件类型并读取信息。

EffectId / EffectEngine 扩展
----
目前 EffectEngine（位于 src/main/java/top/terry_mc/c6be/service/EffectEngine.java）解析简单的 "key:amount" 字符串，例如：
- "damage:3" — 对目标格减 3 HP
- "heal:2" — 对目标格加 2 HP

如果你要新增效果类型（如范围伤害、临时 buff、召唤等），按以下步骤：
1. 在 EffectEngine 的 static registry 中添加新的 key 与处理函数，例如 "aoe:2"。
2. 处理函数签名为 BiFunction<EffectContext, Integer, List<Packet>>，其中 EffectContext 包含 room、player、targetPublicCardId。
3. 处理函数应修改对应 room 的 publicCards，并返回必要的 Packet（通常 PublicCardsUpdate，必要时 ActionBroadcast）。

示例（伪代码）：
```java
registry.put("aoe", (ctx, amount) -> {
    List<Packet> out = new ArrayList<>();
    if (ctx.targetPublicCardId == null) return out;
    int center = ctx.targetPublicCardId;
    // 计算周围格子索引（四方向或 3x3），对每格执行减血
    for (int nid : neighbors(center)) {
        var slot = ctx.room.getPublicCards().get(nid);
        if (slot.getCard() == null) continue;
        slot.setHp(slot.getHp() - amount);
        if (slot.getHp() <= 0) ctx.room.getPublicCards().set(nid, new PublicCard(null,0F,null,true,false));
    }
    out.add(new PublicCardsUpdate(ctx.room.getPublicCardAccesses()));
    return out;
});
```
不要忘记在 EffectEngine.applyEffectString 中解析你的 key（第 0 部分）并把 amount 解析为整数。

如何使新卡出现在牌堆（deck）中
----
GameLogicService 在开局使用 Card.getAllNonSpecialCards() 生成牌堆：

java.util.List<Card> deck = new ArrayList<>(Card.getAllNonSpecialCards());
Collections.shuffle(deck);

因此：
- 只要在 Card.java 中用 register 注册卡，且 type != SPECIAL，它就会自动成为牌堆的一部分，无需额外操作。

命名规范与注意事项
----
- cardId 应全小写并用下划线或短横分隔（已存在卡用下划线），避免空格或特殊字符。例："li_an"。
- 确保 cardId 唯一：重复注册将覆盖之前注册（因为 registeredCards 使用 ConcurrentHashMap）。
- CHARACTER 通常需提供 attributes、defense、attack、speed；COMMAND 通常只需 cost 和 effectId；SPECIAL 通常只需 defense（HP）。

添加卡后的校验与编译
----
1. 保存 Card.java 修改。
2. 在项目根目录运行：
```powershell
.\gradlew.bat build
```
3. 运行单元测试：
```powershell
.\gradlew.bat test
```
4. 启动服务并用两个客户端做端到端测试（JOIN -> 部署 -> 移动/指令使用）。

建议的额外工作流程（团队协作）
----
- 把卡数据维护在一个单独的 JSON/YAML 文件（例如 resources/cards.json），而不是直接硬编码到 Java。在代码启动时读取该文件并调用 register(...)。这便于设计师编辑卡池而不触碰 Java 源代码。
- 如果要我实现 JSON 加载器，可以按下述简单 schema：
  - cardId, type, attributes: [ "S","B","A",... ], defense, attack, speed, cost, effectId
- 我可以帮你实现一个 loader：读取 resources/cards.json 并在应用启动时注册所有卡。

示例 cards.json（建议 schema）
```json
[
  {
    "cardId": "li_an",
    "type": "CHARACTER",
    "attributes": ["S","B","A","S","S","B","B","A"],
    "defense": 8.0,
    "attack": 8.0,
    "speed": 6
  },
  {
    "cardId": "fire_strike",
    "type": "COMMAND",
    "cost": 2,
    "effectId": "damage:3"
  }
]
```
要实现 loader：在应用启动的某处（例如一个 @Component 的 @PostConstruct）解析该 JSON 并调用 register(...)。

常见问题（FAQ）
----
Q: 我注册了卡但游戏中没有出现它？
A: 请确认已重新编译并重启服务；确认 Card.register 成功执行（静态块运行在类加载时）。如果是 SPECIAL 类型则不会进入牌堆（仅 CHARACTER/COMMAND 会加入 deck）。

Q: effectId 没有生效？
A: 请确认 effectId 格式正确（例如 "damage:3"），并且调用 use 时在 ActionData 中传入 targetPublicCardId。查看服务端日志或在 EffectEngine 中添加调试打印。

Q: 我想在卡被使用/放置时触发额外逻辑，如何做？
A: 使用 listeners 参数注册 EventListener（参见上面 listeners 示例），在监听函数中处理事件。注意注册时要传入 Event 类型（如 UseCardEvent.class）。

需要我代劳？
----
- 我可以把现有的硬编码卡迁移到 `resources/cards.json` 并实现自动加载器；或把你给我的卡表（Excel/CSV/JSON）批量导入为 register(...) 调用并提交到代码库。告诉我你更偏好哪种方式。

结束语
----
本指南覆盖了从最简单的卡到带效果卡的添加流程、监听器、EffectEngine 扩展、JSON 迁移建议与测试步骤。若你把一批卡的表格（包括 cardId、type、8 门课等级、防/攻/速、cost、effectId）发给我，我可以把它们直接导入并提交一个 PR。
