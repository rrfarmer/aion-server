# Phase 6 Session 2611 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2611: Send NPC shop abyss-point denial live. See
[Phase-6-Session-2611-Completion.md](Phase-6-Session-2611-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- Current commit - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` sends
  `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT()` when `calculateAbyssRewardBuyList` fails.
- Java review confirmed negative `tradeList.getRequiredAp()` audits the player, sends the same not-enough-AP packet,
  and returns `false`.
- Java review confirmed `STR_MSG_NOT_ENOUGH_ABYSSPOINT` is message id `1300927`.
- C# live `CM_BUY_ITEM` action `13` now consumes `BlockedNotEnoughAbyssPoints`,
  `BlockedNotEnoughRequiredItems`, and `AuditNegativeRequiredAp` and sends the Java denial packet.
- The denial branch returns before item/kinah/AP/required-item mutation.

## Files Changed In UOW-2611

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2611-Completion.md`
- `docs/Phase-6-Session-2611-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.model.trade.TradeList#calculateAbyssRewardBuyList`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_NOT_ENOUGH_ABYSSPOINT`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Services.TradeBuyTransactionPlanService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 335/335.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/planner/packet coverage. The filtered command built `Aion.GameServer`
and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success and invalid-goods, insufficient-kinah, AP/required-item/negative-AP, full-inventory, and limited-item denials; AP/token successes, persistence, audit logging, and some branches remain incomplete. |
| `com.aionemu.gameserver.model.trade.TradeList#calculateAbyssRewardBuyList` | `Aion.GameServer.Services.TradeBuyTransactionPlanService.CreatePlan` | Service/planner | Partial | Unit Tested | Partial Parity | Planner detects AP and required-item shortages; live handler now consumes those blocked statuses for the packet effect. Successful AP/required-item mutation remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_NOT_ENOUGH_ABYSSPOINT` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.MsgNotEnoughAbyssPoints` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300927` is pinned; no full Java packet byte golden was run this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopNotEnoughAbyssPointsSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` AP shortage branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves kinah/AP unchanged when AP is insufficient | Java source review + live C# handler assertion | Does not execute successful AP-cost purchase. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopMissingRequiredItemSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` required-item shortage branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves inventory unchanged when required items are missing | Java source review + live C# handler assertion | Does not decrease required items on success. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopNegativeRequiredApSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` negative required-AP audit branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves kinah/AP unchanged for negative AP metadata | Java source review + live C# handler assertion | Live audit logging remains disabled. |
| `GamePacketTests` system-message assertion | Unit/packet | `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT` | Pins C# helper to Java message id `1300927` | Java source review + C# packet assertion | Not a full byte-for-byte Java golden. |

## Known Gaps

- Successful AP/required-item NPC shop purchases remain disabled by the success-only live guard.
- Negative required-AP audit logging remains planner metadata only; the live packet denial is wired.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Successful limited-item counter updates are not live or persisted.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only successful purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Next Recommended Runtime UOW

**UOW-2612 candidate: persist successful normal NPC shop kinah buys through an existing C# repository boundary.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop kinah buys currently mutate live in-memory inventory/kinah and send packets, but item/kinah state is not persisted.
- Java source method or runtime path: TradeService.performBuyTransaction -> player.getInventory().decreaseKinah(...) and ItemService.addItem(...) with Java inventory persistence side effects.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus the existing player inventory persistence/repository surface.
- Client-visible/state/persistence effect expected: successful action 13 normal kinah shop buys persist bought item rows and kinah decrease using the existing database shape.
- Why this is not preview-only/test-only/documentation-only if feasible: it persists/restores live inventory state rather than only reporting planner readiness.
```

Suggested focused validation if this UOW wires persistence:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldRepositoryTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: persistence boundary and
live state side effect changed; start focused and document any broad skip or escalation after the focused result.

## Safe Runtime Candidates

- Persist successful normal NPC shop kinah buys if an existing repository save surface can be reused safely.
- Execute successful AP/required-item buy branches only after AP and required-item mutation ordering is scoped from Java.
- Successful limited-item counter updates only if live mutation and persistence are scoped from Java
  `LimitedItemTradeService`.
- `ABYSS_KINAH`, `ABYSS`, or `REWARD` buy execution only after AP/required-item mutation and packet effects are scoped
  from Java source.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action `13` normal kinah stackable buy executes live as of UOW-2606.
- `CM_BUY_ITEM` NPC action `13` normal insufficient-funds denial sends live as of UOW-2607.
- `CM_BUY_ITEM` NPC action `13` normal invalid-goods denial sends live as of UOW-2608.
- `CM_BUY_ITEM` NPC action `13` normal full-inventory denial sends live as of UOW-2609.
- `CM_BUY_ITEM` NPC action `13` normal limited-item denial sends live as of UOW-2610.
- `CM_BUY_ITEM` NPC action `13` normal AP/required-item/negative required-AP denial sends live as of UOW-2611.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
