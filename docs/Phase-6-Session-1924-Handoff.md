# Phase 6 Session 1924 Handoff - Socket Sell Classification Wiring

Date: 2026-05-31
Unit of Work: UOW-1924
Status: Completed

## What Changed

- Wired the disabled, read-only `CmBuyItemSellActionFactAdapterService` into `GameServerConnection.HandleBuyItem` diagnostics for NPC action `1`.
- The socket diagnostic path now reads a supplied/runtime `TradeListTable`, looks up the purchase template by `WorldNpc.TemplateId`, and passes the resulting `PurchaseTemplate` into the existing non-live handler composition plan.
- Added socket-level regression coverage proving:
  - ABYSS purchase templates select disabled AP-sell outcome composition.
  - NORMAL purchase templates remain disabled normal sell-to-shop outcome composition.
- Kept this strictly non-live. No sell/AP-sell mutation plans, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, AP state, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item socket/classification slice passed with 53 tests.
- Related buy-item/trade slice passed with 75 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4944 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults until equivalent NPC function metadata is safely exposed to this socket path.
- Sell-to-shop/AP-sell plan hydration, inventory snapshots, goods-list validation, sell limits, Kinah state, repurchase state, AP state, packet dispatch, transaction behavior, repository writes, and real client behavior remain unwired.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1924 rows in `docs/PHASE-6-PROGRESS.md` for:
  - socket diagnostic purchase-template dispatch classification
  - ABYSS/NORMAL static purchase-template socket regression coverage
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: add disabled sell/AP-sell mutation fact hydration candidates for action `1`, starting with inventory item snapshots and goods-list lookup only if they can be read without enabling live mutation.

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
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellActionFactAdapterService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1924-Completion.md`
- `docs/Phase-6-Session-1924-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellToShop`
  - Java `TradeService.performSellForAPToShop`
  - Java `TradeListData.getPurchaseTemplate`
  - Java NPC function checks for `canBuy()` and `canPurchase()`
  - C# `GameServerConnection.HandleBuyItem`
  - C# `CmBuyItemSellActionFactAdapterService`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `TradeSellToShopPlanService`
  - C# `TradeSellForApToShopPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
