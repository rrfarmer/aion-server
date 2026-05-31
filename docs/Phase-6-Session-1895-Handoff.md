# Phase 6 Session 1895 Handoff - Trade Sell For AP Planner

Date: 2026-05-31
Unit of Work: UOW-1895
Status: Completed

## What Changed

- Added `TradeSellForApToShopPlanService`, a non-live AP-sell decision/reward planner for Java `TradeService.performSellForAPToShop`.
- Added AP-sell status, step, item request, AP reward, and plan records.
- Added focused tests for Java branch ordering, purchase goods validation, AP reward calculation, and delete-failure continue behavior.
- Kept this work non-live. No live config read, live player restriction query, live goods-list lookup, inventory deletion, AP mutation, packet fanout, audit logging side effects, repository writes, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeApFormulaServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused AP-sell planner slice passed with 8 tests.
- Related trade/AP/`CM_BUY_ITEM` slice passed with 45 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4849 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The AP-sell planner is non-live and is not invoked by `TradeService`, `CmBuyItemSellToShopCompositionPlanService`, or `GameServerConnection`.
- Java config, `PlayerRestrictions`, trade-list template lookup, goods-list lookup, item-template acquisition lookup, inventory state, and AP state are represented by supplied facts or existing helpers.
- Live inventory deletion, AP mutation, packet sends, audit logging, and transaction boundaries remain unimplemented.

## Parity Table Updates

- Added Session 1895 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `TradeService.performSellForAPToShop` decision/AP reward flow
  - `CM_BUY_ITEM` action `1` AP-sell branch dependency
  - `TradeList` AP-sell item iteration payload
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch, without invoking live side effects.

Safe alternative candidates:

- Connect `TradeBuyTransactionPlanService` as an optional payload inside `CmBuyItemBuyFromShopCompositionPlanService` without invoking live side effects.
- Inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.
- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellForApToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellForApToShopPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellToShopCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeApFormulaService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1895-Completion.md`
- `docs/Phase-6-Session-1895-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing trade work, inspect:
  - Java `CM_BUY_ITEM` action `1`
  - Java `TradeService.performSellForAPToShop`
  - C# `CmBuyItemSellToShopCompositionPlanService`
  - C# `TradeSellForApToShopPlanService`
  - C# `TradeSellToShopPlanService`
  - C# tests for sell/AP composition
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
