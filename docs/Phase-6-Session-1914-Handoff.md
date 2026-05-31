# Phase 6 Session 1914 Handoff - Pet Merchant Final Outcome Plan

Date: 2026-05-31
Unit of Work: UOW-1914
Status: Completed

## What Changed

- Added `PetMerchantSellOutcomePlanService`.
- Added `PetMerchantSellOutcomePlan`, `PetMerchantSellOutcomeStepPlan`, `PetMerchantSellOutcomePlanStatus`, and `PetMerchantSellOutcomeStepKind`.
- The final outcome plan groups the disabled pet merchant facade and sell-to-shop plan into one opt-in result with disabled inventory, repurchase, Kinah, and transaction-boundary intents.
- Added focused tests for successful disabled outcome grouping, missing facade terminal behavior, and blocked facade terminal behavior.
- Kept this strictly non-live. No inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet construction, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet merchant sell slice passed with 32 tests.
- Related buy-item/trade/private-store/pet slice passed with 77 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4909 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The final outcome and facade plans are disabled descriptor plans only.
- Java transaction behavior for pet merchant sell is not runtime-verified; the transaction boundary is only a disabled intent.
- Live inventory mutation, repurchase state, Kinah mutation, packet dispatch, transactions, rollback behavior, repository writes, pet auto-sell notification behavior, and full pet merchant action `17` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` pet merchant paths still must not be treated as verified Java purchase execution.

## Parity Table Updates

- Added Session 1914 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled final pet merchant sell outcome/transaction plan service
  - disabled final pet merchant sell outcome plan
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect whether `CM_BUY_ITEM` action `0` and action `17` can expose their disabled final outcome plans through handler composition or a higher-level diagnostic result without enabling live execution.

Safe alternative candidates:

- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetMerchantSellLiveExecutorFacadePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1914-Completion.md`
- `docs/Phase-6-Session-1914-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl` action `0` and action `17`
  - Java `PrivateStoreService.sellStoreItem`
  - Java `TradeService.performSellToShop`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `PrivateStorePurchaseOutcomePlanService`
  - C# `PetMerchantSellOutcomePlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
