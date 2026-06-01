# Phase 6 Session 1926 Handoff - Socket Normal Sell Plan Hydration

Date: 2026-06-01
Unit of Work: UOW-1926
Status: Completed

## What Changed

- Wired NPC action `1` normal sell socket diagnostics to hydrate a disabled `TradeSellToShopPlan`.
- The normal sell diagnostic plan now uses active player inventory, parsed packet items, static item templates, Java `ItemMask.SELLABLE`, `gameserver.prices.vendor.sellmod`, the existing sell-limit planner, and the C# baseline can-trade guard.
- Added socket-level regression coverage proving:
  - sellable full-stack normal sell facts create a disabled sell plan and disabled final outcome with inventory/Kinah/repurchase/persistence/send intent recorded but not executed
  - non-sellable item-mask facts block the disabled sell plan before mutation while recording the Java system-message send intent
- Kept this strictly non-live. No inventory deletion/decrease, Kinah mutation, repurchase mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerSellLimitPlanServiceTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/normal-sell slice passed with 79 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4948 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Normal sell diagnostics still do not allocate object IDs for partial-stack repurchase items or missing Kinah rows.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults until equivalent NPC function metadata is safely exposed to this socket path.
- Sell-limit hydration does not yet use Java's live account max-level source, membership-clamped `Rates.SELL_LIMIT`, or live account sell-limit map mutation.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1926 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled normal sell-to-shop mutation plan hydration from socket facts
  - Java sellable-mask fact adaptation
  - sell-limit formula consumption in socket diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: safely expand normal sell diagnostics to cover partial-stack repurchase object ID allocation and missing-Kinah-row creation as disabled facts, without enabling live mutation.

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
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1926-Completion.md`
- `docs/Phase-6-Session-1926-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performSellToShop`
  - Java `PlayerLimitService.updateSellLimit`
  - Java `Item.isSellable`
  - Java `ItemMask.SELLABLE`
  - Java `TradeListData.getPurchaseTemplate`
  - Java NPC function checks for `canBuy()` and `canPurchase()`
  - C# `GameServerConnection.HandleBuyItem`
  - C# `TradeSellToShopPlanService`
  - C# `PlayerSellLimitPlanService`
  - C# `SellLimitLookupService`
  - C# `CmBuyItemHandlerCompositionPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
