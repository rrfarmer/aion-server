# Phase 6 Session 1919 Handoff - Buy-From-Shop Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1919
Status: Completed

## What Changed

- Composed selected NPC buy-from-shop handler plans into disabled `TradeBuyTransactionOutcomePlan` diagnostics.
- Added optional `TradeBuyTransactionPlan` forwarding from handler composition input into `CmBuyItemBuyFromShopCompositionInput`.
- Added `BuyFromShopOutcomeCreated` and `BuyFromShopOutcomePlan` to `CmBuyItemSideEffectOutcomePlanService`.
- Added high-level disabled flags for persistence, buyer inventory mutation, Kinah mutation, packet sends, audit logging, and transaction-boundary intent.
- Updated side-effect outcome and socket-level buy-item tests for successful, missing-plan, and audit branches.
- Kept this strictly non-live. No live handler dispatch, AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, transaction commit, rollback, packet dispatch, audit write, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-from-shop outcome composition slice passed with 58 tests.
- Related buy-item/trade/private-store/pet slice passed with 123 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4926 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The buy-from-shop outcome composition is disabled diagnostic plumbing only.
- Production socket processing still does not hydrate live buy-transaction facts; selected NPC buy-from-shop diagnostics currently surface missing-transaction output unless tests inject a plan.
- Live AP mutation, Kinah mutation, required-item deletion, item add, limited-item counter mutation, packet dispatch, audit logging, transaction/rollback behavior, repository writes, and full action `13`-`16` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1919 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled buy-from-shop high-level outcome composition
  - buy transaction diagnostic payload handoff wiring
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1919-Completion.md`
- `docs/Phase-6-Session-1919-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellForAPToShop`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performBuyTransaction`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `TradeSellForApToShopPlanService`
  - C# `TradeBuyTransactionPlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
