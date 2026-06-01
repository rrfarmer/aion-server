# Phase 6 Session 1927 Completion - Socket Normal Sell Diagnostic Object IDs

Date: 2026-06-01
Unit of Work: UOW-1927
Status: Completed

## Work Discovery

- Re-read required orchestration/parity docs, latest Phase 6 progress context, latest completion context, and latest handoff before selecting work.
- Inspected Java `TradeService.performSellToShop`.
- Confirmed Java allocates a new repurchase item for partial-stack sells through `ItemFactory.newItem(item.getItemId(), count)`.
- Confirmed Java may create a Kinah row through `inventory.increaseKinah(kinahReward, INC_KINAH_SELL)` when the cube has no existing Kinah item.
- Inspected C# `GameServerConnection.ResolveBuyItemSellToShopPlan` and `TradeSellToShopPlanService`.
- Confirmed the existing C# planner already blocks allocation-required paths when `nextObjectId()` returns `0`.

## What Changed

- Added an explicit diagnostic object-ID provider to `GameServerConnection` for buy-item normal sell diagnostics.
- The provider is optional and defaults to no IDs, preserving the previous no-live-allocation boundary.
- Socket diagnostics can now model disabled normal sell plans for:
  - partial-stack item decrease plus diagnostic repurchase item creation
  - missing-Kinah-row diagnostic creation
- Added socket-level regression coverage for:
  - supplied diagnostic IDs producing a disabled partial-stack/missing-Kinah normal sell plan
  - absent diagnostic IDs keeping partial-stack sell blocked at `BlockedRepurchaseItemCreateFailed`
- Kept this work non-live. No live ID allocation, inventory mutation, Kinah mutation, repurchase mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerSellLimitPlanServiceTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/normal-sell slice first failed because the test item template used `MaxStackCount=1`, which clamped the diagnostic repurchase stack; after correcting the fixture template to `MaxStackCount=10`, the slice passed with 81 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4950 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Diagnostic object IDs are supplied facts only and are not live `IDFactory` reservations.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults in this socket path.
- Sell-limit hydration still lacks Java's live account max-level source, membership-clamped `Rates.SELL_LIMIT`, and live account sell-limit map mutation.
- Normal sell plan hydration still does not prove live inventory mutation, Kinah mutation, repurchase state mutation, packet fanout, transaction, or repository semantics.

## Parity Table Updates

- Added Session 1927 rows in `docs/PHASE-6-PROGRESS.md` for:
  - partial-stack repurchase diagnostic object-ID consumption
  - missing-Kinah-row diagnostic object-ID consumption
- All rows remain `Partial Parity`, `Regression Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: expose NPC buy/purchase function facts to the socket diagnostic path if static metadata can be proven equivalent to Java `npc.canBuy()` / `npc.canPurchase()`.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Add Java-runtime golden capture for `CM_BUY_ITEM` once compatible Java and Maven are available.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
