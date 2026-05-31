# Phase 6 Session 1892 Completion - CM_BUY_ITEM Buy-From-Shop Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1892
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` actions `13`-`16`, `TradeService.performBuyFromShop`, and the `performBuyTransaction` `useKinah` branch selection.
- Added a non-live composition planner for action `13`-`16` parser-to-dispatch intent.
- Kept live socket handlers, known-list lookup, live interaction checks, NPC state queries, trade-list data lookup, inventory/AP/Kinah mutation, limited-item goods-list mutation, repository writes, packet fanout, audit logging side effects, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItemBuyFromShopCompositionPlanService`.
- Added composition status, step, input, item request, dispatch descriptor, and plan records.
- Added focused tests for action `13`-`16` buy-from-shop dispatch, trade NPC type to `useKinah` branch mapping, parser audit stop, non-action skip, target branch gates, interaction audit ordering, NPC capability skip, unknown trade NPC type handling, and missing-player skip.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~SmTradeListPacketPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused composition slice passed with 17 tests.
- Related parser/buy/sell/trade-list slice passed with 53 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4816 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC `canSell`, and trade-list template lookup are represented by supplied facts.
- Java `performBuyTransaction` internals remain unported here; only action `13`-`16` branch selection and `useKinah` classification are represented.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, AP mutation, Kinah mutation, inventory mutation, limited-item goods-list mutation, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1892 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` action `13`-`16` run flow
  - `TradeService.performBuyFromShop` `NORMAL`/`ABYSS_KINAH` branch
  - `TradeService.performBuyFromShop` `ABYSS`/`REWARD` branch
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect live C# `GameServerConnection` handler gaps for `CmBuyItem` and decide whether a non-live dispatcher aggregation layer can connect the parser/composition planners without enabling side effects.

Safe alternative candidates:

- Inspect Java `TradeService.performBuyTransaction` internals as a non-live buy-from-shop transaction planner.
- Inspect Java `TradeService.performSellForAPToShop` internals as a non-live AP-sell planner.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
