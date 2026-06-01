# Phase 6 Session 1926 Completion - Socket Normal Sell Plan Hydration

Date: 2026-06-01
Unit of Work: UOW-1926
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `TradeService.performSellToShop`.
- Inspected Java `Item.isSellable` and `ItemMask.SELLABLE`.
- Inspected Java `PlayerLimitService.updateSellLimit` and `SellLimit.getSellLimit`.
- Inspected C# `GameServerConnection.HandleBuyItem`.
- Inspected C# `TradeSellToShopPlanService`, `PlayerSellLimitPlanService`, `SellLimitLookupService`, and buy-item socket diagnostics.
- Confirmed normal sell diagnostics can safely hydrate disabled full-stack/no-allocation plans from read-only facts while keeping live mutation disabled.

## What Changed

- Wired NPC action `1` normal sell socket diagnostics to create a disabled `TradeSellToShopPlan` when item-template facts are available.
- The normal sell diagnostic path now uses:
  - active player inventory snapshot
  - parsed `CM_BUY_ITEM` item requests
  - static item templates
  - Java `ItemMask.SELLABLE` bit `1 << 2`
  - `_options.Prices.VendorSellModifier`
  - existing non-live sell-limit planning over a diagnostic current-limit/base-limit fact
  - existing C# baseline `PlayerRestrictions.canTrade` equivalent
- Added socket-level regression coverage for:
  - valid full-stack normal sell plan hydration from inventory/template facts
  - non-sellable item-mask rejection with the disabled Java not-sellable packet intent
- Kept this work non-live. No inventory deletion/decrease, Kinah mutation, repurchase mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerSellLimitPlanServiceTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/normal-sell slice passed with 79 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4948 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Normal sell diagnostics still do not allocate object IDs for partial-stack repurchase items or missing Kinah rows.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults in this socket path.
- Sell-limit hydration uses the loaded player level/base limit or an injected current-limit diagnostic fact; Java's account max-level lookup, membership-clamped `Rates.SELL_LIMIT`, and live account sell-limit map mutation remain unwired.
- Normal sell plan hydration still does not prove live inventory mutation, Kinah mutation, repurchase state mutation, packet fanout, transaction, or repository semantics.

## Parity Table Updates

- Added Session 1926 rows in `docs/PHASE-6-PROGRESS.md` for:
  - disabled normal sell-to-shop mutation plan hydration from socket facts
  - Java sellable-mask fact adaptation
  - sell-limit formula consumption in socket diagnostics
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
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
