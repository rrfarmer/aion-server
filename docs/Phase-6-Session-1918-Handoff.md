# Phase 6 Session 1918 Handoff - Buy Transaction Disabled Outcome Boundaries

Date: 2026-05-31
Unit of Work: UOW-1918
Status: Completed

## What Changed

- Added disabled persistence/send/audit/final outcome plans for buy-from-shop transactions over `TradeBuyTransactionPlan`.
- Added `TradeBuyTransactionPersistenceAdapterPlanService`, `TradeBuyTransactionSendAdapterPlanService`, and `TradeBuyTransactionOutcomePlanService`.
- Added operation, intent, outcome-step, and final outcome record types plus status/kind enums.
- The persistence adapter records disabled AP, Kinah, required-item, bought-item, and limited-item counter write intents.
- The send adapter records disabled Java failure messages, negative-required-AP audit intent, and successful AP/Kinah/item packet intents.
- Added focused tests for success, blocked message/audit, and missing-plan terminal branches.
- Kept this strictly non-live. No live handler dispatch, AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, transaction commit, rollback, packet dispatch, audit write, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy transaction slice passed with 34 tests.
- Related buy-item/trade/private-store/pet slice passed with 120 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4923 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The buy transaction adapter/outcome plans are disabled diagnostic plumbing only.
- `CmBuyItemSideEffectOutcomePlanService` still does not compose buy-from-shop outcomes.
- Live AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, packet dispatch, audit logging, transaction/rollback behavior, repository writes, and full action `13`-`16` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1918 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled buy transaction persistence adapter
  - disabled buy transaction send/audit adapter
  - disabled buy transaction final outcome boundary
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: compose buy-from-shop disabled outcome plans into the high-level `CmBuyItemSideEffectOutcomePlanService` for selected `CM_BUY_ITEM` action `13`-`16` handler plans, still without enabling live execution.

Safe alternative candidates:

- Add disabled AP-sell outcome planning over `TradeSellForApToShopPlan`.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeBuyTransactionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeBuyTransactionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1918-Completion.md`
- `docs/Phase-6-Session-1918-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performBuyTransaction`
  - Java `TradeService.performSellForAPToShop`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `CmBuyItemBuyFromShopCompositionPlanService`
  - C# `TradeBuyTransactionPlanService`
  - C# `TradeSellForApToShopPlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
