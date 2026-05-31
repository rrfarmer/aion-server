# Phase 6 Session 1917 Completion - CM_BUY_ITEM Disabled Outcome Diagnostic Hook

Date: 2026-05-31
Unit of Work: UOW-1917
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl`.
- Inspected C# `GameServerConnection.HandleBuyItem`, the existing `CmBuyItemHandlerCompositionPlan` observer, `CmBuyItemSideEffectOutcomePlanService`, and socket-level buy-item tests.
- Confirmed the safe unit was observer-only diagnostic exposure, not live handler execution.

## What Changed

- Added an optional `CmBuyItemSideEffectOutcomePlan` observer to `GameServerConnection`.
- Updated `HandleBuyItem` to create the existing non-live handler composition plan when either the handler-plan observer or the new outcome observer is present.
- Invoked `CmBuyItemSideEffectOutcomePlanService.CreateDisabledPlan(plan)` for the new observer.
- Extended socket-level buy-item tests to assert diagnostic outcome plans for skipped branches and a known player-target action `0` branch.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused diagnostic/socket slice passed with 27 tests.
- Related buy-item/trade/private-store/pet slice passed with 86 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4918 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The new observer is disabled diagnostic plumbing only.
- `GameServerConnection` still does not hydrate live private-store, pet merchant, NPC template, repurchase, or inventory facts into the handler composition plan.
- Live private-store and pet merchant inventory mutations, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, and pet auto-sell notification behavior remain unwired.
- No live `CM_BUY_ITEM` path treats these diagnostic plans as verified Java execution.

## Parity Table Updates

- Added Session 1917 rows in `docs/PHASE-6-PROGRESS.md` for:
  - socket-level disabled `CM_BUY_ITEM` side-effect outcome diagnostic hook
  - player action `0` diagnostic outcome path from `GameServerConnection`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add source-reviewed disabled outcome/detail planning for a remaining `CM_BUY_ITEM` branch, preferably buy-from-shop or AP-sell, before live execution is attempted.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
