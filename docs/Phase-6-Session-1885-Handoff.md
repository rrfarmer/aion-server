# Phase 6 Session 1885 Handoff - Sell-To-Shop Repurchase Capture Planner

Date: 2026-05-31
Unit of Work: UOW-1885
Status: Completed

## What Changed

- Added `TradeSellToShopPlanService`.
- Added `TradeSellToShopPlanServiceTests`.
- The planner is non-live and source-reviewed against Java `TradeService.performSellToShop`.
- It returns mutation intent for:
  - seller item deletes
  - seller item count updates
  - replacement repurchase item set
  - Kinah increase
- Covered Java sell-to-shop behavior:
  - caller-supplied `PlayerRestrictions.canTrade(player)` gate
  - item lookup by object id
  - purchase-template goods-list validation
  - normal sellability guard
  - `PricesService.getSellReward`
  - purchase-template buy-rate reward calculation
  - externally supplied `PlayerLimitService.updateSellLimit` count
  - zero adjusted count break
  - full-stack delete and original-item repurchase capture
  - partial-stack decrease and fresh repurchase item creation
  - `RepurchaseService.addRepurchaseItems(player, items)` replacement intent
  - `inventory.increaseKinah(kinahReward, INC_KINAH_SELL)` intent
- Kept this work isolated. No live `CM_BUY_ITEM` wiring, repository writes, packet fanout, Java runtime capture, audit logging, sell-limit persistence, or real repurchase state mutation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests|FullyQualifiedName~SmSellItemPacketPlanServiceTests|FullyQualifiedName~PricesServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused sell-to-shop planner slice passed with 8 tests.
- Related trade/repurchase/pricing slice passed with 31 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4752 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The planner is not wired into `CM_BUY_ITEM` action `1` or any live socket handler.
- Java `PlayerLimitService.updateSellLimit` is represented by caller-supplied adjusted count.
- Java `item.isSellable()` is represented by caller-supplied `IsSellable`.
- Live inventory mutation, Kinah packet/update type, repository persistence, repurchase singleton replacement, audit logging, transaction behavior, and rollback behavior remain unimplemented.
- Partial-sale `ItemFactory.newItem` behavior is approximated through `InventoryItemFactory.CreateNewItem`; Java runtime object defaults were not captured.

## Parity Table Updates

- Added Session 1885 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `TradeService.performSellToShop`
  - `RepurchaseService.addRepurchaseItems`
  - `PricesService.getSellReward` sell-to-shop composition
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: inspect existing C# `NpcDialogServiceSelectPlanService` BUY_AGAIN descriptor and add a non-live `SmRepurchase` dialog composition plan only if it can be done without socket sends or repurchase singleton state.

Safe alternative candidates:

- Inspect and model `CM_BUY_ITEM` action `2` read validation as a non-live parser/plan.
- Wire `TradeSellToShopPlanService` only after inventory/repository/packet mutation ordering and rollback behavior are scoped.
- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellToShopPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmRepurchase.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmRepurchaseTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1885-Completion.md`
- `docs/Phase-6-Session-1885-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing repurchase/dialog work, inspect:
  - Java `DialogService` BUY_AGAIN branch
  - Java `SM_REPURCHASE`
  - Java `RepurchaseService.getRepurchaseItems`
  - C# `NpcDialogServiceSelectPlanService`
  - C# `SmRepurchase`
  - C# `TradeSellToShopPlanService`
  - C# `RepurchasePlanService`
- If JDK 25 and Maven become available, prioritize the condition preview Java capture draft before further source-only work.
