# Phase 6 Session 1915 Handoff - CM_BUY_ITEM Disabled Side-Effect Outcome Composer

Date: 2026-05-31
Unit of Work: UOW-1915
Status: Completed

## What Changed

- Added `CmBuyItemSideEffectOutcomePlanService`.
- Added `CmBuyItemSideEffectOutcomePlan` and `CmBuyItemSideEffectOutcomePlanStatus`.
- The composer accepts a `CmBuyItemHandlerCompositionPlan` and creates either:
  - the private-store final outcome for Player action `0`
  - the pet merchant final outcome for Pet action `17`
- Added focused tests for successful private-store composition, successful pet merchant composition, blocked private-store terminal composition, non-eligible handlers, and missing handlers.
- Kept this strictly non-live. No live handler dispatch, inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused side-effect outcome composer slice passed with 5 tests.
- Related buy-item/trade/private-store/pet slice passed with 82 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4914 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The side-effect outcome composer is disabled diagnostic plumbing only.
- Live private-store and pet merchant inventory mutations, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, pet auto-sell notification behavior, and full action `0`/`17` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1915 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled `CM_BUY_ITEM` side-effect outcome composer
  - disabled `CM_BUY_ITEM` side-effect outcome summary plan
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt, or explicitly document why the shared sell-to-shop plan covers enough of the persistence boundary and what send/notification gaps remain.

Safe alternative candidates:

- Inspect whether the disabled `CmBuyItemSideEffectOutcomePlanService` can be exposed through `GameServerConnection` diagnostic hooks without enabling live execution.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1915-Completion.md`
- `docs/Phase-6-Session-1915-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl` action `0` and action `17`
  - Java `PrivateStoreService.sellStoreItem`
  - Java `TradeService.performSellToShop`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `PrivateStorePurchaseOutcomePlanService`
  - C# `PetMerchantSellOutcomePlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
