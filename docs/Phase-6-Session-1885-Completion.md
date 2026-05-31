# Phase 6 Session 1885 Completion - Sell-To-Shop Repurchase Capture Planner

Date: 2026-05-31
Unit of Work: UOW-1885
Status: Completed

## Scope

- Performed Work Discovery around Java `TradeService.performSellToShop` and `RepurchaseService.addRepurchaseItems`.
- Added a non-live C# planner for sell-to-shop mutation intent and repurchase capture.
- Kept live `CM_BUY_ITEM` wiring, repository writes, packet send ordering, audit logging, and Java runtime capture out of scope.

## What Changed

- Added `TradeSellToShopPlanService`.
- Added `TradeSellToShopPlanServiceTests`.
- The planner models Java sell-to-shop behavior:
  - caller-supplied `PlayerRestrictions.canTrade(player)` gate
  - inventory item lookup by object id
  - purchase-template goods-list validation
  - normal sellability guard when no purchase template is supplied
  - Java sell reward and purchase-template buy-rate reward calculation
  - externally supplied sell-limit adjusted count
  - zero adjusted count break
  - full-stack delete intent and original item repurchase capture
  - partial-stack decrease intent and fresh repurchase item creation
  - repurchase price assignment
  - replacement repurchase item set intent
  - Kinah increase intent

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
