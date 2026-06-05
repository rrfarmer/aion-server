# Phase 6 Session 2606 Completion

## UOW

[Phase 6] UOW-2606: Execute NPC shop kinah buys live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` can now execute a normal NPC shop purchase for a kinah-only stackable item, mutating the active player's inventory
and sending the Java buy packet sequence.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: NPC shop buy-from-list action 13 now executes from live CM_BUY_ITEM instead of only composing disabled planner/outcome records.
- Java source method or runtime path: CM_BUY_ITEM.runImpl action 13 -> TradeService.performBuyFromShop -> TradeService.performBuyTransaction -> inventory.tryDecreaseKinah and ItemService.addItem(..., BUY/INC_ITEM_BUY).
- C# runtime artifact wired or fixed: GameServerConnection.HandleBuyItemAsync/TryExecuteBuyFromShopPurchaseAsync plus SM_INVENTORY_ADD_ITEM and SM_INVENTORY_UPDATE_ITEM buy masks.
- Client-visible/state/persistence effect changed: live active-player kinah decreases, bought item inventory state is added/updated, SM_INVENTORY_UPDATE_ITEM DEC_KINAH_BUY is sent, and SM_INVENTORY_ADD_ITEM BUY plus cube update are sent for new stack adds.
- Why this is not preview-only/test-only/documentation-only: this wires a deferred live client packet path with real player inventory mutation and real server packets.
```

## Java Source Reviewed

- `CM_BUY_ITEM.readImpl` creates a `TradeList` for action `13` and adds packet item IDs/counts.
- `CM_BUY_ITEM.runImpl` resolves the known-list target, gates NPC interaction, and dispatches action `13` through
  `TradeService.performBuyFromShop` when the NPC can sell.
- `TradeService.performBuyFromShop` routes `TradeNpcType.NORMAL` and `ABYSS_KINAH` to
  `performBuyTransaction(..., true)`.
- `TradeService.performBuyTransaction` validates allowed goods, checks kinah/AP/items/slots/limits, decreases kinah,
  then calls `ItemService.addItem(player, itemId, count, true, ItemAddType.BUY, ItemUpdateType.INC_ITEM_BUY)`.
- Java `ItemPacketService` defines `ItemAddType.BUY` and `ItemUpdateType.INC_ITEM_BUY` as `0x1C`; kinah spend uses
  `DEC_KINAH_BUY` (`0x1D`).

## C# Changes

- Changed `GameServerConnection.HandleBuyItemAsync` so ordinary no-observer NPC action `13` packets are no longer
  dropped by the private-store-only early live guard.
- Added `TryExecuteBuyFromShopPurchaseAsync` for the first live buy-from-shop slice: action `13`, `NORMAL` trade list,
  kinah-only, no AP/token costs, no limited-item counter update.
- Reused `TradeBuyTransactionPlanService` as the Java-derived validation/cost source before mutating live state.
- Reused `InventoryAddService` to apply Java-shaped stack add/update behavior and allow overflow for the post-cost
  item-add stage.
- Added `SmInventoryAddItem.Buy` / `CreateBuy` and `SmInventoryUpdateItem.IncreaseItemBuy` for Java `0x1C` buy masks.
- Extended the buy-item fixture so tests can run the ordinary no-observer live path.

## Known Gaps

- The live slice is limited to `CM_BUY_ITEM` action `13` with `TradeNpcType.NORMAL`, kinah cost, and no AP/token
  requirements.
- C# does not yet persist the NPC shop buy inventory/kinah mutation through a repository surface.
- C# does not yet update Java limited-item counters after a successful buy.
- C# does not yet execute AP, reward, required-item, or `ABYSS_KINAH` buy branches.
- C# does not yet send live Java denial packets for blocked buy-from-shop plans such as invalid goods, not enough kinah,
  full inventory, limited item, or not enough abyss points.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 NPC shop buy execution plus buy packet masks.
- Specific behavior/contract: no-observer live action 13 NORMAL shop purchase decreases kinah, adds the bought stack, sends DEC_KINAH_BUY, sends SM_INVENTORY_ADD_ITEM with BUY mask, and sends cube count update.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 328/328 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live handler state mutation and packet side effects changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer test dispatches an encoded CM_BUY_ITEM packet through the ordinary live guard and asserts state mutation plus packet order/masks.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM` action `13` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live handler | Partial | Unit Tested | Partial Parity | Normal kinah action 13 can execute live; AP/reward/required-item/denial branches remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers `NORMAL` kinah-only buy path; `ABYSS_KINAH`, `ABYSS`, `REWARD`, limited counters, and persistence remain missing. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` + live handler application | Service/planner/live application | Partial | Unit Tested | Partial Parity | Existing planner supplies Java-derived costs and validation; live handler applies only the safe kinah/item subset. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType#BUY` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.Buy` | Packet mask | Complete | Unit Tested | Partial Parity | Numeric mask `0x1C` is pinned; full buy packet byte golden remains broader packet coverage. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType#INC_ITEM_BUY` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.IncreaseItemBuy` | Packet mask | Complete | Unit Tested | Partial Parity | Numeric mask `0x1C` is pinned; live stack-update branch is implemented but not separately covered this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopExecutesNormalKinahPurchaseWithoutObservers` | Unit/live handler | `CM_BUY_ITEM.runImpl` + `TradeService.performBuyTransaction` + `ItemPacketService` | Ordinary no-observer action 13 packet mutates kinah/item state and sends DEC_KINAH_BUY, BUY add, and cube update packets | Java source review + live C# handler assertion | Does not cover persistence, AP/token costs, limited counters, or denial packets. |
| `GamePacketTests` buy mask assertions | Unit/packet | `ItemPacketService.ItemAddType.BUY` and `ItemUpdateType.INC_ITEM_BUY` | Pins both C# buy packet masks to Java `0x1C` | Java source review + C# constant assertion | Not a full packet golden for the buy packets. |

## Summary Metrics

- Focused UOW validation: 328 tests passed.
- Runtime progress: live NPC action 13 normal kinah buy now mutates player inventory and sends buy packets.
- Total Java artifacts touched/discovered this UOW: 5.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 5.
- Blocked artifacts: buy persistence, AP/token shop purchases, limited-item counters, denial packets, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live handler uses available C# world-object visibility; exact Java known-list membership remains approximated unless a resolver is supplied.
- The first live slice deliberately does not persist the new item/kinah row, so logout/restart parity is incomplete.
- `InventoryAddService` can produce multiple rows for non-stackable purchases; this UOW validated the stackable new-item case only.
- If no `IDFactory` or diagnostic object-id provider is available, new bought item creation cannot proceed.

## Next Runtime Candidate

UOW-2607 candidate: send the live not-enough-kinah denial packet for NPC action `13` normal shop buys.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: blocked NPC shop buys should send Java denial packets from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> if useKinah && !tradeList.calculateBuyListPrice -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync or adjacent action-13 denial helper using TradeBuyTransactionPlanStatus.BlockedNotEnoughKinah.
- Client-visible/state/persistence effect expected: live action 13 with insufficient kinah sends a real SM_SYSTEM_MESSAGE denial and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```
