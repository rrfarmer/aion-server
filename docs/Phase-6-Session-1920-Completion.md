# Phase 6 Session 1920 Completion - AP-Sell Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1920
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, latest completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `1`.
- Inspected Java `TradeService.performSellForAPToShop`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `CmBuyItemSellToShopCompositionPlanService`, `CmBuyItemSideEffectOutcomePlanService`, and `TradeSellForApToShopPlanService`.
- Confirmed the safe unit was disabled AP-sell outcome composition over the existing AP-sell plan, not live AP-sell execution.

## What Changed

- Added optional `TradeSellForApToShopPlan` payload wiring through `CmBuyItemHandlerCompositionInput` into the action `1` sell-to-shop composition branch.
- Added `TradeSellForApToShopOutcomePlanService` with disabled final outcome planning for AP-sell plans.
- Extended `CmBuyItemSideEffectOutcomePlanService` with a `SellForApToShopOutcomeCreated` branch for selected action `1` ABYSS AP-sell handler plans.
- Added high-level disabled outcome flags for persistence, seller inventory mutation, AP mutation, packet sends, and transaction-boundary intent.
- Covered the Java disabled-feature message branch separately from mutation-ready plans.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, inventory deletion, AP mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused AP-sell outcome composition slice passed with 49 tests after one assertion-only correction in the new disabled-message outcome test.
- Related buy-item/trade/private-store/pet slice passed with 129 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4932 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The AP-sell outcome composition is disabled diagnostic plumbing only.
- Production socket processing still does not hydrate live AP-sell facts, so populated AP-sell diagnostics require an injected plan.
- Normal sell-to-shop outcome composition remains outside this unit.
- Live inventory deletion, AP mutation, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1920 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled AP-sell final outcome planning
  - high-level `CM_BUY_ITEM` AP-sell outcome composition
  - AP-sell diagnostic payload handoff wiring
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add production-safe AP-sell fact hydration for selected NPC action `1` diagnostics without dispatching live effects, or, if ownership is too broad, add normal sell-to-shop disabled outcome composition over `TradeSellToShopPlan`.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
