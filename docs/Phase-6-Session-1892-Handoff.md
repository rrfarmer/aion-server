# Phase 6 Session 1892 Handoff - CM_BUY_ITEM Buy-From-Shop Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1892
Status: Completed

## What Changed

- Added `CmBuyItemBuyFromShopCompositionPlanService`, a non-live planner for Java `CM_BUY_ITEM.runImpl` actions `13`, `14`, `15`, and `16`.
- Added composition status, step, input, item request, dispatch descriptor, and plan records.
- Added focused tests for buy-from-shop action dispatch intent, `TradeNpcType` to `useKinah` branch mapping, parser audit stop, non-action skip, target branch gates, interaction audit ordering, NPC `canSell` skip, unknown trade NPC type handling, and missing-player skip.
- Kept this work non-live. No socket handler, actual known-list lookup, live `DialogService` call, live NPC state query, trade-list data lookup, inventory/AP/Kinah mutation, limited-item goods-list mutation, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~SmTradeListPacketPlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused composition slice passed with 17 tests.
- Related parser/buy/sell/trade-list slice passed with 53 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4816 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC `canSell`, and trade-list template lookup are represented by supplied facts.
- Java `performBuyTransaction` internals remain unported here; only action `13`-`16` branch selection and `useKinah` classification are represented.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, AP mutation, Kinah mutation, inventory mutation, limited-item goods-list mutation, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1892 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` action `13`-`16` run flow
  - `TradeService.performBuyFromShop` `NORMAL`/`ABYSS_KINAH` branch
  - `TradeService.performBuyFromShop` `ABYSS`/`REWARD` branch
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect live C# `GameServerConnection` handler gaps for `CmBuyItem` and decide whether a non-live dispatcher aggregation layer can connect the parser/composition planners without enabling side effects.

Safe alternative candidates:

- Inspect Java `TradeService.performBuyTransaction` internals as a non-live buy-from-shop transaction planner.
- Inspect Java `TradeService.performSellForAPToShop` internals as a non-live AP-sell planner.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemBuyFromShopCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemBuyFromShopCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellToShopCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSellToShopCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1892-Completion.md`
- `docs/Phase-6-Session-1892-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performBuyTransaction`
  - Java `TradeService.performSellForAPToShop`
  - C# `CmBuyItem`
  - C# `GameServerConnection` packet dispatch handling
  - C# `CmBuyItemRepurchaseCompositionPlanService`
  - C# `CmBuyItemSellToShopCompositionPlanService`
  - C# `CmBuyItemBuyFromShopCompositionPlanService`
  - C# trade-list packet/plan services
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
