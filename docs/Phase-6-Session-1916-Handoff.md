# Phase 6 Session 1916 Handoff - Pet Merchant Disabled Persistence/Send Adapters

Date: 2026-05-31
Unit of Work: UOW-1916
Status: Completed

## What Changed

- Added disabled persistence and send adapter plans for pet merchant sell outputs from `TradeSellToShopPlan`.
- Added `PetMerchantSellPersistenceAdapterPlanService` and `PetMerchantSellSendAdapterPlanService`.
- Updated `PetMerchantSellLiveExecutorFacadePlan` to carry disabled adapter plans.
- Updated `PetMerchantSellOutcomePlan` to group persistence, send, and transaction-boundary steps.
- Updated `CmBuyItemSideEffectOutcomePlanService` so pet merchant action `17` reports disabled send intent presence.
- Added tests for successful adapter plans, terminal adapter plans, facade adapter composition, final outcome grouping, and high-level CM_BUY_ITEM outcome reporting.
- Kept this strictly non-live. No live handler dispatch, seller inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet merchant adapter/sell/outcome slice passed with 24 tests.
- Related buy-item/trade/private-store/pet slice passed with 85 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4917 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The adapter plans are disabled diagnostic plumbing only.
- Live pet merchant inventory mutation, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, and full action `17` execution remain unwired.
- `PetService.sell` auto-sell notification behavior remains separate from direct `CM_BUY_ITEM` pet action `17` and is not implemented as live behavior.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1916 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled pet merchant sell persistence adapter
  - disabled pet merchant sell send adapter
  - disabled pet merchant final side-effect boundary with adapter plans
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: expose the disabled `CmBuyItemSideEffectOutcomePlanService` through a safe `GameServerConnection` diagnostic hook or handler-composition diagnostic path without enabling live side effects.

Safe alternative candidates:

- Add equivalent disabled send/persistence detail to any remaining `CM_BUY_ITEM` buy-from-shop or AP-sell outcome plans before live execution.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetMerchantSellLiveExecutorFacadePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1916-Completion.md`
- `docs/Phase-6-Session-1916-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl` action `0` and action `17`
  - Java `PrivateStoreService.sellStoreItem`
  - Java `TradeService.performSellToShop`
  - Java `PetService.sell`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `PrivateStorePurchaseOutcomePlanService`
  - C# `PetMerchantSellOutcomePlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
