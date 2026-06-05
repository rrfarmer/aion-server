# Phase 6 Session 2607 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2607: Send NPC shop kinah denial live. See
[Phase-6-Session-2607-Completion.md](Phase-6-Session-2607-Completion.md).

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
- Current commit - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` sends
  `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY()` when a kinah shop buy cannot afford the calculated price.
- Java review confirmed `STR_MSG_NOT_ENOUGH_MONEY` is message id `1300759`, distinct from the existing C#
  `NotEnoughMoney()` helper id `1300388`.
- C# live `CM_BUY_ITEM` action `13` now consumes `TradeBuyTransactionPlanStatus.BlockedNotEnoughKinah` and sends the
  Java denial packet from the live path.
- The denial branch returns before item/kinah mutation, preserving player inventory state on blocked purchases.

## Files Changed In UOW-2607

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2607-Completion.md`
- `docs/Phase-6-Session-2607-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService#performBuyFromShop`
- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.model.trade.TradeList#calculateBuyListPrice`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_NOT_ENOUGH_MONEY`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 329/329.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/planner/packet coverage. The filtered command built `Aion.GameServer`
and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success and insufficient-kinah denial; other denial and AP/token branches remain incomplete. |
| `com.aionemu.gameserver.model.trade.TradeList#calculateBuyListPrice` | `Aion.GameServer.Services.TradeBuyTransactionPlanService.CreatePlan` | Planner consumed by live handler | Partial | Unit Tested | Partial Parity | Supplies `BlockedNotEnoughKinah` to the live handler from Java-derived kinah cost checks. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_NOT_ENOUGH_MONEY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.MsgNotEnoughMoney` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300759` is pinned; no full Java packet byte golden was run. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopInsufficientKinahSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` insufficient-kinah branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_MONEY` and leaves kinah unchanged | Java source review + live C# handler assertion | Does not cover other denial branches or real client display. |
| `GamePacketTests` system-message assertion | Unit/packet | `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY` | Pins C# helper to Java message id `1300759` | Java source review + C# packet assertion | Not a full byte-for-byte Java golden. |

## Known Gaps

- Action `13` live execution remains limited to `TradeNpcType.NORMAL`, kinah-only shop purchases.
- No buy-from-shop inventory/kinah persistence is wired yet.
- Limited-item counters are not updated.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy paths remain non-live.
- Invalid-goods, full-inventory, AP/token, required-item, and limited-item denial branches are not live yet.
- Existing buy-from-shop diagnostic outcome plans still report disabled side effects for branches not consumed by the
  live handler.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Generic Denial Dispatcher

- A broad status-to-packet dispatcher would have touched several unverified denial branches in one UOW.
- This UOW kept to the single Java branch whose packet id and planner status were already scoped.

### Persistence Follow-Up

- Persistence remains important, but it would not have advanced the current Java denial packet gap.
- No existing minimal inventory persistence surface was discovered during this UOW.

## Next Recommended Runtime UOW

**UOW-2608 candidate: send the live invalid-goods denial message for NPC action `13` normal shop buys.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: invalid NPC shop goods should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> validateBuyItems false -> PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold from this NPC.") and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync or adjacent action-13 denial helper using TradeBuyTransactionPlanStatus.BlockedInvalidBuyItem.
- Client-visible/state/persistence effect expected: live action 13 with an item not sold by the NPC sends a real server message packet and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```

Suggested focused validation if this UOW includes the denial packet:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet side effect
changed; start focused and document any broad skip.

## Safe Runtime Candidates

- Live invalid-goods denial message for action `13` normal shop buys.
- Live full-inventory denial packet for action `13`, after confirming the exact Java packet/message emitted by
  `ItemService.addItem` or the pre-add inventory check path.
- Live limited-item denial packet for action `13`, after scoping Java limited purchase counters.
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
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
