# Phase 6 Session 2617 Completion

## UOW

[Phase 6] UOW-2617: Execute NPC shop `ABYSS_KINAH` buys live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `ABYSS_KINAH` buy transactions were planned but blocked by the live handler's `NpcType == NORMAL` guard.
- Java source/runtime path: TradeService.performBuyFromShop -> TradeNpcType.ABYSS_KINAH -> performBuyTransaction(..., true), with sellModifier = template.getSellPriceRate2() and apSellModifier = template.getApSellPriceRate2().
- C# runtime artifact wired: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync now accepts the Java use-kinah shop types through ShouldUseKinahForBuyTransaction; existing TradeBuyTransactionPlanService secondary-rate logic is consumed by live code.
- Client-visible/state/persistence effect: action 13 `ABYSS_KINAH` NPC buys spend kinah and AP using Java's secondary rates, persist the live inventory/AP mutation, send AP/kinah/item packets, and add the bought item.
- Why this is runtime progress: this UOW removes a live packet-handler guard and executes a real client buy path with state mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyFromShop`
  - `performBuyTransaction`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
  - `TradeNpcType.ABYSS_KINAH`
  - `getSellPriceRate2`
  - `getApSellPriceRate2`

## C# Changes

- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
  - Replaced the `NpcType == NORMAL` live guard with the existing Java-equivalent use-kinah classifier.
  - Allows `NORMAL` and `ABYSS_KINAH` action 13 buy paths to execute live.
- `GameServerConnectionBuyItemTests`
  - Added live handler coverage for `ABYSS_KINAH` secondary kinah/AP rates, persistence capture, AP state mutation, inventory mutation, and packet order.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcAbyssKinahBuyFromShopUsesSecondaryRatesLive` | Unit/live handler | `TradeService.performBuyFromShop` and `performBuyTransaction` `ABYSS_KINAH` branch | Live action 13 `ABYSS_KINAH` buy spends AP and kinah using `sellPriceRate2`/`apSellPriceRate2`, persists the mutation, mutates player state, and sends AP/kinah/item packets in Java order. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. |
| Existing `TradeBuyTransactionPlanServiceTests.CreatePlan_UsesAbyssKinahSecondaryRatesLikeJavaCaller` | Unit/planner consumed live | Java secondary-rate branch | Confirms the planner computes the secondary AP/kinah costs consumed by the live handler. | Java source review + focused C# assertions. | Planner itself is not runtime progress except when consumed by this UOW's live handler. |

## Validation Decision

```text
- Changed surface: live handler guard and live buy execution for an additional Java TradeNpcType.
- Specific behavior/contract: `ABYSS_KINAH` action 13 buys execute live with Java secondary kinah/AP modifiers and the existing AP/kinah/item persistence and packet order.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `TradeService` `ABYSS_KINAH` buy pricing.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner coverage because the changed runtime path was isolated to action 13 buy execution and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the edited dispatch guard, the live AP/kinah/inventory mutation path, persistence capture, packet fanout, and the Java secondary-rate cost calculation.
```

Result: passed, 55/55.

Note: an initial run of the same focused command failed at compile time because the new test referenced the repository capture with the wrong type name. The test assertion was corrected and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | `NORMAL` and `ABYSS_KINAH` action 13 buy branches now execute live. `ABYSS` and `REWARD` no-kinah branches remain blocked. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` plus live handler consumption | Service/live transaction | Partial | Unit Tested | Partial Parity | `ABYSS_KINAH` uses secondary kinah/AP rates and existing AP/kinah/item mutation order. Required-item and limited-item behavior reuse existing live path. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` secondary rates | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | Static data/DTO | Partial | Unit Tested | Partial Parity | `SellPriceRate2` and `ApSellPriceRate2` are consumed by live `ABYSS_KINAH` buy execution. Full XML/default coverage remains outside this UOW. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live NPC shop action 13 execution still blocks `ABYSS` and `REWARD` branches.
- The current live buy method still assumes a kinah template/item is present; this must be adjusted before enabling no-kinah Java branches.
- Rank-change AP side effects reuse existing helper behavior but were not specifically exercised by this UOW.
- Live MySQL execution for this path was not run.
- Real client validation was not run.
