# Phase 6 Session 1916 Completion - Pet Merchant Disabled Persistence/Send Adapters

Date: 2026-05-31
Unit of Work: UOW-1916
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` pet action `17`.
- Inspected Java `TradeService.performSellToShop` and `PetService.sell`.
- Inspected C# `TradeSellToShopPlanService`, `PetMerchantSellLiveExecutorFacadePlanService`, `PetMerchantSellOutcomePlanService`, and `CmBuyItemSideEffectOutcomePlanService`.
- Confirmed the safe unit was disabled adapter planning over existing sell-to-shop output, not live pet merchant execution.

## What Changed

- Added `PetMerchantSellPersistenceAdapterPlanService`.
- Added `PetMerchantSellSendAdapterPlanService`.
- Added pet merchant persistence operation and send intent records plus status/kind enums.
- Updated `PetMerchantSellLiveExecutorFacadePlan` to carry disabled persistence and send adapters.
- Updated `PetMerchantSellOutcomePlan` to group disabled persistence, send, and transaction-boundary intent.
- Updated `CmBuyItemSideEffectOutcomePlanService` so pet merchant action `17` reports disabled send intent presence.
- Documented the Java distinction that `PetService.sell` sends `STR_MSG_MERCHANT_PET_GET_SELL_ITEM`, while direct `CM_BUY_ITEM` pet action `17` only calls `TradeService.performSellToShop`.
- Kept this work non-live. No live `CM_BUY_ITEM` handler dispatch, seller inventory mutation, repurchase mutation, Kinah mutation, packet dispatch, transaction commit, rollback, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet merchant adapter/sell/outcome slice passed with 24 tests.
- Related buy-item/trade/private-store/pet slice passed with 85 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4917 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The pet merchant persistence/send adapters are disabled diagnostic plumbing only.
- Live seller inventory mutation, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, and pet auto-sell notification behavior remain unwired.
- `PetService.sell` auto-sell notification is a separate Java path and was not ported into `CM_BUY_ITEM` action `17`.
- No live `CM_BUY_ITEM` path treats these adapter plans as verified Java execution.

## Parity Table Updates

- Added Session 1916 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled pet merchant sell persistence adapter
  - disabled pet merchant sell send adapter
  - disabled pet merchant final side-effect boundary with adapter plans
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: expose the disabled `CmBuyItemSideEffectOutcomePlanService` through a safe `GameServerConnection` diagnostic hook or handler-composition diagnostic path without enabling live side effects.

Safe alternative candidates:

- Add equivalent disabled send/persistence detail to any remaining `CM_BUY_ITEM` buy-from-shop or AP-sell outcome plans before live execution.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
