# Phase 6 Session 1917 Handoff - CM_BUY_ITEM Disabled Outcome Diagnostic Hook

Date: 2026-05-31
Unit of Work: UOW-1917
Status: Completed

## What Changed

- Added an optional `CmBuyItemSideEffectOutcomePlan` observer to `GameServerConnection`.
- Updated `HandleBuyItem` so it builds the existing non-live handler composition plan when either buy-item diagnostic observer is present.
- The new observer receives `CmBuyItemSideEffectOutcomePlanService.CreateDisabledPlan(plan)`.
- Extended `GameServerConnectionBuyItemTests` to assert disabled outcome diagnostics for skipped branches and a known player-target action `0` diagnostic path.
- Kept this strictly non-live. No live handler dispatch, inventory mutation, repurchase mutation, Kinah mutation, transaction commit, rollback, packet dispatch, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused diagnostic/socket slice passed with 27 tests.
- Related buy-item/trade/private-store/pet slice passed with 86 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4918 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The observer is disabled diagnostic plumbing only.
- `GameServerConnection` still supplies only target-kind facts to `CmBuyItemHandlerCompositionPlanService`; live private-store item lists, purchase plans, pet merchant sell plans, NPC trade templates, repurchase facts, and inventory snapshots are not hydrated from socket state.
- Live private-store and pet merchant inventory mutations, repurchase state, Kinah mutation, packet dispatch, transaction/rollback behavior, repository writes, and full action `0`/`17` execution remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1917 rows in `docs/PHASE-6-PROGRESS.md` for:
  - socket-level disabled `CM_BUY_ITEM` side-effect outcome diagnostic hook
  - player action `0` diagnostic outcome path from `GameServerConnection`
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add source-reviewed disabled outcome/detail planning for a remaining `CM_BUY_ITEM` branch, preferably buy-from-shop or AP-sell, before live execution is attempted.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSideEffectOutcomePlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1917-Completion.md`
- `docs/Phase-6-Session-1917-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performSellForAPToShop`
  - Java `PetService.sell`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# `TradeBuyTransactionPlanService`
  - C# `TradeSellForApToShopPlanService`
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
