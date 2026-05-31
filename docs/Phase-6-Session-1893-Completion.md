# Phase 6 Session 1893 Completion - CM_BUY_ITEM Handler Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1893
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM.runImpl`, C# `CmBuyItem`, packet registration, and the existing non-live sell, repurchase, and buy-from-shop branch composition planners.
- Added a non-live top-level branch selector for Java `CM_BUY_ITEM.runImpl`.
- Kept live socket handlers, known-list lookup, live interaction checks, NPC/Pet state queries, private-store sale, pet merchant sale, inventory/AP/Kinah mutation, repository writes, packet fanout, audit logging side effects, and Java runtime capture out of scope.

## What Changed

- Added `CmBuyItemHandlerCompositionPlanService`.
- Added composition status, step, input, and plan records.
- Added focused tests for sell, repurchase, buy-from-shop branch delegation; parser audit stop; missing player and missing target gates; Player target action `0`; Npc interaction audit ordering; Npc unknown action default branch; and Pet action `17` merchant classification.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused handler composition slice passed with 15 tests.
- Related `CM_BUY_ITEM` parser/branch planner slice passed with 78 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4831 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The handler composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC/Pet state queries, and dependency calls are represented by supplied facts and nested planners.
- Java private-store action `0` and Pet merchant action `17` are identified as unsupported branches only.
- Java `performBuyTransaction`, `performSellForAPToShop`, and live sell/repurchase mutation internals remain partially represented only by prior non-live planners.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, AP mutation, Kinah mutation, inventory mutation, limited-item goods-list mutation, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1893 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` top-level target/action branch selection
  - `PrivateStoreService.sellStoreItem` action `0` call site
  - Pet MERCHANT action `17` `TradeService.performSellToShop` call site
- Rows remain conservative: top-level branch selector is `Partial Parity`; unsupported private-store and pet branches are `Needs Verification`.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `TradeService.performBuyTransaction` internals and add a non-live buy-from-shop transaction planner only for validation/cost/free-slot/limit decision ordering, without live AP/Kinah/item mutation.

Safe alternative candidates:

- Inspect Java `TradeService.performSellForAPToShop` internals as a non-live AP-sell planner.
- Inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.
- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
