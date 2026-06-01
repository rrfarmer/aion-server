# Phase 6 Session 1928 Completion - Socket NPC Function Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1928
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` NPC function gates.
- Inspected Java `Npc.canSell`, `Npc.canBuy`, and `Npc.canPurchase`.
- Inspected Java `NpcTemplate.supportsAction` and `TalkInfo.func_dialogs`.
- Inspected C# `NpcTemplateSummary.SupportsDialogAction`, `TradeListTable`, `GameServerConnection.HandleBuyItem`, and existing buy-item diagnostic planners.
- Confirmed Java action `1` uses `npc.canBuy() || npc.canPurchase()`, action `2` uses `npc.canBuy()`, and actions `13`-`16` use `npc.canSell()`.

## What Changed

- Added `CmBuyItemNpcTradeFunctionFactAdapterService` to derive diagnostic NPC trade-function facts from static NPC function dialog IDs and trade-list/purchase-template availability.
- The adapter mirrors the Java source formulas:
  - `canSell`: trade-list template exists and NPC supports `DialogAction.BUY` (`2`)
  - `canBuy`: NPC supports `DialogAction.SELL` (`3`) or derived `canSell`
  - `canPurchase`: purchase template exists and NPC supports `DialogAction.TRADE_SELL_LIST` (`103`)
- Wired `GameServerConnection` buy-item diagnostics to resolve those facts for world NPC targets.
- Updated sell-action fact resolution to consume the resolved NPC function facts instead of hardcoded diagnostic defaults.
- Added unit coverage for sell, buy, purchase, and missing-function/missing-trade-data derivation.
- Added socket-level regression coverage proving an NPC without the required function metadata skips disabled sell dispatch even when purchase data exists.
- Kept this work non-live. No live buy/sell/repurchase/private-store/pet mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/function-fact slice first failed because one socket fixture lacked the Java `TRADE_SELL_LIST` function id required for an ABYSS purchase template; after correcting the fixture, the slice passed with 74 tests.
- Related `CM_BUY_ITEM` diagnostic branch slice passed with 126 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4955 tests.
- After a final fixture cleanup, the related `CM_BUY_ITEM` diagnostic branch slice was rerun and passed again with 126 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1`, action `2`, and actions `13`-`16` execution remains disabled.
- NPC function facts are diagnostic-only and depend on loaded C# static `FunctionDialogIds` plus `TradeListTable` data.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Buy-from-shop transaction fact hydration and live buy transaction mutation remain unwired.
- Normal/AP sell, repurchase, private-store, and pet merchant mutation paths remain non-live.

## Parity Table Updates

- Added Session 1928 rows in `docs/PHASE-6-PROGRESS.md` for:
  - Java `Npc.canSell`
  - Java `Npc.canBuy`
  - Java `Npc.canPurchase`
  - Java `CM_BUY_ITEM.runImpl` NPC function guard diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add Java-runtime golden capture for `CM_BUY_ITEM` once compatible Java and Maven are available.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
