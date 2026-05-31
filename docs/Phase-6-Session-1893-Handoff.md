# Phase 6 Session 1893 Handoff - CM_BUY_ITEM Handler Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1893
Status: Completed

## What Changed

- Added `CmBuyItemHandlerCompositionPlanService`, a non-live top-level branch selector for Java `CM_BUY_ITEM.runImpl`.
- Added composition status, step, input, and plan records.
- The planner delegates Java Npc actions `1`, `2`, and `13`-`16` to the existing non-live sell-to-shop, repurchase, and buy-from-shop composition planners.
- The planner explicitly identifies Player action `0` private-store and Pet action `17` merchant sale as unsupported/non-live gaps.
- Kept this work non-live. No socket handler, actual known-list lookup, live `DialogService` call, live NPC/Pet state query, private-store sale, pet merchant sale, inventory/AP/Kinah mutation, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused handler composition slice passed with 15 tests.
- Related `CM_BUY_ITEM` parser/branch planner slice passed with 78 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4831 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The handler composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC/Pet state queries, and dependency calls are represented by supplied facts and nested planners.
- Java private-store action `0` and Pet merchant action `17` are identified as unsupported branches only.
- Java `performBuyTransaction`, `performSellForAPToShop`, and live sell/repurchase mutation internals remain partially represented only by prior non-live planners.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, AP mutation, Kinah mutation, inventory mutation, limited-item goods-list mutation, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1893 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` top-level target/action branch selection
  - `PrivateStoreService.sellStoreItem` action `0` call site
  - Pet MERCHANT action `17` `TradeService.performSellToShop` call site
- Rows remain conservative: top-level branch selector is `Partial Parity`; unsupported private-store and pet branches are `Needs Verification`.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `TradeService.performBuyTransaction` internals and add a non-live buy-from-shop transaction planner only for validation/cost/free-slot/limit decision ordering, without live AP/Kinah/item mutation.

Safe alternative candidates:

- Inspect Java `TradeService.performSellForAPToShop` internals as a non-live AP-sell planner.
- Inspect Java `PrivateStoreService.sellStoreItem` action `0` as a gap-scoped non-live planner.
- Inspect Java pet merchant action `17` sell-rate branch as a gap-scoped non-live planner.
- Wire `CmBuyItemHandlerCompositionPlanService` into a no-op diagnostic path only if live side effects remain disabled and handler ownership is scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemHandlerCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemHandlerCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemBuyFromShopCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellToShopCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1893-Completion.md`
- `docs/Phase-6-Session-1893-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `TradeService.performBuyTransaction`
  - Java `TradeService.validateBuyItems`
  - Java `LimitedItemTradeService`
  - Java `TradeList.calculateBuyListPrice`
  - Java `TradeList.calculateAbyssRewardBuyList`
  - C# `CmBuyItemHandlerCompositionPlanService`
  - C# `CmBuyItemBuyFromShopCompositionPlanService`
  - C# trade-list summary/dataholder services
  - C# inventory/AP/Kinah planner types already used by adjacent non-live services
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
