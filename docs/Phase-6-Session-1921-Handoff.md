# Phase 6 Session 1921 Handoff - Sell-To-Shop Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1921
Status: Completed

## What Changed

- Composed selected NPC action `1` non-ABYSS sell-to-shop handler plans into disabled `TradeSellToShopOutcomePlan` diagnostics.
- Added `SellToShopOutcomeCreated` and `SellToShopOutcomePlan` to `CmBuyItemSideEffectOutcomePlanService`.
- Added disabled normal sell-to-shop outcome planning for successful plans, blocked plans, missing plans, and the Java not-sellable system-message branch.
- Added high-level disabled flags for persistence, seller inventory mutation, repurchase-item mutation, Kinah mutation, packet sends, and transaction-boundary intent.
- Updated normal sell-to-shop outcome and high-level `CM_BUY_ITEM` side-effect tests.
- Kept this strictly non-live. No live handler dispatch, inventory deletion/decrease, repurchase mutation, Kinah mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused sell-to-shop outcome composition slice passed with 51 tests.
- Related buy-item/trade/private-store/pet slice passed with 134 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4937 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The normal sell-to-shop outcome composition is disabled diagnostic plumbing only.
- Production socket processing still does not hydrate live sell-to-shop facts; selected NPC action `1` sell diagnostics need injected plans for populated output.
- Java count-exceeds audit logging and exact transaction/rollback behavior remain outside this outcome-only unit.
- Live inventory mutation, repurchase mutation, Kinah mutation, packet dispatch, transaction behavior, repository writes, and full action `1` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1921 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled normal sell-to-shop final outcome planning
  - high-level `CM_BUY_ITEM` normal sell-to-shop outcome composition
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellToShopPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1921-Completion.md`
- `docs/Phase-6-Session-1921-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellToShop`
  - Java `TradeService.performSellForAPToShop`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `TradeSellToShopPlanService`
  - C# `TradeSellForApToShopPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
