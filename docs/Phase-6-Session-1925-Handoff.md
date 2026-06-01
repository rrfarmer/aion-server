# Phase 6 Session 1925 Handoff - Socket AP Sell Plan Hydration

Date: 2026-05-31
Unit of Work: UOW-1925
Status: Completed

## What Changed

- Wired NPC action `1` ABYSS socket diagnostics to hydrate a disabled `TradeSellForApToShopPlan`.
- The AP-sell diagnostic plan now uses active player inventory, parsed packet items, static item templates, static purchase goods lists, `gameserver.selling.apitems.enabled`, and the C# baseline can-trade guard.
- Added socket-level regression coverage proving:
  - valid AP purchase facts create a disabled AP-sell plan and disabled final outcome with inventory/AP/persistence/send intent recorded but not executed
  - purchase goods-list rejection blocks the disabled AP-sell plan before side-effect intent
- Kept normal sell-to-shop mutation planning unwired. It still needs sellability, sell-limit adjustment, repurchase object ID allocation, Kinah row handling, and transaction/packet persistence facts before safe hydration.
- Kept this strictly non-live. No inventory decrease, AP mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/AP-sell slice first failed at compile because `SellingApItemsEnabled` was referenced on the wrong options object, then passed after correction with 66 tests.
- Related buy-item/trade slice passed with 77 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4946 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Normal sell-to-shop mutation planning remains missing in socket diagnostics.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults until equivalent NPC function metadata is safely exposed to this socket path.
- AP-sell plan hydration still does not prove live `Storage.decreaseByObjectId`, `AbyssPointsService.addAp`, packet fanout, transaction, or repository semantics.
- Normal sell sellability, sell-limit, Kinah, repurchase, packet, transaction, repository, and real client behavior remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1925 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled AP-sell mutation plan hydration from socket facts
  - invalid purchase-item guard coverage from socket diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellForApToShopPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1925-Completion.md`
- `docs/Phase-6-Session-1925-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellToShop`
  - Java `TradeService.performSellForAPToShop`
  - Java `PlayerLimitService.updateSellLimit`
  - Java item sellability metadata / `Item.isSellable`
  - Java `TradeListData.getPurchaseTemplate`
  - Java NPC function checks for `canBuy()` and `canPurchase()`
  - C# `GameServerConnection.HandleBuyItem`
  - C# `TradeSellToShopPlanService`
  - C# `TradeSellForApToShopPlanService`
  - C# `CmBuyItemHandlerCompositionPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
