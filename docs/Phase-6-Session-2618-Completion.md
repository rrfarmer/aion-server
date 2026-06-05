# Phase 6 Session 2618 Completion

## UOW

[Phase 6] UOW-2618: Execute NPC shop `ABYSS` buys live without kinah.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `ABYSS` buy transactions were planned with useKinah=false but blocked by the live handler's use-kinah guard and unconditional kinah item/template requirement.
- Java source/runtime path: TradeService.performBuyFromShop -> TradeNpcType.ABYSS -> performBuyTransaction(..., false), which skips kinah price/decrease but still validates AP/required items, inventory, limited items, then adds bought items and updates limited counters.
- C# runtime artifact wired: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync now accepts `ABYSS` and treats kinah updates as nullable when the transaction requires no kinah.
- Client-visible/state/persistence effect: action 13 `ABYSS` NPC buys spend AP and/or required items without requiring kinah, persist live AP/item mutations, send AP/item packets, add bought items, and keep limited-counter behavior available.
- Why this is runtime progress: this UOW removes a live packet-handler guard and executes a real no-kinah client buy path with state mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyFromShop`
  - `performBuyTransaction`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
  - `TradeNpcType.ABYSS`

## C# Changes

- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
  - Added `ABYSS` to the live action 13 shop type classifier.
  - Requires a kinah template and inventory row only when `RequiredKinah > 0`.
  - Passes a null kinah item into NPC-shop buy persistence for no-kinah transactions.
  - Sends no kinah update packet when Java `useKinah=false` produces no kinah cost.
- `GameServerConnectionBuyItemTests`
  - Added live handler coverage for an `ABYSS` AP buy with no kinah template and no kinah inventory item.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcAbyssBuyFromShopSpendsApWithoutKinahLive` | Unit/live handler | `TradeService.performBuyFromShop` `ABYSS` branch and `performBuyTransaction(..., false)` | Live action 13 `ABYSS` buy spends AP, persists AP/item mutations with no kinah row, mutates player state, adds the item, and sends AP/item packets without a kinah packet. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. |
| Existing `TradeBuyTransactionPlanServiceTests.CreatePlan_SkipsKinahCalculationWhenJavaUseKinahIsFalse` | Unit/planner consumed live | Java `performBuyTransaction(..., false)` | Confirms the transaction plan skips kinah cost calculation when Java useKinah is false. | Java source review + focused C# assertions. | Planner itself is not runtime progress except when consumed by this UOW's live handler. |

## Validation Decision

```text
- Changed surface: live handler guard, no-kinah transaction mutation, persistence input, and packet fanout.
- Specific behavior/contract: `ABYSS` action 13 buys execute live with Java useKinah=false, spend AP without requiring kinah, persist no kinah item, and send no kinah packet.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `TradeService` `ABYSS` no-kinah buy behavior.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner coverage because the changed runtime path was isolated to action 13 no-kinah buy execution and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the edited dispatch classifier, nullable kinah persistence handoff, absence of kinah packets, AP mutation, item add, and the planner's Java useKinah=false behavior.
```

Result: passed, 56/56.

Note: an initial run of the same focused command failed because the new test expected the old rank after a large AP spend. The assertion was corrected to reflect the existing AP rank recalculation path, and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | `NORMAL`, `ABYSS_KINAH`, and `ABYSS` action 13 buy branches now execute live. `REWARD` remains blocked. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` plus live handler consumption | Service/live transaction | Partial | Unit Tested | Partial Parity | `ABYSS` uses Java `useKinah=false`; kinah row and packet are omitted when required kinah is zero. Required-item and limited-item behavior reuse existing live path. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeNpcType#ABYSS` | `Aion.GameServer.Dataholders.TradeListTemplateSummary.NpcType` and buy handler classifier | Static data/handler routing | Partial | Unit Tested | Partial Parity | `ABYSS` now routes to live no-kinah buy execution. Full XML/default coverage remains outside this UOW. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live NPC shop action 13 execution still blocks `REWARD`.
- Rank-change AP spend side effects reuse existing helper behavior but were not specifically exercised beyond packet payload changes in this UOW.
- Live MySQL execution for this path was not run.
- Real client validation was not run.
