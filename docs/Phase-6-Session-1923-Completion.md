# Phase 6 Session 1923 Completion - Buy Item Sell Classification Adapter

Date: 2026-05-31
Unit of Work: UOW-1923
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` action `1`.
- Inspected Java `TradeListData.getPurchaseTemplate`.
- Inspected C# `TradeListTable.GetPurchaseTemplate`.
- Inspected C# `GameServerConnection.HandleBuyItem`, `CmBuyItemHandlerCompositionPlanService`, `CmBuyItemSellToShopCompositionPlanService`, and buy-item sell outcome tests.
- Confirmed Java action `1` selects AP-sell only when `npc.canBuy() || npc.canPurchase()` is true and the purchase template type is `TradeNpcType.ABYSS`; missing or non-ABYSS purchase templates use normal sell-to-shop.

## What Changed

- Added `CmBuyItemSellActionFactAdapterService` as a disabled, read-only fact adapter for `CM_BUY_ITEM` action `1`.
- The adapter records the Java can-buy/can-purchase guard, purchase-template lookup, and ABYSS-vs-normal sell dispatch classification.
- The adapter exposes whether downstream diagnostics should hydrate normal sell-to-shop or AP-sell plans, while keeping live side effects disabled.
- Added unit tests for:
  - NPCs that cannot buy or purchase.
  - ABYSS purchase-template classification to AP sell.
  - missing and NORMAL purchase-template classification to normal sell-to-shop.
- Kept this work non-live. No socket wiring, sell plan creation, inventory snapshot hydration, goods-list validation, sell-limit state, Kinah state, AP state, repurchase state, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused adapter/composition slice passed with 32 tests.
- Related buy-item sell diagnostic slice passed with 73 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` first timed out before reporting results at 184 seconds, then passed on rerun with a longer timeout with 4942 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The adapter is disabled diagnostic plumbing only and is not wired into `GameServerConnection.HandleBuyItem`.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Sell-to-shop/AP-sell plan hydration, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, AP state, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1923 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled `CM_BUY_ITEM` action `1` sell/AP-sell branch classification
  - static purchase-template classification test coverage
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: wire the read-only sell/AP-sell classification adapter into `GameServerConnection.HandleBuyItem` diagnostics only if the static trade-list table can be supplied safely, without sell-plan hydration or live mutation.

Safe alternative candidates:

- Add tests proving adapter output composes correctly with `CmBuyItemHandlerCompositionPlanService` using supplied purchase-template facts.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
