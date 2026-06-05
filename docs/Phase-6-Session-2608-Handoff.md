# Phase 6 Session 2608 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2608: Send NPC shop invalid-goods denial live. See
[Phase-6-Session-2608-Completion.md](Phase-6-Session-2608-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- `8b85fa6` - `[Phase 6][UOW-2601] Send private-store seller kinah add live`
- `e5ab413` - `[Phase 6][UOW-2602] Order private-store seller messages live`
- `60a17a2` - `[Phase 6][UOW-2603] Interleave private-store buyer fanout live`
- `b3a0d9c` - `[Phase 6][UOW-2604] Persist private-store purchases live`
- `4fe0644` - `[Phase 6][UOW-2605] Log private-store sales live`
- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- Current commit - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` validates NPC goods before kinah/AP/free-slot/limit checks.
- Java review confirmed invalid goods sends `PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold from this NPC.")`.
- Java review confirmed `PacketSendUtility.sendMessage(Player, String)` uses `SM_MESSAGE` with sender `0`, no sender name,
  and `ChatType.GOLDEN_YELLOW`.
- C# live `CM_BUY_ITEM` action `13` now consumes `TradeBuyTransactionPlanStatus.BlockedInvalidBuyItem` and sends the
  Java text denial from the live path.
- The denial branch returns before item/kinah mutation, preserving player inventory state on blocked purchases.

## Files Changed In UOW-2608

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2608-Completion.md`
- `docs/Phase-6-Session-2608-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.services.TradeService#validateBuyItems`
- `com.aionemu.gameserver.utils.PacketSendUtility#sendMessage(Player, String)`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Network.Aion.ServerPackets.SmMessage`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 330/330.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/planner/packet coverage. The filtered command built `Aion.GameServer`
and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success, invalid-goods denial, and insufficient-kinah denial; AP/token, full-inventory, limited-item, persistence, and other branches remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#validateBuyItems` | `Aion.GameServer.Services.TradeBuyTransactionPlanService.CreatePlan` consumed by live handler | Service/planner/live application | Partial | Unit Tested | Partial Parity | Planner identifies invalid count or goods-list mismatch; live handler now sends the Java text denial for that status. |
| `com.aionemu.gameserver.utils.PacketSendUtility#sendMessage(Player, String)` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Packet helper | Partial | Unit Tested | Partial Parity | C# uses golden-yellow `SM_MESSAGE` for the invalid-goods denial; broader `sendMessage` usages remain individually scoped. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopInvalidGoodsSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` invalid-goods branch + `PacketSendUtility.sendMessage` | Ordinary no-observer action 13 packet sends the exact Java text through golden-yellow `SM_MESSAGE` and leaves kinah unchanged | Java source review + live C# handler assertion | Does not cover invalid count separately or real client display. |

## Known Gaps

- Action `13` live execution remains limited to `TradeNpcType.NORMAL`, kinah-only shop purchases.
- No buy-from-shop inventory/kinah persistence is wired yet.
- Limited-item counters are not updated.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy paths remain non-live.
- Full-inventory, AP/token, required-item, negative AP audit, and limited-item denial branches are not live yet.
- Existing buy-from-shop diagnostic outcome plans still report disabled side effects for branches not consumed by the
  live handler.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Generic Status Dispatcher

- A broad status-to-packet dispatcher would have touched several unverified denial branches in one UOW.
- This UOW kept to the single Java branch whose text packet and planner status were already scoped.

### Invalid Count Separate From Invalid Goods

- Java uses the same denial branch for invalid item count and disallowed goods.
- The live handler consumes the shared `BlockedInvalidBuyItem` status; separate count coverage can be added if a later
  runtime bug appears, but the selected UOW focused on the client-visible packet branch.

## Next Recommended Runtime UOW

**UOW-2609 candidate: send the live full-inventory denial packet for NPC action `13` normal shop buys.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: full-inventory NPC shop buys should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> if freeSlots < tradeList.size() -> SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedInventoryFull and SmSystemMessage.FullInventory().
- Client-visible/state/persistence effect expected: live action 13 with insufficient free cube slots sends a real SM_SYSTEM_MESSAGE and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```

Suggested focused validation if this UOW includes the denial packet:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet side effect
changed; start focused and document any broad skip.

## Safe Runtime Candidates

- Live full-inventory denial packet for action `13` normal shop buys.
- Live limited-item denial packet for action `13`, after scoping Java limited purchase counters.
- Live AP/required-item denial packets for action `13`, after AP and required-item mutation/packet behavior is scoped.
- Buy-from-shop persistence only if an existing repository surface can be reused or a minimal Java-shaped inventory store
  contract is implemented and consumed by the live handler in the same UOW.
- `ABYSS_KINAH`, `ABYSS`, or `REWARD` buy execution only after AP/required-item mutation and packet effects are scoped
  from Java source.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` private-store purchase behavior has been progressively wired through UOW-2596 to
  UOW-2605, including packet order, persistence, and logging.
- `CM_BUY_ITEM` NPC action `13` normal kinah stackable buy executes live as of UOW-2606.
- `CM_BUY_ITEM` NPC action `13` normal kinah insufficient-funds denial sends live as of UOW-2607.
- `CM_BUY_ITEM` NPC action `13` normal invalid-goods denial sends live as of UOW-2608.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
