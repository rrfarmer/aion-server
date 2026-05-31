# Phase 6 Session 1915 Completion - CM_BUY_ITEM Disabled Side-Effect Outcome Composer

Date: 2026-05-31
Unit of Work: UOW-1915
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `0` and action `17`.
- Inspected Java `PrivateStoreService.sellStoreItem` and `TradeService.performSellToShop`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `PrivateStorePurchaseOutcomePlanService`, `PetMerchantSellOutcomePlanService`, and related tests.
- Confirmed the safe unit was a separate disabled diagnostic composer over handler composition, not direct handler-plan mutation or live execution.

## What Changed

- Added `CmBuyItemSideEffectOutcomePlanService`.
- Added `CmBuyItemSideEffectOutcomePlan` and `CmBuyItemSideEffectOutcomePlanStatus`.
- The service accepts a `CmBuyItemHandlerCompositionPlan` and composes the private-store final outcome for Player action `0` or pet merchant final outcome for Pet action `17`.
- Added tests for successful private-store composition, successful pet merchant composition, blocked private-store terminal composition, non-eligible handlers, and missing handlers.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused side-effect outcome composer slice passed with 5 tests.
- Related buy-item/trade/private-store/pet slice passed with 82 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4914 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The side-effect outcome composer is disabled diagnostic plumbing only.
- Live private-store and pet merchant inventory mutations, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, and pet auto-sell notification behavior remain unwired.
- No live `CM_BUY_ITEM` path treats this side-effect outcome composer as verified Java execution.

## Parity Table Updates

- Added Session 1915 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled `CM_BUY_ITEM` side-effect outcome composer
  - disabled `CM_BUY_ITEM` side-effect outcome summary plan
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt, or explicitly document why the shared sell-to-shop plan covers enough of the persistence boundary and what send/notification gaps remain.

Safe alternative candidates:

- Inspect whether the disabled `CmBuyItemSideEffectOutcomePlanService` can be exposed through `GameServerConnection` diagnostic hooks without enabling live execution.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
