# Phase 6 Session 1922 Completion - Socket Sell Action Missing Outcome Diagnostic

Date: 2026-05-31
Unit of Work: UOW-1922
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 completion, and latest handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `1`.
- Inspected C# `GameServerConnection.HandleBuyItem`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `CmBuyItemSideEffectOutcomePlanService`, and socket-level buy-item tests.
- Confirmed production socket diagnostics currently do not hydrate trade templates or inventory facts, so action `1` fact hydration was too broad for a safe unit.

## What Changed

- Added socket-level diagnostic coverage for known NPC `CM_BUY_ITEM` action `1`.
- The new test verifies the connection selects the non-live sell-to-shop composition branch.
- The new test verifies the disabled high-level outcome is `SellToShopOutcomeCreated` with `MissingSellToShopPlan`.
- The new test verifies no packet sends or live side effects occur.
- Kept this work non-live. No trade-template hydration, AP-sell template classification, inventory snapshot hydration, sell plan creation, inventory mutation, repurchase mutation, Kinah mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused socket/trade diagnostic slice passed with 69 tests.
- Related buy-item/trade/private-store/pet slice passed with 135 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4938 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- This is socket-level diagnostic coverage only; live `CM_BUY_ITEM` action `1` execution remains disabled.
- Production socket processing still lacks safe sell/AP-sell fact hydration, including trade-template lookup, ABYSS purchase-template classification, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, and AP state.
- Live inventory mutation, repurchase mutation, AP/Kinah mutation, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1922 rows in `docs/PHASE-6-PROGRESS.md` for:
  - socket diagnostic handling of `CM_BUY_ITEM` action `1`
  - current sell-to-shop missing-plan fact boundary
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add a production-safe, disabled fact-hydration adapter for action `1` sell/AP-sell diagnostics, starting with trade-template classification only if it can be read without live mutation.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
