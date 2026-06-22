Class6Kards 后端 WebSocket API 文档（中文）

概述
----
本服务使用 WebSocket（Spring STOMP）传输实时游戏消息。客户端将消息发送到 /app/* 的映射（见下文），服务器通过 /topic/room/{roomId} 和 /user/{playerId}/queue/hand 推送消息给订阅者或指定用户。

连接与订阅
----
- 建立 WebSocket/STOMP 连接到服务器（例如 ws://{host}:{port}/ws）。
- 客户端应订阅以下频道：
  - /topic/room/{roomId}/start — 游戏开始广播（StartBroadcast）
  - /topic/room/{roomId}/public — 公示区（25 格棋盘）更新（PublicCardsUpdate）
  - /topic/room/{roomId}/action — 动作动画广播（ActionBroadcast）
  - /topic/room/{roomId}/end — 游戏结束广播（EndBroadcast）
  - /topic/room/{roomId}/turn — 回合结束/切换广播（FinishTurnBroadcast）
  - /topic/room/{roomId}/state — 回合/节信息（RoundUpdate）
- 客户端应为私有推送订阅队列： /user/queue/hand，用于接收私人手牌更新（PrivateHandUpdate）。

客户端 -> 服务器（消息发送到 /app/*）
----
公共请求类型由 `ClientPayload` 表示：
{
  "type": "JOIN|USE|MOVE|FINISH",
  "roomId": "room1",
  "playerId": "playerA",
  "data": ...
}

1) 加入房间（JOIN）
- 路径：/app/join
- data: String（玩家名字）
- 示例：
  {"type":"JOIN","roomId":"r1","playerId":"p1","data":"Alice"}
- 服务器响应（通过多个 packet 推送）：
  - JoinBroadcast -> /topic/room/{roomId}/join
  - StartBroadcast（当两名玩家都已加入） -> /topic/room/{roomId}/start
  - PrivateHandUpdate -> /user/{playerId}/queue/hand (每位玩家)
  - PublicCardsUpdate -> /topic/room/{roomId}/public
  - RoundUpdate -> /topic/room/{roomId}/state

2) 使用卡牌（USE）
- 路径：/app/use
- data: ActionBroadcast.ActionData（见下）
- 语义：当玩家想要使用手牌中的一张卡（部署角色或使用指令卡）时发送。
- ActionData 字段说明：
  - card: CardAccess（必填） — 来自手牌的卡的访问数据（至少包含 cardId）
  - publicCardId: Integer|null — 
    - 当部署角色时（CHARACTER）表示目标格索引（0..24）；
    - 当操作涉及 public 区（例如作用于某个公开格）时使用；
    - 否则为 null（例如只是弃牌）。
  - targetPublicCardId: Integer|null — 指令/效果目标的公开格索引（若 effect 需要目标）。
  - path: List<Integer> — 在 USE 中通常为空。
- 示例（部署一张角色卡到格子 1）：
  {"type":"USE","roomId":"r2","playerId":"p1","data": {"card":{"cardId":"li_an"},"publicCardId":1,"targetPublicCardId":null,"path":null}}
- 服务器会处理花费（COMMAND）/部署规则并下发：
  - ActionBroadcast -> /topic/room/{roomId}/action
  - PublicCardsUpdate -> /topic/room/{roomId}/public（若 public 发生变化）
  - PrivateHandUpdate -> /user/{playerId}/queue/hand

3) 移动/攻击（MOVE）
- 路径：/app/move
- data: ActionBroadcast.ActionData
- 语义：玩家选择场上某个已部署角色进行移动或对目标攻击。可以传入 `path`（逐格索引列表）或直接指定 `targetPublicCardId`。
- rules（后端校验逻辑）：
  - `publicCardId` 为源格索引（src）；必须存在卡且属于该玩家（ownerPlayerPublicId）。
  - 若传入 `path`：
    - `path[0]` 必须等于 src；连续的格子序列会被逐格校验；若路径中出现友方角色则路径非法；遇到敌方角色则触发攻击并在攻击前一格停止移动；若路径合法且目标为空则移动到目标格。
  - 若未传入 path 而给出 `targetPublicCardId`：使用曼哈顿距离校验（|dr|+|dc|）是否在速度允许范围内。
  - 攻击：攻击者造成其当前攻击值（currAttack）伤害到目标的 hp；若目标仍存活且为 CHARACTER，则立刻反击一次（使用目标的 currAttack）；死亡（hp <= 0）则移除目标格。
- 返回：
  - ActionBroadcast -> /topic/room/{roomId}/action
  - PublicCardsUpdate -> /topic/room/{roomId}/public
  - 若触发游戏结束 -> EndBroadcast -> /topic/room/{roomId}/end
- 示例（逐格移动从 1 到 6）：
  {"type":"MOVE","roomId":"r2","playerId":"p1","data": {"card":{"cardId":"li_an"},"publicCardId":1,"targetPublicCardId":6,"path":[1,6]}}

4) 结束回合（FINISH）
- 路径：/app/finish
- data: null
- 语义：当前回合玩家结束其行动阶段，服务端会切换到对方回合并触发回合开始逻辑（指挥点上限+1、补满、双方各摸一张、每个格子重置行动状态）。
- 返回：
  - FinishTurnBroadcast -> /topic/room/{roomId}/turn
  - RoundUpdate -> /topic/room/{roomId}/state
  - PrivateHandUpdate -> /user/{playerId}/queue/hand（每位玩家）

服务器 -> 客户端（Packet 类型说明）
----
所有服务器推送消息均为实现了 `Packet` 接口的对象。关键 Packet 类型说明：

1) StartBroadcast
- 含义：游戏开始并包含课程表顺序（8 节课）
- 结构（Java record）： StartBroadcast(List<Subject> classes)
- 频道： /topic/room/{roomId}/start

2) PublicCardsUpdate
- 含义：公示区（5x5 棋盘）状态更新
- 结构： PublicCardsUpdate(List<Map.Entry<CardAccess, Float>> publicCards)
  - 列表长度固定为 25，按行主序（index 0..24）排列。
  - 每个 entry.key（CardAccess）可能为 null（代表该格为空或是特殊情况）；entry.value 为该格当前 HP（Float），special 玩家 HQ 卡的 HP 也放这里。
- 频道： /topic/room/{roomId}/public
- 说明：客户端收到后应把其渲染到 5x5 网格中，index->(row=index/5, col=index%5)

3) ActionBroadcast
- 含义：播放动作动画（玩家使用卡、移动、攻击等）由所有客户端可见
- 结构： ActionBroadcast(PlayerAccess player, String actionType, ActionData data)
  - actionType: "USE" 或 "MOVE"
  - ActionData: { card: CardAccess, publicCardId: Integer|null, targetPublicCardId: Integer|null, path: List<Integer>|null }
- 频道： /topic/room/{roomId}/action

4) PrivateHandUpdate
- 含义：私有手牌更新，仅发送给指定用户的 /user/{playerId}/queue/hand
- 结构： PrivateHandUpdate(PlayerAccess player, List<CardAccess> hand)
- 频道： /user/{playerId}/queue/hand

5) RoundUpdate
- 含义：回合/节信息（当前 roundCnt, classCnt）
- 结构： RoundUpdate(int roundCnt, int classCnt)
- 频道： /topic/room/{roomId}/state

6) FinishTurnBroadcast
- 含义：通知回合结束与下家开始回合
- 频道： /topic/room/{roomId}/turn

7) EndBroadcast
- 含义：游戏结束，包含胜者（PlayerAccess）或 null（平局）
- 频道： /topic/room/{roomId}/end

数据模型（重要类型说明）
----
1) CardAccess
- 结构（简化）： { cardId: String, cardType: "CHARACTER"|"COMMAND"|"SPECIAL", attributes: {...} }
- 注意：CardAccess 并不包含当前 HP/状态信息，仅用于标识卡。

2) PlayerAccess
- 结构： { playerId: String, name: String, publicCardId: Integer }

3) PublicCard（后端模型，客户端可参考）
- 每个格子后端维护字段：
  - card: Card|null
  - hp: Float
  - ownerPlayerPublicId: Integer|null
  - canAct: boolean
  - hasMoved: boolean
  - currAttack/currDefense/currSpeed: 当前受课程影响后的数值
  - remainingMove: 本回合剩余移动点
- 客户端通过 PublicCardsUpdate 获得卡 id 与 HP（CardAccess, Float），若需要更多字段，可通过额外的 API 扩展。

游戏规则关键点（后端实现摘要）
----
- 棋盘：5x5，共 25 个格子，按行主序 index 0..24；HQ 放置在 index 2（先手）和 22（后手）。
- 开局：双方各摸 4 张初始手牌；游戏开始时按随机课程表开始第 1 节课；回合开始时双方各摸 1 张并指挥点上限+1且补满。
- 部署：玩家可在靠近自己 HQ 的两行（先手 rows 0-1，后手 rows 3-4）部署角色卡（CHARACTER）。
- 移动：移动消耗速度点；可以传入逐格路径（path）或直接指定目标（使用曼哈顿距���校验）；遇敌停止并攻击。
- 攻击/反击：攻击造成攻击者 currAttack 的伤害，若目标存活且为 CHARACTER 则进行一次反击（不触发二次反击）。
- 指令卡（COMMAND）：包含 cost 和 effectId；使用时会消耗指挥点并由效果引擎（EffectEngine）解析执行（支持 "damage:N"、"heal:N" 等简单效果）。
- 胜负：当某一方 HQ（其对应的特殊卡格）HP <= 0 时，游戏结束；若双方均 <=0 则平局；若 8 节课全部结束且未分出胜负则平局或比较 HP 判胜（实现依赖）。

示例完整流程（客户端与服务器交互）
----
1) 两位玩家分别发送 JOIN，订阅频道。
2) 服务器在两人到齐后发送 StartBroadcast、PublicCardsUpdate（包含 HQ）、并给双方下发 PrivateHandUpdate（初始牌）。
3) 回合开始（先手）：服务器触发 newTurn -> 双方各摸 1 张并得到 PrivateHandUpdate；先手收到可以行动的 UI。
4) 先手使用 USE 部署一张角色卡 -> 服务端校验并下发 ActionBroadcast + PublicCardsUpdate + PrivateHandUpdate。
5) 先手发 MOVE 对该角色进行移动/攻击 -> 服务端校验路径/速度并下发 ActionBroadcast + PublicCardsUpdate（并可能触发 EndBroadcast）。

常见错误码/调试建议
----
- 服务端不会返回 HTTP 风格的错误码；任何非法操作一般会被忽略并不会改变游戏状态。调试时可在服务端日志中查看 DEBUG 打印。
- 确保客户端在发送私有消息时使用正确的 playerId 与 roomId。

后续扩展建议
----
- 将 PublicCardsUpdate 扩展为包含更多格子字段，方便前端渲染（如 currAttack/currDefense/currSpeed、remainingMove 等）。
- 指令效果用 JSON 或脚本化描述替换简单字符串，以支持更复杂的效果链与目标选择规则。
- 增加 REST API 用于获取房间初始数据、回放记录等。

---
文档结束。如需我把文档转为 HTML 或在 README 中嵌入示例代码（前端 JS + STOMP 客户端示例），我可以继续生成。
