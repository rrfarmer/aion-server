# Phase 6 Session 1923 Handoff - Buy Item Sell Classification Adapter

Date: 2026-05-31
Unit of Work: UOW-1923
Status: Completed

## What Changed

- Added a disabled, read-only `CmBuyItemSellActionFactAdapterService` for `CM_BUY_ITEM` action `1`.
- Captured the Java source control flow:
  - check `npc.canBuy() || npc.canPurchase()`
  - lookup `DataManager.TRADE_LIST_DATA.getPurchaseTemplate(npc.getNpcId())`
  - dispatch AP sell only for `TradeNpcType.ABYSS`
  - otherwise dispatch normal sell-to-shop
- Added unit coverage for NPCs that cannot buy/purchase, ABYSS purchase-template AP-sell classification, and missing/NORMAL purchase-template normal sell classification.
- Kept this strictly non-live. No socket wiring, sell plan hydration, inventory snapshot hydration, goods-list validation, sell-limit state, Kinah state, repurchase state, AP state, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused adapter/composition slice passed with 32 tests.
- Related buy-item sell diagnostic slice passed with 73 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` first timed out before reporting results at 184 seconds, then passed on rerun with a longer timeout with 4942 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- The new adapter is not wired into `GameServerConnection.HandleBuyItem`; production socket diagnostics still do not populate purchase-template classification.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Sell-to-shop/AP-sell plan hydration, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, AP state, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1923 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled action `1` sell/AP-sell branch classification
  - purchase-template classification test coverage
- All rows remain `Partial Parity`, `Unit Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellActionFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSellActionFactAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1923-Completion.md`
- `docs/Phase-6-Session-1923-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeListData.getPurchaseTemplate`
  - Java `TradeService.performSellToShop`
  - Java `TradeService.performSellForAPToShop`
  - C# `GameServerConnection.HandleBuyItem`
  - C# `CmBuyItemSellActionFactAdapterService`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemSideEffectOutcomePlanService`
  - C# static trade template data holders/loaders
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
