# Phase 6 Session 2609 Completion

## UOW

[Phase 6] UOW-2609: Send NPC shop full-inventory denial live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` for normal NPC shop buys now sends Java `SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY` (`1300762`) when the player's
normal cube has fewer free slots than the requested trade-list item count, and leaves inventory state unchanged.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: full-inventory NPC shop buys now send the Java denial packet from live CM_BUY_ITEM instead of only composing a disabled planner result.
- Java source method or runtime path: TradeService.performBuyTransaction -> if freeSlots < tradeList.size() -> SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY and return false.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedInventoryFull and SmSystemMessage.FullInventory().
- Client-visible/state/persistence effect changed: live action 13 with insufficient free cube slots sends a real SM_SYSTEM_MESSAGE denial and does not mutate player inventory.
- Why this is not preview-only/test-only/documentation-only: this sends a real server packet from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyTransaction` snapshots `player.getInventory().getFreeSlots()` before rate/cost checks.
- After kinah, AP, required-item, and negative-required-AP checks, Java evaluates `freeSlots < tradeList.size()`.
- On full inventory, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY()` and returns `false`.
- `SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY` is message id `1300762`.

## C# Changes

- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so `BlockedInventoryFull` sends
  `SmSystemMessage.FullInventory()` before the success-only mutation branch.
- Added live no-observer action `13` coverage for a full normal cube that asserts no bought item, no kinah spend, and
  the Java full-inventory packet.

## Known Gaps

- Other buy-from-shop denial branches remain incomplete: AP/required-item shortages, negative AP audit, and limited-item
  blocks.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 full-inventory denial packet.
- Specific behavior/contract: full inventory sends STR_MSG_FULL_INVENTORY (1300762), emits no inventory mutation, and keeps the no-observer live path active.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 331/331 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet side effect changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer test dispatches an encoded CM_BUY_ITEM packet through the ordinary live path and asserts the Java full-inventory message plus unchanged inventory state; GamePacketTests already pins the message id.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success, invalid-goods, insufficient-kinah, and full-inventory denials; AP/token, limited-item, persistence, and other branches remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage#getFreeSlots` / `ItemStorage#getFreeSlots` | `Aion.GameServer.Services.InventoryCapacity.GetFreeCubeSlots` consumed by `TradeBuyTransactionPlanService` | Inventory capacity helper | Partial | Unit Tested | Partial Parity | C# planner uses Java-shaped normal cube slot count; broader storage behavior and special cube nuances remain separately scoped. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_FULL_INVENTORY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.FullInventory` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300762` is pinned; no full Java packet byte golden was run this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopFullInventorySendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` full-inventory branch | Ordinary no-observer action 13 packet sends `STR_MSG_FULL_INVENTORY` and leaves kinah/items unchanged when normal cube slots are exhausted | Java source review + live C# handler assertion | Does not cover stack-merge nuance where Java pre-check still compares free slots to trade-list size. |

## Summary Metrics

- Focused UOW validation: 331 tests passed.
- Runtime progress: live NPC action `13` normal shop buy now sends the full-inventory denial packet.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: remaining buy denial packets, buy persistence, AP/token shop purchases, limited-item counters, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live branch depends on the C# planner's free-slot snapshot matching Java storage behavior for all item/storage
  classifications.
- Java checks `freeSlots < tradeList.size()` before item-add overflow is allowed; the C# live test covers the blocked
  new-item case but not all stack-merge edge cases.
- The packet id is pinned from existing C# packet tests and Java source review, not a new Java byte-for-byte golden.
- Existing nullable/analyzer warnings appeared during focused validation; they are pre-existing and outside this UOW's
  changed lines.

## Next Runtime Candidate

UOW-2610 candidate: send the live limited-item denial packet for NPC action `13` normal shop buys.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: limited-item NPC shop buys should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> !canBuyLimitItem -> SM_SYSTEM_MESSAGE.STR_MSG_LIMITED_BUYING_CANT_SELECT_NO_ITEMS and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedLimitedItem and a SmSystemMessage helper for message id 1400353.
- Client-visible/state/persistence effect expected: live action 13 blocked by the limited-item counter sends a real SM_SYSTEM_MESSAGE and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```
