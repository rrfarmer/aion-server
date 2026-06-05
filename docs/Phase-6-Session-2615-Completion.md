# Phase 6 Session 2615 Completion

## UOW

[Phase 6] UOW-2615: Mutate NPC shop limited-item counters live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful normal NPC shop buys for limited items now update the live limited-item state used by later buys and trade-list facts.
- Java source/runtime path: TradeService.performBuyTransaction final success loop -> LimitedItemTradeService.getInstance().getLimitedItem(itemId, npcId) -> LimitedItem.setBuyCount(...) and LimitedItem.setSellLimit(...), after ItemService.addItem succeeds.
- C# runtime artifact wired: LimitedItemTradeService, GameServerRuntimeContext, GameServerConnection.TryExecuteBuyFromShopPurchaseAsync, and NpcDialogLimitedItemFactAdapterService.
- Client-visible/state/persistence effect: a successful action 13 normal shop buy decrements the NPC/item sell limit and increments the player's buy count in live runtime state, so later buys can be denied and later buy dialogs can report updated limited-item facts.
- Why this is runtime progress: this UOW mutates live shop-limit runtime state from the client packet path and changes subsequent client-visible buy eligibility; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyTransaction`
  - `canBuyLimitItem`
- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`
  - `start`
  - `getLimitedItem`
- `game-server/src/com/aionemu/gameserver/model/limiteditems/LimitedItem.java`
  - `getBuyCount`
  - `setBuyCount`
  - `setSellLimit`
  - `setToDefault`
- `game-server/src/com/aionemu/gameserver/model/limiteditems/LimitedTradeNpc.java`
  - `getLimitedItem`

## C# Changes

- Added `LimitedItemTradeService`.
  - Builds runtime limited-item state from `TradeListTable` and `GoodsListTable`, matching Java's `LimitedItemTradeService.start` data walk.
  - Exposes live facts for dialog packet planning.
  - Applies Java-equivalent sell-limit and per-player buy-limit checks.
  - Mutates per-player buy count and current sell limit after a successful buy.
- Updated `GameServerRuntimeContext`.
  - Creates the limited-item runtime service when static data is loaded.
- Updated `GameServerConnection`.
  - Uses live limited-item facts for buy dialogs and buy transaction planning when runtime state is available.
  - Calls `LimitedItemTradeService.BuyItem` after successful item-add packet fanout in the live action 13 normal shop path.
- Updated `NpcDialogLimitedItemFactAdapterService`.
  - Accepts live limited-item facts and emits live packet summaries instead of rebuilding static defaults when runtime state is available.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopSuccessfulLimitedItemUpdatesLiveCounter` | Unit/live handler | `TradeService.performBuyTransaction` plus `LimitedItem` counter mutation | A successful limited-item shop buy adds the item, spends kinah, mutates live limited counters, and a second buy is denied by the updated runtime state. | Java source review + focused C# live handler assertions. | Uses in-memory runtime service; no real client validation. |
| `LimitedItemTradeServiceTests.BuyItem_UpdatesPlayerBuyCountAndSellLimitLikeJava` | Unit/service | `LimitedItem.setBuyCount`, `LimitedItem.setSellLimit`, and `canBuyLimitItem` checks | Service-level buy mutation increments player buy count, decrements sell limit, and affects later `CanBuy` checks. | Java source review + focused C# assertions. | Reset scheduling is not implemented in this UOW. |
| `LimitedItemTradeServiceTests.BuyItem_NoOpsForNonLimitedItemLikeJavaMissingLimitedItem` | Unit/service | `LimitedItemTradeService.getLimitedItem` null branch | Non-limited goods remain unrestricted and do not create mutation state. | Java source review + focused C# assertions. | None for this branch. |
| `NpcDialogLimitedItemFactAdapterServiceTests.CreatePlan_UsesLiveLimitedItemsWhenRuntimeStateIsAvailable` | Unit/packet-plan boundary | `LimitedItem.getBuyCount` and current `sellLimit` packet facts | Dialog packet planning prefers live runtime limited-item values over static defaults. | Focused C# assertions. | Packet serialization itself was covered by existing tests. |

## Validation Decision

```text
- Changed surface: live handler state mutation, limited-item runtime service, buy dialog limited-item facts, and transaction planning input.
- Specific behavior/contract: successful limited-item action 13 normal NPC shop buy mutates current sell limit and player buy count, and later buy attempts observe those updated counters.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~NpcDialogLimitedItemFactAdapterServiceTests|FullyQualifiedName~LimitedItemTradeServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for TradeService/LimitedItemTradeService shop-buy mutation.
- Broad-validation trigger: live state boundary changed.
- Broad .NET decision: skipped after focused live handler/service/dialog coverage because the changed path was isolated to limited-item NPC-shop action 13 behavior and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the edited live dispatch path, the new runtime service, the dialog fact handoff, and adjacent transaction-planner behavior.
```

Result: passed, 121/121.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Normal action 13 buy success now handles kinah-only, required-item, AP-cost, and limited-counter mutation branches. Non-normal shop types remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#canBuyLimitItem` | `Aion.GameServer.Services.LimitedItemTradeService.CanBuy` plus transaction planner input | Service/state gate | Partial | Unit Tested | Partial Parity | Later buys observe live sell-limit/player-buy-count state when runtime context is available. |
| `com.aionemu.gameserver.services.LimitedItemTradeService#start` | `Aion.GameServer.Services.LimitedItemTradeService.Create` | Runtime static-data load | Partial | Unit Tested | Partial Parity | C# loads limited-item runtime state from trade/goods lists used by live code. Java cron reset scheduling is not wired yet. |
| `com.aionemu.gameserver.model.limiteditems.LimitedItem` | `Aion.GameServer.Services.LimitedItemRuntimeState` | Runtime state | Partial | Unit Tested | Partial Parity | Buy count and sell-limit mutation are covered. `setToDefault` scheduled reset remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` limited-item facts | `NpcDialogLimitedItemFactAdapterService` and buy-dialog planning | Packet facts | Partial | Unit Tested | Partial Parity | Buy dialogs can emit live counts when runtime state is available. Real client validation was not run. |

## Known Gaps

- Java schedules `LimitedItem.setToDefault` from `LimitedItemTradeService.start` using each goods list `salesTime`; C# does not yet schedule limited-item resets.
- Limited-item state is runtime in-memory state in this UOW, matching the reviewed Java service shape; no DB persistence was added.
- Live NPC shop execution remains scoped to `TradeNpcType.NORMAL`, action `13`.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Duplicate limited-item definitions across tabs were not deeply validated.
- Real client validation was not run.
