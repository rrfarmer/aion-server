# Phase 6 Session 1924 Completion - Socket Sell Classification Wiring

Date: 2026-05-31
Unit of Work: UOW-1924
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `1`.
- Inspected Java `TradeListData.getPurchaseTemplate`.
- Inspected C# `GameServerConnection.HandleBuyItem`.
- Inspected C# `CmBuyItemSellActionFactAdapterService`, `CmBuyItemHandlerCompositionPlanService`, `CmBuyItemSellToShopCompositionPlanService`, and buy-item socket diagnostics.
- Confirmed the socket diagnostic path could safely read a static `TradeListTable` without enabling sell/AP-sell mutations.

## What Changed

- Wired `CmBuyItemSellActionFactAdapterService` into `GameServerConnection.HandleBuyItem` diagnostics for NPC action `1`.
- The socket path now passes the adapter's `PurchaseTemplate` into the existing non-live handler composition plan.
- ABYSS purchase templates now classify socket diagnostics as disabled AP-sell outcomes.
- NORMAL purchase templates now classify socket diagnostics as disabled normal sell-to-shop outcomes.
- Added socket-level regression tests for both ABYSS and NORMAL purchase-template branches.
- Kept this work non-live. No `TradeSellToShopPlan`, `TradeSellForApToShopPlan`, inventory snapshot, goods-list validation, sell-limit state, Kinah state, AP state, repurchase state, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item socket/classification slice passed with 53 tests.
- Related buy-item/trade slice passed with 75 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4944 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults in this socket path.
- Sell-to-shop/AP-sell plan hydration, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, AP state, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1924 rows in `docs/PHASE-6-PROGRESS.md` for:
  - socket diagnostic action `1` purchase-template dispatch classification
  - ABYSS/NORMAL static purchase-template socket regression coverage
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled sell/AP-sell mutation fact hydration candidates for action `1`, starting with inventory item snapshots and goods-list lookup only if they can be read without enabling live mutation.

Safe alternative candidates:

- Expose NPC buy/purchase function facts to the socket diagnostic path if static function metadata can be proven equivalent to Java `npc.canBuy()` / `npc.canPurchase()`.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
