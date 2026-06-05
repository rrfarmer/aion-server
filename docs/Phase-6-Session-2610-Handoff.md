# Phase 6 Session 2610 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2610: Send NPC shop limited-item denial live. See
[Phase-6-Session-2610-Completion.md](Phase-6-Session-2610-Completion.md).

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
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- Current commit - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` checks `!canBuyLimitItem` after free-slot checks and
  before any cost subtraction or item add.
- Java review confirmed limited-item purchase blocks send
  `SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS()` and return `false`.
- Java review confirmed `STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS` is message id `1400353`.
- C# live `CM_BUY_ITEM` action `13` now consumes `TradeBuyTransactionPlanStatus.BlockedLimitedItem` and sends the
  Java limited-item denial packet from the live path.
- The denial branch returns before item/kinah mutation, preserving player inventory state on blocked purchases.

## Files Changed In UOW-2610

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2610-Completion.md`
- `docs/Phase-6-Session-2610-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.services.TradeService#canBuyLimitItem`
- `com.aionemu.gameserver.services.LimitedItemTradeService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CanBuyLimitedItem`
- `Aion.GameServer.Services.TradeBuyTransactionPlanService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 332/332.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/planner/packet coverage. The filtered command built `Aion.GameServer`
and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success and invalid-goods, insufficient-kinah, full-inventory, and limited-item denials; AP/token, persistence, and some branches remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#canBuyLimitItem` | `Aion.GameServer.Network.Aion.GameServerConnection.CanBuyLimitedItem` consumed by `TradeBuyTransactionPlanService` | Limited-item guard | Partial | Unit Tested | Partial Parity | C# planner rejects over-limit limited items and live handler sends the Java denial; successful limited counter updates remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LimitedBuyingCantSelectNoItems` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1400353` is pinned; no full Java packet byte golden was run this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopLimitedItemSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` limited-item branch | Ordinary no-observer action 13 packet sends `STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS` and leaves kinah unchanged when over the buy limit | Java source review + live C# handler assertion | Does not update successful limited-item counters. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopLimitedItemPlanRecordsDisabledOutcomeAndSendsLiveDenial` | Unit/live handler/diagnostic | Same Java branch | Confirms diagnostic outcome remains non-mutating while the live packet branch now sends the denial | Java source review + C# handler assertion | Diagnostic outcome still reports disabled persistence/side effects. |
| `GamePacketTests` system-message assertion | Unit/packet | `SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS` | Pins C# helper to Java message id `1400353` | Java source review + C# packet assertion | Not a full byte-for-byte Java golden. |

## Known Gaps

- Action `13` live execution remains limited to `TradeNpcType.NORMAL`, kinah-only shop purchases.
- No buy-from-shop inventory/kinah persistence is wired yet.
- Successful limited-item counter updates are not live or persisted.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy paths remain non-live.
- AP/token, required-item, and negative AP audit denial branches are not live yet.
- Existing buy-from-shop diagnostic outcome plans still report disabled side effects for persistence and remaining
  unconsumed branches.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### AP/Required-Item Denial First

- Java orders limited-item checks after free-slot checks but before cost subtraction.
- The selected UOW finished the remaining scoped normal-shop denial branch before moving into AP/required-item handling.

### Limited-Item Counter Persistence

- Updating successful limited-item counters would require live mutation/persistence beyond the denial branch.
- This UOW only sent the Java denial packet and documented counter updates as a remaining runtime gap.

## Next Recommended Runtime UOW

**UOW-2611 candidate: send the live not-enough-abyss-points denial packet for NPC action `13` buy transaction failures.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: AP/required-item/negative-AP NPC shop buy failures should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> !tradeList.calculateAbyssRewardBuyList or tradeList.getRequiredAp() < 0 -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedNotEnoughAbyssPoints, BlockedNotEnoughRequiredItems, and AuditNegativeRequiredAp with a SmSystemMessage helper for message id 1300927.
- Client-visible/state/persistence effect expected: live action 13 blocked by AP/required-item shortage or negative AP audit sends a real SM_SYSTEM_MESSAGE and does not mutate inventory/AP.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```

Suggested focused validation if this UOW includes the denial packet:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet side effect
changed; start focused and document any broad skip.

## Safe Runtime Candidates

- Live AP/required-item/negative-AP denial packet for action `13` buy transaction failures.
- Buy-from-shop persistence only if an existing repository surface can be reused or a minimal Java-shaped inventory store
  contract is implemented and consumed by the live handler in the same UOW.
- Successful limited-item counter updates only if live mutation and persistence are scoped from Java
  `LimitedItemTradeService`.
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
- `CM_BUY_ITEM` NPC action `13` normal full-inventory denial sends live as of UOW-2609.
- `CM_BUY_ITEM` NPC action `13` normal limited-item denial sends live as of UOW-2610.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
