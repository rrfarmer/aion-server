# Phase 6 Session 1929 Handoff - Buy Transaction Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1929
Status: Completed

## What Changed

- Preserved Java acquisition `type`, required item ID, and required item count in `ItemTemplateSummary` and static-data parsing.
- `GameServerConnection` buy-from-shop diagnostics now resolve ordinary NPC trade-list templates for actions `13`-`16`.
- Socket diagnostics now validate packet item IDs against ordinary goods-list rows before building disabled buy transaction plans.
- The existing `TradeBuyTransactionPlanService` is now hydrated from socket-visible facts for selected NPC buy-from-shop diagnostics.
- Added socket regression coverage for a normal NPC buy-from-shop request that records disabled Kinah, AP, required-item, persistence-intent, and packet-intent diagnostics.
- Added real Java static-data loader assertions proving representative acquisition type/item/count metadata is preserved.
- Kept this strictly non-live. No live item add/delete, AP/Kinah mutation, limited-item mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/trade transaction slice first failed because `GoodsListItemSummary` uses `Id`, not `ItemId`; after correction, it passed with 84 tests.
- Focused buy-item/trade transaction slice plus real static-data loader test passed with 85 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4956 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` buy-from-shop execution remains disabled.
- Diagnostic limited-item availability currently defaults to allowed; live `LimitedItemTradeService.getLimitedItem` counters are not wired.
- The socket hydration uses the existing deterministic planner price inputs and still does not prove full Java `PricesService.getBuyPrice(price, race)` global influence/tax parity.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Live AP/Kinah/item mutation, item-add overflow behavior, repository writes, transaction boundaries, and packet fanout remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1929 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `Acquisition`
  - `TradeList.calculateBuyListPrice`
  - `TradeList.calculateAbyssRewardBuyList`
  - `TradeService.performBuyTransaction`
  - `TradeService.performBuyFromShop`
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled limited-item fact hydration for buy-from-shop diagnostics by reusing the existing dialog limited-item fact adapter where safe.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add Java-runtime golden capture for `CM_BUY_ITEM` once compatible Java and Maven are available.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1929-Completion.md`
- `docs/Phase-6-Session-1929-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `TradeService.canBuyLimitItem`
  - Java `LimitedItemTradeService.getLimitedItem`
  - Java `LimitedItem`
  - C# `NpcDialogLimitedItemFactAdapterService`
  - C# `TradeBuyTransactionPlanService`
  - C# `GameServerConnection.ResolveBuyItemBuyTransactionPlan`
  - C# `GoodsListItemSummary.IsLimitedItem`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
