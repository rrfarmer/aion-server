# Phase 6 Session 1896 Handoff - CM_BUY_ITEM AP-Sell Composition Payload

Date: 2026-05-31
Unit of Work: UOW-1896
Status: Completed

## What Changed

- Updated `CmBuyItemSellToShopCompositionPlanService` to carry an optional `TradeSellForApToShopPlan`.
- Added `SellForApToShopPlan` to the sell-to-shop composition input and dispatch descriptor.
- Added `AttachSellForApToShopPlan` composition step.
- Updated focused tests for ABYSS AP-sell payload attachment and normal sell branch payload exclusion.
- Kept this work non-live. No socket handler, live `TradeService`, config read, player restriction query, goods-list lookup, inventory deletion, AP mutation, packet fanout, audit logging side effects, repository writes, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeApFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused sell composition slice passed with 11 tests.
- Related trade/AP/`CM_BUY_ITEM` slice passed with 46 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4850 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The sell composition planner remains non-live and is not invoked by `GameServerConnection`.
- The AP-sell planner remains non-live and is not executed by `TradeService`.
- Java config, player restrictions, goods-list lookup, item-template acquisition lookup, inventory state, AP state, packet sends, audit logging, and transaction boundaries remain unimplemented live behavior.

## Parity Table Updates

- Added Session 1896 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` action `1` AP-sell dispatch payload
  - `TradeService.performSellForAPToShop` planned AP-sell result payload
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: connect `TradeBuyTransactionPlanService` as an optional non-live payload inside `CmBuyItemBuyFromShopCompositionPlanService` for buy-from-shop dispatch, without invoking live side effects.

Safe alternative candidates:

- Inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.
- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellToShopCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSellToShopCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellForApToShopPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemBuyFromShopCompositionPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1896-Completion.md`
- `docs/Phase-6-Session-1896-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing trade work, inspect:
  - Java `CM_BUY_ITEM` actions `13`-`16`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performBuyTransaction`
  - C# `CmBuyItemBuyFromShopCompositionPlanService`
  - C# `TradeBuyTransactionPlanService`
  - C# tests for buy-from-shop composition and transaction planning
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
