# Phase 6 Session 1899 Completion - CM_BUY_ITEM Private Store Composition

Date: 2026-05-31
Unit of Work: UOW-1899
Status: Completed

## Scope

- Performed Work Discovery around Java `CM_BUY_ITEM` action `0`, Java `PrivateStoreService.sellStoreItem`, Java `PrivateStoreService.getBoughtItems`, C# `CmBuyItemHandlerCompositionPlanService`, `PrivateStoreBoughtItemsPlanService`, and `PrivateStorePurchasePlanService`.
- Connected the non-live private-store bought-items selector into the `CM_BUY_ITEM` player-target action `0` handler composition path.
- Added optional non-live purchase-plan payload carry-through for future composition without executing private-store mutation.
- Kept live socket handlers, live private-store mutation, inventory mutation, Kinah transfer, item cloning, packet sends, audit/log side effects, repository writes, Java runtime capture, and real client validation out of scope.

## What Changed

- Added `SelectedPrivateStorePlanner` handler composition status.
- Added `InvokePrivateStorePlanner` handler composition step.
- Added `PrivateStoreItems` and optional `PrivateStorePurchasePlan` to `CmBuyItemHandlerCompositionInput`.
- Added `PrivateStoreBoughtItemsPlan` and `PrivateStorePurchasePlan` outputs to `CmBuyItemHandlerCompositionPlan`.
- Updated player-target action `0` handling to invoke `PrivateStoreBoughtItemsPlanService` using parsed packet items and supplied private-store item summaries.
- Updated focused handler composition tests for successful selector output, blocked selector output, optional purchase-plan carry-through, and non-private-store player action isolation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~PrivateStoreBoughtItemsPlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests|FullyQualifiedName~PrivateStoreOpenPlanServiceTests|FullyQualifiedName~PrivateStoreOpenGuardPlanServiceTests|FullyQualifiedName~PrivateStoreItemValidationPlanServiceTests|FullyQualifiedName~PrivateStoreClosePlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused handler/private-store selector slice passed with 22 tests.
- Related private-store/`CM_BUY_ITEM` composition slice passed with 117 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4859 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- `CmBuyItemHandlerCompositionPlanService` remains non-live and is not invoked by live `GameServerConnection`.
- `PrivateStoreBoughtItemsPlanService` and `PrivateStorePurchasePlanService` remain non-live and do not mutate inventory or Kinah.
- Java private-store live state, insertion-order source map, seller/buyer online and race state, inventory state, Kinah balances, item cloning, packet sends, audit logging, logging side effects, and close-store mutation remain represented by supplied facts or separate non-live planners.

## Parity Table Updates

- Added Session 1899 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` player-target action `0` dispatch composition
  - `PrivateStoreService.sellStoreItem` non-live payload composition
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java pet merchant action `17` sell-rate branch and add a gap-scoped non-live planner or payload bridge for `TradeService.performSellToShop(player, tradeList, null, pf.getRatePrice())`.

Safe alternative candidates:

- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Add a disabled private-store live-executor facade plan that consumes the selector and purchase plans without mutating state.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
