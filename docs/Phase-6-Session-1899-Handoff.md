# Phase 6 Session 1899 Handoff - CM_BUY_ITEM Private Store Composition

Date: 2026-05-31
Unit of Work: UOW-1899
Status: Completed

## What Changed

- Updated `CmBuyItemHandlerCompositionPlanService` to select a non-live private-store planner for player-target action `0`.
- Added `SelectedPrivateStorePlanner` status and `InvokePrivateStorePlanner` step.
- Added `PrivateStoreItems` and optional `PrivateStorePurchasePlan` to handler composition input.
- Added `PrivateStoreBoughtItemsPlan` and `PrivateStorePurchasePlan` outputs to handler composition plans.
- The player-target action `0` branch now invokes `PrivateStoreBoughtItemsPlanService` over parsed `CmBuyItem.Items` and supplied private-store summaries.
- Updated handler composition tests for successful selection, blocked selection, optional purchase-plan carry-through, and non-action-0 player branch isolation.
- Kept this work non-live. No socket handler, live private-store mutation, inventory mutation, Kinah transfer, item clone/add, packet send, audit/log side effect, repository write, Java runtime capture, or real client validation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemHandlerCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PrivateStoreBoughtItemsPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1899-Completion.md`
- `docs/Phase-6-Session-1899-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM` trade branch work, inspect:
  - Java `CM_BUY_ITEM` action `17`
  - Java pet merchant function/rate-price lookup
  - Java `TradeService.performSellToShop(player, tradeList, null, pf.getRatePrice())`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `TradeSellToShopPlanService`
  - C# pet model/function summaries, if present
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
