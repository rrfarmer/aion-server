# Phase 6 Session 2623 Completion

## UOW

[Phase 6] UOW-2623: Execute partial-stack AP sell-to-shop live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_BUY_ITEM action 1 ABYSS sell-to-shop already executed exact-count AP item deletes live, but partial-stack sells returned before live mutation because the executor only accepted item.Count == sell count.
- Java source/runtime path: CM_BUY_ITEM.runImpl action 1 ABYSS -> TradeService.performSellForAPToShop(Player, TradeList, TradeListTemplate), especially Storage.decreaseByObjectId for counts smaller than the seller stack.
- C# runtime artifact wired: TradeSellForApToShopPlanService now carries updated seller item counts, GameServerConnection.TryExecuteSellForApToShopAsync applies and packets those updates, and PlayerEnterWorldService persists them through SaveNpcShopApSellMutationAsync.
- Client-visible/state/persistence effect: selling part of an AP item stack decreases the live inventory count, awards AP, persists the updated item row and abyss rank, and sends an inventory decrease update plus AP/rank packets without deleting the stack.
- Why this is runtime progress: this UOW extends an already-live client packet path with inventory state mutation, persistence, and packet fanout for a Java runtime branch.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - `runImpl` action 1 routes ABYSS purchase templates to `TradeService.performSellForAPToShop`.
- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performSellForAPToShop` calls `inventory.decreaseByObjectId(itemObjectId, count)` and only awards AP when the decrease succeeds.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseByObjectId` rejects missing or insufficient stacks, deletes exact-count stacks, and sends a decrease update plus persistence update for remaining stacks.

## C# Changes

- `TradeSellForApToShopPlanService`
  - Distinguishes exact-count deletes from partial-stack count updates.
  - Skips AP rewards when the requested count is larger than the live stack, matching the Java decrease failure path.
  - Carries copied `InventoryItem` updates for partial-stack decreases.
- `GameServerConnection.TryExecuteSellForApToShopAsync`
  - Allows sell counts smaller than the live stack.
  - Applies updated seller items to `Player.InventoryItems`.
  - Sends `SmInventoryUpdateItem` with the Java-aligned decrease update type before AP/rank packets.
- `PlayerEnterWorldService`
  - Persists AP sell seller item updates through the existing AP sell mutation repository call.
- Focused tests now cover the planner contract and the live packet/state/persistence path for a partial-stack AP sell.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_UpdatesPartialStackAndPlansAbyssPointReward` | Unit/planner consumed live | `TradeService.performSellForAPToShop` and `Storage.decreaseByObjectId` | Partial-stack AP sell plans an item count update and AP reward instead of a delete. | Java source review plus C# planner assertions. | Planner evidence counts only because the live executor consumes the update. |
| `CreatePlan_TooLargeCountSkipsAbyssPointRewardAndContinuesLikeJava` | Unit/planner consumed live | `Storage.decreaseByObjectId` insufficient-count rejection | Oversized sell count is skipped and later valid items can still sell. | Java source review plus C# planner assertions. | Does not prove audit logging or real client behavior. |
| `ProcessPacketAsync_CmBuyItemNpcAbyssSellActionUpdatesPartialStackLive` | Unit/live handler | `CM_BUY_ITEM.runImpl` action 1 ABYSS and `TradeService.performSellForAPToShop` | Live packet execution decreases the stack, persists item/AP state, and sends item decrease plus AP/rank packets. | Focused C# live handler assertions over runtime state, repository capture, and packets. | Uses fake repository capture; no live MySQL or real client validation. |

## Validation Decision

```text
- Changed surface: live handler/state/persistence/packet boundary for NPC AP sell-to-shop.
- Specific behavior/contract: CM_BUY_ITEM action 1 ABYSS partial-stack sell decreases item count, awards AP, persists item/AP rows, and sends item-update/AP/rank packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for TradeService.performSellForAPToShop partial-stack behavior.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused coverage because the command built the affected projects and directly covered planner, handler, persistence handoff, state mutation, and packet fanout.
- Why this scope is sufficient: the focused tests exercise the Java-derived decrease semantics and the live packet path that now consumes partial-stack updates.
```

Result: passed, 119/119.

Notes:

- An initial focused run failed compilation because adjacent `TradeSellForApToShopPlan` test fixtures needed the new `UpdatedItems` record argument. The fixtures were updated.
- A later focused run failed because the new live test used the BUY dialog action instead of `TRADE_SELL_LIST`; the fixture was corrected to the Java purchase gate (`103`) and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 ABYSS AP sell now executes exact-count deletes and partial-stack decreases for the covered success paths. Pet sell action 17 and real-client validation remain gaps. |
| `com.aionemu.gameserver.services.TradeService#performSellForAPToShop` | `Aion.GameServer.Services.TradeSellForApToShopPlanService` plus `GameServerConnection.TryExecuteSellForApToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Partial-stack seller item updates, AP reward, persistence handoff, and item/AP/rank packets execute live. Java audit logging and live MySQL are not validated. |
| `com.aionemu.gameserver.model.items.storage.Storage#decreaseByObjectId` | AP sell inventory mutation in `GameServerConnection` | Runtime inventory mutation | Partial | Unit Tested | Partial Parity | Covered for insufficient count, exact-count delete, and partial-stack decrease in the AP sell path only. Broader Storage semantics remain partial. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 8.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live MySQL execution for AP sell partial-stack persistence was not run.
- Real client validation was not run.
- Java audit logging for AP sell trade-abuse cases remains modeled but not emitted through live C# audit infrastructure.
- C# still reports disabled side-effect outcome records to existing observers for diagnostics; live side effects are executed independently by the handler.
- Pet merchant action 17 sell-to-shop remains deferred.
