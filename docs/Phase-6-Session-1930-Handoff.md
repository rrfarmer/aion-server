# Phase 6 Session 1930 Handoff - Buy Limited Item Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1930
Status: Completed

## What Changed

- `GameServerConnection` buy-from-shop diagnostics now build limited-item facts from the resolved ordinary trade-list template and goods-list rows.
- The disabled buy transaction plan now evaluates the Java `TradeService.canBuyLimitItem` guard shape for selected NPC buy-from-shop requests.
- Over-limit limited-item purchases now produce a disabled `BlockedLimitedItem` transaction plan and limited-buy denial send intent instead of defaulting to allowed.
- Added socket regression coverage for an NPC buy-from-shop request where count `2` exceeds a limited item's sell/buy limit of `1`.
- Kept this strictly non-live. No live item add/delete, AP/Kinah mutation, limited-item counter mutation, cron reset, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~NpcDialogLimitedItemFactAdapterServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/limited-item composition slice passed with 83 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4957 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` buy-from-shop execution remains disabled.
- Live `LimitedItemTradeService` startup state, scheduled cron resets, sell-limit decrementing, and per-player buy-count mutation remain unwired.
- The diagnostic path uses loaded static/default sell-limit facts and default player buy counts unless an adapter caller supplies buy-count state; it does not prove parity with a long-running Java server after previous purchases.
- Full Java NPC dialog, known-list, and range validation remains pending.
- Live AP/Kinah/item mutation, item-add overflow behavior, repository writes, transaction boundaries, and packet fanout remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1930 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `LimitedItemTradeService.start`
  - `GoodsList.getLimitedItems`
  - `TradeService.canBuyLimitItem`
  - `LimitedItem`
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1930-Completion.md`
- `docs/Phase-6-Session-1930-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing the recommended pet auto-sell notification slice, inspect:
  - Java `PetService.sell`
  - Java packet/system-message behavior for pet auto-sell notifications
  - Existing C# `CM_BUY_ITEM` action `17` pet branch diagnostics
  - Existing disabled notification/outcome planner patterns
- If continuing `CM_BUY_ITEM` buy-from-shop instead, inspect:
  - Java `PricesService.getBuyPrice`
  - Java `TradeList.calculateBuyListPrice`
  - C# buy transaction price inputs and test fixtures
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
