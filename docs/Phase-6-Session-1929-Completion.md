# Phase 6 Session 1929 Completion - Buy Transaction Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1929
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `TradeService.performBuyFromShop` and `TradeService.performBuyTransaction`.
- Inspected Java `TradeList.calculateBuyListPrice` and `TradeList.calculateAbyssRewardBuyList`.
- Inspected Java `Acquisition` / `AcquisitionType`.
- Inspected C# `TradeBuyTransactionPlanService`, `CmBuyItemBuyFromShopCompositionPlanService`, `GameServerConnection.HandleBuyItem`, `TradeListTable`, `GoodsListTable`, and item-template static-data parsing.

## What Changed

- Preserved Java acquisition `type`, required item ID, and required item count in `ItemTemplateSummary`.
- Updated static-data parsing so `<acquisition type="..." item="..." count="..." ap="..."/>` keeps all buy-transaction source facts needed by Java `TradeList.calculateAbyssRewardBuyList`.
- Wired `GameServerConnection` buy-from-shop diagnostics to resolve the NPC ordinary trade-list template for actions `13`-`16`.
- Added diagnostic goods-list membership validation for packet item IDs before disabled buy-transaction planning.
- Hydrated the existing non-live `TradeBuyTransactionPlan` from socket-visible facts:
  - packet item IDs/counts
  - item template price/AP/acquisition metadata
  - player Kinah, AP, required-item counts, and free cube slots
  - ordinary trade-list NPC type/rate data
- Added socket regression coverage proving a normal NPC buy-from-shop request attaches a disabled buy transaction plan with Kinah, AP, required-item, persistence-intent, and packet-intent diagnostics.
- Added real Java static-data loader assertions for representative acquisition type/item/count rows.
- Kept this work non-live. No live item add/delete, AP/Kinah mutation, limited-item mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/trade transaction slice first failed because `GoodsListItemSummary` uses `Id`, not `ItemId`; after correction, it passed with 84 tests.
- Focused buy-item/trade transaction slice plus real static-data loader test passed with 85 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4956 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` buy-from-shop execution remains disabled.
- Diagnostic limited-item availability currently defaults to allowed; live `LimitedItemTradeService.getLimitedItem` counters are not wired.
- The socket hydration uses the existing deterministic planner price inputs and still does not prove full Java `PricesService.getBuyPrice(price, race)` global influence/tax parity.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Live AP/Kinah/item mutation, item-add overflow behavior, repository writes, transaction boundaries, and packet fanout remain unwired.

## Parity Table Updates

- Added Session 1929 rows in `docs/PHASE-6-PROGRESS.md` for:
  - Java `Acquisition`
  - Java `TradeList.calculateBuyListPrice`
  - Java `TradeList.calculateAbyssRewardBuyList`
  - Java `TradeService.performBuyTransaction`
  - Java `TradeService.performBuyFromShop`
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
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
