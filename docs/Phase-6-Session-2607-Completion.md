# Phase 6 Session 2607 Completion

## UOW

[Phase 6] UOW-2607: Send NPC shop kinah denial live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` for normal NPC shop buys now sends Java `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY` (`1300759`) when the
player lacks required kinah, and leaves inventory state unchanged.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: insufficient-kinah NPC shop buys now send the Java denial packet from live CM_BUY_ITEM instead of only composing a disabled planner result.
- Java source method or runtime path: TradeService.performBuyTransaction -> if useKinah && !tradeList.calculateBuyListPrice -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY and return false.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus SmSystemMessage.MsgNotEnoughMoney.
- Client-visible/state/persistence effect changed: live action 13 with insufficient kinah sends a real SM_SYSTEM_MESSAGE denial and does not mutate player inventory.
- Why this is not preview-only/test-only/documentation-only: this sends a real server packet from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyFromShop` routes `TradeNpcType.NORMAL` and `ABYSS_KINAH` through
  `performBuyTransaction(..., true)`.
- `TradeService.performBuyTransaction` sends `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY()` and returns `false` when
  `useKinah` is true and `TradeList.calculateBuyListPrice` fails.
- `TradeList.calculateBuyListPrice` returns `false` when required kinah exceeds the player's inventory kinah.
- `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY` is message id `1300759`, distinct from `STR_NOT_ENOUGH_MONEY`
  (`1300388`).

## C# Changes

- Added `SmSystemMessage.MsgNotEnoughMoney()` for Java `STR_MSG_NOT_ENOUGH_MONEY` (`1300759`).
- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so a `BlockedNotEnoughKinah` transaction plan sends
  the live denial packet before the success-only mutation guard.
- Added live no-observer action `13` coverage for insufficient kinah that asserts no mutation and the denial packet.
- Pinned the new system-message helper in `GamePacketTests`.

## Known Gaps

- Other buy-from-shop denial branches remain incomplete: invalid goods, AP/required-item shortages, full inventory,
  limited-item blocks, and audit-only negative AP cases.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 denial packet plus system-message helper.
- Specific behavior/contract: insufficient kinah sends STR_MSG_NOT_ENOUGH_MONEY (1300759), emits no inventory mutation, and keeps the no-observer live path active.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 329/329 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet side effect changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer test dispatches an encoded CM_BUY_ITEM packet through the ordinary live path and asserts the Java denial packet plus unchanged inventory state; GamePacketTests pins the message id.
```

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

## Summary Metrics

- Focused UOW validation: 329 tests passed.
- Runtime progress: live NPC action `13` normal kinah buy now sends the insufficient-kinah denial packet.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: other buy denial packets, buy persistence, AP/token shop purchases, limited-item counters, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live branch still depends on the C# planner's derived cost/status result matching Java for all edge cases.
- The denial packet id is pinned from Java source review but not from a byte-for-byte Java golden comparison.
- If later live branches reorder validation before the kinah check, this denial path must be rechecked against Java order.

## Next Runtime Candidate

UOW-2608 candidate: send the live invalid-goods denial message for NPC action `13` normal shop buys.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: invalid NPC shop goods should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> validateBuyItems false -> PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold from this NPC.") and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync or adjacent action-13 denial helper using TradeBuyTransactionPlanStatus.BlockedInvalidBuyItem.
- Client-visible/state/persistence effect expected: live action 13 with an item not sold by the NPC sends a real server message packet and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```
