# Phase 6 Session 1920 Handoff - AP-Sell Disabled Outcome Composition

Date: 2026-05-31
Unit of Work: UOW-1920
Status: Completed

## What Changed

- Composed selected NPC action `1` ABYSS AP-sell handler plans into disabled `TradeSellForApToShopOutcomePlan` diagnostics.
- Added optional `TradeSellForApToShopPlan` forwarding from handler composition input into `CmBuyItemSellToShopCompositionInput`.
- Added `SellForApToShopOutcomeCreated` and `SellForApToShopOutcomePlan` to `CmBuyItemSideEffectOutcomePlanService`.
- Added disabled AP-sell outcome planning for successful AP-sell plans, blocked plans, missing plans, and the Java disabled-feature message branch.
- Added high-level disabled flags for persistence, seller inventory mutation, AP mutation, packet sends, and transaction-boundary intent.
- Updated AP-sell outcome and high-level `CM_BUY_ITEM` side-effect tests.
- Kept this strictly non-live. No live handler dispatch, inventory deletion, AP mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused AP-sell outcome composition slice passed with 49 tests.
- Related buy-item/trade/private-store/pet slice passed with 129 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4932 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The AP-sell outcome composition is disabled diagnostic plumbing only.
- Production socket processing still does not hydrate live AP-sell facts; selected NPC action `1` AP-sell diagnostics need injected plans for populated output.
- Normal sell-to-shop outcome composition remains outside this unit.
- Live inventory deletion, AP mutation, packet dispatch, transaction behavior, repository writes, and full action `1` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1920 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled AP-sell final outcome planning
  - high-level `CM_BUY_ITEM` AP-sell outcome composition
  - AP-sell diagnostic payload handoff wiring
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellForApToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemHandlerCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellForApToShopPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1920-Completion.md`
- `docs/Phase-6-Session-1920-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellForAPToShop`
  - Java `TradeService.performSellToShop`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `TradeSellForApToShopPlanService`
  - C# `TradeSellToShopPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
