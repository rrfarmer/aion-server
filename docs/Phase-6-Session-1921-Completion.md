# Phase 6 Session 1921 Completion - Sell-To-Shop Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1921
Status: Completed

## Work Discovery

- Re-read latest Phase 6 handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `1`.
- Inspected Java `TradeService.performSellToShop`.
- Inspected C# `TradeSellToShopPlanService`, `CmBuyItemSellToShopCompositionPlanService`, and `CmBuyItemSideEffectOutcomePlanService`.
- Confirmed the safe unit was disabled normal sell-to-shop outcome composition over the existing sell-to-shop plan, not live sell execution.

## What Changed

- Added `TradeSellToShopOutcomePlanService` with disabled final outcome planning for normal sell-to-shop plans.
- Extended `CmBuyItemSideEffectOutcomePlanService` with a `SellToShopOutcomeCreated` branch for selected NPC action `1` non-ABYSS handler plans.
- Added high-level disabled outcome access to the composed `TradeSellToShopOutcomePlan`.
- Added high-level disabled flags for persistence, seller inventory mutation, repurchase-item mutation, Kinah mutation, packet sends, and transaction-boundary intent.
- Covered the Java not-sellable system-message branch separately from mutation-ready plans.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, inventory deletion/decrease, repurchase mutation, Kinah mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused sell-to-shop outcome composition slice passed with 51 tests.
- Related buy-item/trade/private-store/pet slice passed with 134 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4937 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The normal sell-to-shop outcome composition is disabled diagnostic plumbing only.
- Production socket processing still does not hydrate live sell-to-shop facts, so populated diagnostics require an injected plan.
- Java count-exceeds audit logging and exact transaction/rollback behavior remain outside this outcome-only unit.
- Live inventory mutation, repurchase mutation, Kinah mutation, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1921 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled normal sell-to-shop final outcome planning
  - high-level `CM_BUY_ITEM` normal sell-to-shop outcome composition
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: hydrate production-safe action `1` sell/AP-sell facts into the diagnostic path without dispatching live effects, or add socket-level tests showing normal/AP sell outcomes remain missing-plan diagnostics until fact hydration exists.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
