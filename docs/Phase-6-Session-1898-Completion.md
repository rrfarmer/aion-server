# Phase 6 Session 1898 Completion - Private Store Bought-Items Planner

Date: 2026-05-31
Unit of Work: UOW-1898
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` action `0`, Java `PrivateStoreService.sellStoreItem`, Java `PrivateStoreService.getBoughtItems`, C# `CmBuyItem`, C# private-store planners, and `CmBuyItemHandlerCompositionPlanService`.
- Added a non-live planner for the Java private-store bought-items selection step where action `0` treats `CM_BUY_ITEM` item ids as private-store list indices.
- Kept live socket handlers, live private-store mutation, inventory mutation, Kinah transfer, item cloning, packet sends, audit logging side effects, repository writes, Java runtime capture, and real client validation out of scope.

## What Changed

- Added `PrivateStoreBoughtItemsPlanService`.
- Added `PrivateStoreListedItemSummary`, `PrivateStoreBoughtItemsPlan`, and `PrivateStoreBoughtItemsPlanStatus`.
- Modeled Java `getBoughtItems` index lookup over insertion-ordered private-store items.
- Modeled invalid index and requested-count-greater-than-store-count branches as null-equivalent blocked plans with no bought-items output.
- Added focused tests for successful index mapping, empty trade-list pass-through, invalid indices, over-count rejection, and null-equivalent failure output.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests|FullyQualifiedName~PrivateStoreOpenPlanServiceTests|FullyQualifiedName~PrivateStoreOpenGuardPlanServiceTests|FullyQualifiedName~PrivateStoreItemValidationPlanServiceTests|FullyQualifiedName~PrivateStoreClosePlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused private-store bought-items slice passed with 6 tests.
- Related private-store/`CM_BUY_ITEM` slice passed with 80 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4858 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The bought-items planner remains non-live and is not invoked by `GameServerConnection`.
- `PrivateStorePurchasePlanService` remains non-live and is not executed by live private-store handling.
- Java private-store live state, insertion-order source map, seller/buyer online and race state, inventory state, Kinah balances, item cloning, packet sends, audit logging, logging side effects, and close-store mutation remain represented by supplied facts or separate non-live planners.

## Parity Table Updates

- Added Session 1898 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `PrivateStoreService.getBoughtItems`
  - `CM_BUY_ITEM` action `0` private-store item-index interpretation
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: compose `PrivateStoreBoughtItemsPlanService` with `PrivateStorePurchasePlanService` behind the `CM_BUY_ITEM` player-target action `0` planner path, keeping it non-live and disabled from socket mutation.

Safe alternative candidates:

- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
