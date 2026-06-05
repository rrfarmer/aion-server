# Phase 6 Session 2610 Completion

## UOW

[Phase 6] UOW-2610: Send NPC shop limited-item denial live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` for normal NPC shop buys now sends Java `SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS`
(`1400353`) when the limited-item counter blocks the requested purchase, and leaves inventory state unchanged.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: limited-item NPC shop buys now send the Java denial packet from live CM_BUY_ITEM instead of only composing a disabled planner result.
- Java source method or runtime path: TradeService.performBuyTransaction -> !canBuyLimitItem -> SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS and return false.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedLimitedItem and SmSystemMessage.LimitedBuyingCantSelectNoItems().
- Client-visible/state/persistence effect changed: live action 13 blocked by the limited-item counter sends a real SM_SYSTEM_MESSAGE denial and does not mutate player inventory.
- Why this is not preview-only/test-only/documentation-only: this sends a real server packet from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyTransaction` checks limited-item availability after invalid-goods, kinah/AP/required-item,
  negative-required-AP, and free-slot checks.
- `TradeService.canBuyLimitItem` rejects over-limit NPC limited purchases before any cost subtraction or item add.
- On limited-item block, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS()` and returns
  `false`.
- `SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS` is message id `1400353`.

## C# Changes

- Added `SmSystemMessage.LimitedBuyingCantSelectNoItems()` for Java message id `1400353`.
- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so `BlockedLimitedItem` sends the Java limited-buy
  denial packet before the success-only mutation branch.
- Updated the existing diagnostic-plan test to reflect that the live handler now sends the denial packet even while
  outcome/persistence remain disabled.
- Added live no-observer action `13` coverage for limited-item denial that asserts no kinah spend and the Java packet.

## Known Gaps

- AP/required-item shortage and negative AP audit denial packets remain incomplete.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Limited-item counter mutation after successful buys remains disabled.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 limited-item denial packet plus system-message helper.
- Specific behavior/contract: limited-item over-buy sends STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS (1400353), emits no inventory mutation, and keeps the no-observer live path active.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 332/332 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet side effect changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer test dispatches an encoded CM_BUY_ITEM packet through the ordinary live path and asserts the Java limited-item message plus unchanged inventory state; GamePacketTests pins the message id.
```

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

## Summary Metrics

- Focused UOW validation: 332 tests passed.
- Runtime progress: live NPC action `13` normal shop buy now sends the limited-item denial packet.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: AP/required-item denial packets, buy persistence, AP/token shop purchases, limited-item counter updates, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Limited-item facts currently come from the C# fact adapter and default player buy counts; successful live counter
  mutation/persistence is still missing.
- The packet id is pinned from Java source review and C# packet tests, not a new Java byte-for-byte golden.
- Existing nullable/analyzer warnings appeared during focused validation; they are pre-existing and outside this UOW's
  changed lines.

## Next Runtime Candidate

UOW-2611 candidate: send the live not-enough-abyss-points denial packet for NPC action `13` buy transaction failures.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: AP/required-item/negative-AP NPC shop buy failures should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> !tradeList.calculateAbyssRewardBuyList or tradeList.getRequiredAp() < 0 -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedNotEnoughAbyssPoints, BlockedNotEnoughRequiredItems, and AuditNegativeRequiredAp with a SmSystemMessage helper for message id 1300927.
- Client-visible/state/persistence effect expected: live action 13 blocked by AP/required-item shortage or negative AP audit sends a real SM_SYSTEM_MESSAGE and does not mutate inventory/AP.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```
