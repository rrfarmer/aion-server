# Phase 6 Session 1919 Completion - Buy-From-Shop Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1919
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` actions `13`-`16`.
- Inspected Java `TradeService.performBuyFromShop` and `TradeService.performBuyTransaction`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `CmBuyItemBuyFromShopCompositionPlanService`, `CmBuyItemSideEffectOutcomePlanService`, and `TradeBuyTransactionOutcomePlanService`.
- Confirmed the safe unit was high-level disabled outcome composition over the existing buy transaction plan, not live buy-from-shop execution.

## What Changed

- Added optional `TradeBuyTransactionPlan` payload wiring through `CmBuyItemHandlerCompositionInput` into the selected buy-from-shop dispatch plan.
- Extended `CmBuyItemSideEffectOutcomePlanService` with a `BuyFromShopOutcomeCreated` branch for selected NPC buy-from-shop handler plans.
- Added high-level disabled outcome access to the composed `TradeBuyTransactionOutcomePlan`.
- Added high-level flags for disabled persistence, buyer inventory mutation, Kinah mutation, packet sends, audit logging, and transaction-boundary intent.
- Updated socket-level buy-item tests so selected NPC buy-from-shop diagnostics surface a missing-transaction outcome while unknown-target diagnostics remain non-outcome terminal plans.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, packet dispatch, audit write, transaction commit, rollback, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-from-shop outcome composition slice passed with 58 tests.
- Related buy-item/trade/private-store/pet slice passed with 123 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4926 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The buy-from-shop outcome composition is disabled diagnostic plumbing only.
- Live AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, packet dispatch, audit logging, transaction/rollback behavior, repository writes, and real client behavior remain unwired.
- Production socket processing still lacks live buy-transaction fact hydration, so selected NPC buy-from-shop diagnostics surface the missing-transaction branch unless tests inject a transaction plan.
- No live `CM_BUY_ITEM` path treats these outcome plans as verified Java execution.

## Parity Table Updates

- Added Session 1919 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled buy-from-shop high-level outcome composition
  - buy transaction diagnostic payload handoff wiring
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled AP-sell outcome planning over `TradeSellForApToShopPlan`, then compose it into the high-level `CM_BUY_ITEM` side-effect outcome surface without enabling live execution.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
