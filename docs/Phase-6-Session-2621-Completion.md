# Phase 6 Session 2621 Completion

## UOW

[Phase 6] UOW-2621: Execute `ABYSS` AP sell-to-shop action 1 live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `CM_BUY_ITEM` action 1 with an `ABYSS` purchase template previously created disabled AP-sell outcome records without mutating inventory, AP/rank state, persistence, or packets.
- Java source/runtime path: CM_BUY_ITEM.runImpl action 1 -> TradeService.performSellForAPToShop(Player, TradeList, TradeListTemplate), including CustomConfig.SELLING_APITEMS_ENABLED, PlayerRestrictions.canTrade, goods-list validation, inventory.decreaseByObjectId, and AbyssPointsService.addAp.
- C# runtime artifact wired: GameServerConnection now dispatches ready `TradeSellForApToShopPlan` instances to a live AP sell executor; PlayerEnterWorldService and repository persist item deletion plus abyss-rank update.
- Client-visible/state/persistence effect: exact-count AP item resale removes the sold item, increases player AP/rank state, persists inventory/AP state, and sends delete/cube/AP/rank packets from live code.
- Why this is runtime progress: this UOW executes the remaining Java action 1 shop branch for the covered exact-count case with live inventory/AP mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - `runImpl` action 1 `TradeNpcType.ABYSS` branch.
- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performSellForAPToShop`.
- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
  - Existing C# Java-parity helper for `AbyssPointsService.addAp`.

## C# Changes

- `GameServerConnection.HandleBuyItemAsync`
  - Dispatches AP sell plans through a live executor after normal sell-to-shop handling.
- `GameServerConnection.TryExecuteSellForApToShopAsync`
  - Executes ready AP sell plans for exact-count inventory deletion.
  - Blocks partial-stack AP sell for now because the current plan model only exposes deleted object ids, not seller item count updates.
  - Creates the AP add plan, persists the updated abyss rank and sold item deletion, mutates `Player.InventoryItems` and `Player.AbyssRank`, and sends delete/cube/AP/rank packets.
- `PlayerEnterWorldService` / `PlayerEnterWorldRepository`
  - Added `SaveNpcShopApSellMutationAsync` to persist abyss-rank update plus seller item updates/deletes in one transaction.
- `GameServerConnectionBuyItemTests`
  - Converted the ABYSS AP sell handler test from disabled-outcome-only evidence to live persistence/mutation/packet evidence while keeping the existing diagnostic assertions.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcAbyssSellActionHydratesDisabledApSellPlanFromInventoryFacts` | Unit/live handler | `CM_BUY_ITEM.runImpl` action 1 ABYSS branch and `TradeService.performSellForAPToShop` | Live AP sell deletes the exact-count item, persists abyss rank AP gain and item deletion, mutates player state, and sends delete/cube/AP-gain/rank packets. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. Partial-stack AP sell remains blocked. |
| Existing `TradeSellForApToShopPlanServiceTests.CreatePlan_DeletesItemsAndPlansAbyssPointRewards` | Unit/planner consumed live | `TradeService.performSellForAPToShop` | Confirms goods-list validation and Java AP resale formula used by the live executor. | Java source review + focused C# assertions. | Planner-only evidence counts here only because the plan is consumed by live handler code. |

## Validation Decision

```text
- Changed surface: live handler/state/persistence boundary for AP sell-to-shop.
- Specific behavior/contract: ABYSS NPC action 1 exact-count AP sell deletes a sold item, increases AP with Java reward formula, persists abyss rank and inventory deletion, and sends live item/AP packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `TradeService.performSellForAPToShop`.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner/service coverage because the command built the affected project and directly covered the edited dispatch, AP planner contract, persistence handoff, and packet fanout.
- Why this scope is sufficient: the focused tests exercise the new live AP sell branch, item deletion persistence capture, abyss-rank persistence capture, player inventory/AP mutation, AP reward formula, and delete/cube/AP/rank packets.
```

Result: passed, 116/116.

Note: an initial focused run passed compilation but failed one existing assertion because the AP sell branch now sends live packets; the test was updated to assert the live persistence, state, and packet effects, and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 now executes normal sell-to-shop and exact-count ABYSS AP sell-to-shop live. Repurchase action 2 remains disabled. |
| `com.aionemu.gameserver.services.TradeService#performSellForAPToShop` | `Aion.GameServer.Services.TradeSellForApToShopPlanService` plus `GameServerConnection.TryExecuteSellForApToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Exact-count AP item deletion executes live with AP gain, persistence, and packets. Partial-stack decrease is not yet modeled live. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService#addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` plus AP sell executor | Service/live state mutation | Partial | Unit Tested | Partial Parity | AP sell uses the existing C# add-AP plan and sends AP/rank packets. Rank side-effect breadth remains partial. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live MySQL execution for AP sell-to-shop was not run.
- Real client validation was not run.
- Partial-stack AP sell is intentionally blocked by the live executor until seller item updates are modeled in `TradeSellForApToShopPlan`.
- Feature-disabled AP sell message remains disabled-output evidence only; this UOW covered the enabled exact-count success path.
- Java `performSellForAPToShop` continues after individual `decreaseByObjectId` failures; only the all-success exact-count path was executed live here.
