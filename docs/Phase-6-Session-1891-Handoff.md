# Phase 6 Session 1891 Handoff - CM_BUY_ITEM Sell-To-Shop Composition Planner

Date: 2026-05-31
Unit of Work: UOW-1891
Status: Completed

## What Changed

- Added `CmBuyItemSellToShopCompositionPlanService`, a non-live planner for Java `CM_BUY_ITEM.runImpl` action `1`.
- Added composition status, step, input, dispatch descriptor, and plan records.
- Added focused tests for normal `performSellToShop` dispatch intent, Abyss purchase-template `performSellForAPToShop` branch selection, parser audit stop, non-action skip, target branch gates, interaction audit ordering, NPC `canBuy/canPurchase` skip, and missing-player skip.
- Kept this work non-live. No socket handler, actual known-list lookup, live `DialogService` call, live NPC state query, inventory/AP/Kinah mutation, repository writes, packet fanout, audit logging side effects, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~SmSellItemPacketPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused composition slice passed with 10 tests.
- Related parser/sell/repurchase/sell-packet slice passed with 36 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4799 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The composition planner is non-live and is not invoked by `GameServerConnection`.
- Java active-player lookup, known-list target lookup, interaction checks, NPC `canBuy/canPurchase`, and purchase-template lookup are represented by supplied facts.
- Java `performSellForAPToShop` internals remain unported here; only action `1` branch selection is represented.
- Live socket handling, packet fanout, repository persistence, transaction behavior, audit logging side effects, AP mutation, Kinah mutation, inventory mutation, and real-client validation remain unimplemented.

## Parity Table Updates

- Added Session 1891 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM` action `1` run flow
  - `TradeService.performSellToShop` action `1` call site
  - `TradeService.performSellForAPToShop` action `1` ABYSS call site
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect Java action `13`-`16` buy-from-shop run gating and add a non-live composition/dispatch descriptor if it can stay independent from live inventory, AP, limited-item, and goods-list mutations.

Safe alternative candidates:

- Inspect live `GameServerConnection` handler gaps for `CmBuyItem` without enabling side effects.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Inspect Java `TradeService.performSellForAPToShop` internals as a non-live AP-sell planner.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemSellToShopCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSellToShopCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmBuyItemRepurchaseCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1891-Completion.md`
- `docs/Phase-6-Session-1891-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM`, inspect:
  - Java `CM_BUY_ITEM.runImpl` action `13`-`16`
  - Java `TradeService.performBuyFromShop`
  - Java `TradeService.performBuyTransaction`
  - C# `CmBuyItem`
  - C# `CmBuyItemSellToShopCompositionPlanService`
  - C# trade-list packet/plan services
  - C# `GameServerConnection` handler behavior for parsed packets without live handling
- If JDK 25 and Maven become available, prioritize Java golden capture before further source-only work.
