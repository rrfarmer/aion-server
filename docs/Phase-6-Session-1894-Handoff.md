# Phase 6 Session 1894 Handoff - Trade Buy Transaction Planner

Date: 2026-05-31
Unit of Work: UOW-1894
Status: Completed

## What Changed

- Added `TradeBuyTransactionPlanService`, a non-live decision/cost planner for Java `TradeService.performBuyTransaction`.
- Added transaction status, step, input, item request, required-item, mutation descriptor, and plan records.
- Added focused tests covering Java guard order and cost derivation.
- Kept this work non-live. No live trade-list/goods-list lookup, live player restriction query, live inventory/AP/Kinah mutation, limited-item mutation, packet fanout, audit logging side effects, repository writes, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~TradeApFormulaServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~SmTradeListPacketPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused transaction planner slice passed with 10 tests.
- Related trade/AP/`CM_BUY_ITEM` slice passed with 55 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4841 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The transaction planner is non-live and is not invoked by `TradeService` or `GameServerConnection`.
- Java `PricesService.getBuyPrice`, trade-list template lookup, goods-list lookup, item-template acquisition lookup, inventory/AP state, and limited-item state are represented by supplied facts or existing helpers.
- Live AP subtraction, Kinah decrease, required-item removal, item add, limited-item counter mutation, packet sends, audit logging, and transaction boundaries remain unimplemented.

## Parity Table Updates

- Added Session 1894 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `TradeService.performBuyTransaction` decision order
  - `TradeList.calculateBuyListPrice` call site
  - `TradeList.calculateAbyssRewardBuyList` call site
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `TradeService.performSellForAPToShop` internals and add a non-live AP-sell planner only for config, trade restriction, template/acquisition validation, AP reward, and inventory deletion decision ordering.

Safe alternative candidates:

- Connect `TradeBuyTransactionPlanService` as an optional payload inside `CmBuyItemBuyFromShopCompositionPlanService` without invoking live side effects.
- Inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.
- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeBuyTransactionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeBuyTransactionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemBuyFromShopCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeApFormulaService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1894-Completion.md`
- `docs/Phase-6-Session-1894-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing trade work, inspect:
  - Java `TradeService.performSellForAPToShop`
  - Java `CustomConfig.SELLING_APITEMS_ENABLED`
  - Java `PlayerRestrictions.canTrade`
  - Java `TradeListTemplate` buy price rate
  - Java `ItemTemplate.Acquisition`
  - C# `TradeBuyTransactionPlanService`
  - C# `TradeSellToShopPlanService`
  - C# `TradeApFormulaService`
  - C# AP/inventory mutation planners and packet plan services
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
