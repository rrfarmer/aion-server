# Phase 6 Session 1928 Handoff - Socket NPC Function Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1928
Status: Completed

## What Changed

- Added a source-reviewed `CM_BUY_ITEM` NPC trade-function fact adapter.
- The adapter derives diagnostic `NpcCanSell`, `NpcCanBuy`, and `NpcCanPurchase` from loaded NPC function dialog IDs and trade-list/purchase-template availability.
- `GameServerConnection` now resolves those facts for world NPC targets before composing disabled buy-item plans.
- Sell-action diagnostics now consume the resolved facts instead of hardcoded `NpcCanBuy=true` / `NpcCanPurchase=false` defaults.
- Added adapter-level regression coverage for Java `canSell`, `canBuy`, `canPurchase`, and missing-data combinations.
- Added socket-level regression coverage proving unsupported NPC function metadata skips the disabled sell dispatch.
- Kept this strictly non-live. No live inventory mutation, AP/Kinah mutation, buy transaction, sell transaction, repurchase mutation, private-store/pet mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemNpcTradeFunctionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/function-fact slice first failed because one socket fixture lacked required `TRADE_SELL_LIST` function metadata; after correction, the slice passed with 74 tests.
- Related `CM_BUY_ITEM` diagnostic branch slice passed with 126 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4955 tests.
- After a final fixture cleanup, the related `CM_BUY_ITEM` diagnostic branch slice was rerun and passed again with 126 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1`, action `2`, and actions `13`-`16` execution remains disabled.
- NPC function facts are diagnostic-only and depend on loaded C# static `FunctionDialogIds` plus `TradeListTable` data.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Buy-from-shop transaction fact hydration and live buy transaction mutation remain unwired.
- Normal/AP sell, repurchase, private-store, and pet merchant mutation paths remain non-live.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1928 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `Npc.canSell`
  - `Npc.canBuy`
  - `Npc.canPurchase`
  - `CM_BUY_ITEM.runImpl` NPC function guard diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemNpcTradeFunctionFactAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemNpcTradeFunctionFactAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1928-Completion.md`
- `docs/Phase-6-Session-1928-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performBuyTransaction`
  - Java NPC function checks for `canSell()`, `canBuy()`, and `canPurchase()`
  - C# `GameServerConnection.HandleBuyItem`
  - C# `CmBuyItemNpcTradeFunctionFactAdapterService`
  - C# `CmBuyItemBuyFromShopCompositionPlanService`
  - C# `TradeBuyTransactionPlanService`
  - C# NPC/goods/item template static data holders
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
