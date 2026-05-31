# Phase 6 Session 1914 Completion - Pet Merchant Final Outcome Plan

Date: 2026-05-31
Unit of Work: UOW-1914
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `17`, Java `PetService.sell`, and Java `TradeService.performSellToShop`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `TradeSellToShopPlanService`, `PetMerchantSellLiveExecutorFacadePlanService`, and existing pet merchant sell tests.
- Confirmed the next safe unit was a disabled final outcome/transaction-boundary planner over the existing disabled pet merchant facade and sell-to-shop plan, not live side-effect dispatch.

## What Changed

- Added `PetMerchantSellOutcomePlanService`.
- Added `PetMerchantSellOutcomePlan`, `PetMerchantSellOutcomeStepPlan`, `PetMerchantSellOutcomePlanStatus`, and `PetMerchantSellOutcomeStepKind`.
- The outcome plan groups the disabled pet merchant facade and sell-to-shop plan into one opt-in result with inventory, repurchase, Kinah, and transaction-boundary intents.
- Added tests for successful disabled outcome grouping plus missing and blocked facade terminal paths.
- Kept this work non-live. No inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet construction, packet dispatch, repository write, Java runtime output, real client validation, or live `CM_BUY_ITEM` pet merchant execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet merchant sell slice passed with 32 tests.
- Related buy-item/trade/private-store/pet slice passed with 77 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4909 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The final outcome and facade plans are disabled descriptors only.
- Java transaction behavior for pet merchant sell is not runtime-verified; the transaction boundary is recorded only as a disabled intent.
- Live inventory mutation, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, pet auto-sell notification behavior, and real pet merchant sell execution remain unwired.
- No live `CM_BUY_ITEM` path treats this final outcome plan as verified Java pet merchant side effects.

## Parity Table Updates

- Added Session 1914 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled final pet merchant sell outcome/transaction plan service
  - disabled final pet merchant sell outcome plan
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect whether `CM_BUY_ITEM` action `0` and action `17` can expose their disabled final outcome plans through handler composition or a higher-level diagnostic result without enabling live execution.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
