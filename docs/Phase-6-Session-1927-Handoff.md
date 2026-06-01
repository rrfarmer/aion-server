# Phase 6 Session 1927 Handoff - Socket Normal Sell Diagnostic Object IDs

Date: 2026-06-01
Unit of Work: UOW-1927
Status: Completed

## What Changed

- Added an optional diagnostic object-ID provider to `GameServerConnection` for buy-item normal sell diagnostics.
- The default path still returns no IDs, so partial-stack repurchase creation and missing-Kinah-row creation remain blocked unless a diagnostic provider supplies facts.
- Added socket-level regression coverage proving:
  - supplied IDs hydrate a disabled partial-stack normal sell plan with seller item update, repurchase item, missing-Kinah creation, disabled persistence/send intent, and no packet dispatch
  - absent IDs keep partial-stack normal sell blocked at `BlockedRepurchaseItemCreateFailed`
- Kept this strictly non-live. No live ID allocation, inventory mutation, Kinah mutation, repurchase mutation, packet dispatch, transaction commit, repository write, Java runtime output, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerSellLimitPlanServiceTests|FullyQualifiedName~CmBuyItemSellActionFactAdapterServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused buy-item/normal-sell slice first failed due to invalid test fixture max-stack metadata, then passed after correction with 81 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4950 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- Live `CM_BUY_ITEM` action `1` execution remains disabled.
- Diagnostic object IDs are supplied facts only and are not live `IDFactory` reservations.
- NPC `canBuy()` / `canPurchase()` function facts still use diagnostic defaults until equivalent NPC function metadata is safely exposed to this socket path.
- Sell-limit hydration does not yet use Java's live account max-level source, membership-clamped `Rates.SELL_LIMIT`, or live account sell-limit map mutation.
- Existing non-live `CM_BUY_ITEM` paths still must not be treated as verified Java execution.

## Parity Table Updates

- Added Session 1927 rows in `docs/PHASE-6-PROGRESS.md` for:
  - partial-stack repurchase diagnostic object-ID consumption
  - missing-Kinah-row diagnostic object-ID consumption
- All rows remain `Partial Parity`, `Regression Tested`, and not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: expose NPC buy/purchase function facts to the socket diagnostic path if static function metadata can be proven equivalent to Java `npc.canBuy()` / `npc.canPurchase()`.

Safe alternative candidates:

- Inspect Java `PetService.sell` auto-sell notification path as a separate disabled notification planner.
- Hydrate safe private-store listed-item facts into the diagnostic path only if no live mutation is enabled.
- Add production-safe buy-transaction fact hydration for selected NPC buy-from-shop diagnostics without dispatching live effects.
- Add Java-runtime golden capture for `CM_BUY_ITEM` once compatible Java and Maven are available.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1927-Completion.md`
- `docs/Phase-6-Session-1927-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java NPC function checks for `canBuy()` and `canPurchase()`
  - Java `TradeService.performSellToShop`
  - Java `TradeService.performBuyTransaction`
  - C# `GameServerConnection.HandleBuyItem`
  - C# NPC template/function metadata loading
  - C# `CmBuyItemSellActionFactAdapterService`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `TradeSellToShopPlanService`
- If JDK and Maven become available, prioritize Java golden capture before further source-only work.
