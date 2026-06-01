# Phase 6 Session 1930 Completion - Buy Limited Item Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1930
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `TradeService.canBuyLimitItem`.
- Inspected Java `LimitedItemTradeService.start` and `LimitedItemTradeService.getLimitedItem`.
- Inspected Java `LimitedItem`, `LimitedTradeNpc`, and `GoodsList.getLimitedItems`.
- Inspected C# `NpcDialogLimitedItemFactAdapterService`, `TradeBuyTransactionPlanService`, and `GameServerConnection.ResolveBuyItemBuyTransactionPlan`.

## What Changed

- Wired selected `CM_BUY_ITEM` buy-from-shop diagnostics to build limited-item facts for the resolved trade-list template and goods-list rows.
- Added a `GameServerConnection` helper that mirrors the Java `TradeService.canBuyLimitItem` guard shape for disabled transaction planning:
  - allow non-limited items
  - reject when requested count exceeds remaining sell limit
  - reject when requested count plus player buy count exceeds buy limit
- Fed that limited-item result into the existing non-live `TradeBuyTransactionPlanService` so over-limit requests now record `BlockedLimitedItem` instead of defaulting to allowed.
- Added socket regression coverage proving an over-limit limited item request records disabled limited-item rejection, would send the limited-buy denial intent, and performs no live side effects.
- Kept this work non-live. No live item add/delete, AP/Kinah mutation, limited-item counter mutation, cron reset, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~NpcDialogLimitedItemFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/limited-item composition slice passed with 83 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4957 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` buy-from-shop execution remains disabled.
- Live `LimitedItemTradeService` startup state, scheduled cron resets, sell-limit decrementing, and per-player buy-count mutation remain unwired.
- The diagnostic path uses loaded static/default sell-limit facts and default player buy counts unless an adapter caller supplies buy-count state; it does not prove parity with a long-running Java server after previous purchases.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Live AP/Kinah/item mutation, item-add overflow behavior, repository writes, transaction boundaries, and packet fanout remain unwired.

## Parity Table Updates

- Added Session 1930 rows in `docs/PHASE-6-PROGRESS.md` for:
  - Java `LimitedItemTradeService.start`
  - Java `GoodsList.getLimitedItems`
  - Java `TradeService.canBuyLimitItem`
  - Java `LimitedItem`
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `PetService.sell` auto-sell notification behavior and add a separate disabled notification planner if it can remain non-live.

Safe alternative candidates:

- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add Java-runtime golden capture for `CM_BUY_ITEM` once compatible Java and Maven are available.
- Continue buy-from-shop diagnostics around Java `PricesService.getBuyPrice(price, race)` global influence/tax facts without enabling live execution.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
