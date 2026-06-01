# Phase 6 Session 1925 Completion - Socket AP Sell Plan Hydration

Date: 2026-05-31
Unit of Work: UOW-1925
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `TradeService.performSellForAPToShop`.
- Inspected Java `TradeService.performSellToShop`.
- Inspected C# `GameServerConnection.HandleBuyItem`.
- Inspected C# `TradeSellForApToShopPlanService`, `TradeSellToShopPlanService`, and buy-item socket diagnostics.
- Confirmed the AP-sell path can hydrate a disabled mutation plan without object ID allocation or live mutation, while normal sell still needs additional facts before safe plan hydration.

## What Changed

- Wired NPC action `1` ABYSS socket diagnostics to create a disabled `TradeSellForApToShopPlan` when item-template and goods-list facts are available.
- The AP-sell diagnostic path now uses:
  - active player inventory snapshot
  - parsed `CM_BUY_ITEM` item requests
  - static item templates
  - static purchase goods lists
  - `gameserver.selling.apitems.enabled` config
  - existing C# baseline `PlayerRestrictions.canTrade` equivalent
- Added socket-level regression coverage for:
  - valid AP-sell plan hydration from inventory/template/goods facts
  - invalid purchase goods-list rejection before side-effect intent
- Kept normal sell-to-shop mutation planning unwired because Java normal sell still needs sellability, sell-limit adjustment, repurchase object ID allocation, Kinah row handling, transaction semantics, and packet persistence facts.
- Kept this work non-live. No inventory decrease, AP mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/AP-sell slice first failed at compile because `SellingApItemsEnabled` was referenced on the wrong options object, then passed after correction with 66 tests.
- Related buy-item/trade slice passed with 77 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4946 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Normal sell-to-shop mutation planning remains missing in socket diagnostics.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults in this socket path.
- AP-sell plan hydration still does not prove live `Storage.decreaseByObjectId`, `AbyssPointsService.addAp`, packet fanout, transaction, or repository semantics.
- Normal sell sellability, sell-limit, Kinah, repurchase, packet, transaction, repository, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1925 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled AP-sell mutation plan hydration from socket facts
  - invalid purchase-item guard coverage from socket diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled normal sell-to-shop fact hydration candidates for action `1`, starting with sellability and sell-limit facts only if they can be read without ID allocation or live mutation.

Safe alternative candidates:

- Expose NPC buy/purchase function facts to the socket diagnostic path if static function metadata can be proven equivalent to Java `npc.canBuy()` / `npc.canPurchase()`.
- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
