# Phase 6 Session 1896 Completion - CM_BUY_ITEM AP-Sell Composition Payload

Date: 2026-05-31
Unit of Work: UOW-1896
Status: Completed

## Scope

- Performed Work Discovery around C# `CmBuyItemSellToShopCompositionPlanService`, `TradeSellForApToShopPlanService`, and Java `CM_BUY_ITEM` action `1` AP-sell dispatch.
- Connected the existing AP-sell planner as an optional non-live payload in the sell composition descriptor.
- Kept live socket handlers, live `TradeService`, config reads, player restrictions, inventory deletion, AP mutation, packet fanout, audit logging side effects, repository writes, and Java runtime capture out of scope.

## What Changed

- Added `SellForApToShopPlan` to `CmBuyItemSellToShopDispatchDescriptor`.
- Added `SellForApToShopPlan` to `CmBuyItemSellToShopCompositionInput`.
- Added `AttachSellForApToShopPlan` composition step.
- Updated tests to verify ABYSS purchase-template dispatch carries AP-sell payloads and normal sell dispatch ignores them.

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
