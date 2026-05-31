# Phase 6 Session 1882 Completion - Private Store Source Item Purchase Planner

Date: 2026-05-31
Unit of Work: UOW-1882
Status: Completed

## Scope

- Performed Work Discovery around Java `PrivateStoreService.sellStoreItem`.
- Added a non-live C# planner for private-store purchase behavior.
- Composed the UOW-1881 `InventoryAddService` source-item clone path into the private-store buyer reward add plan.
- Kept live socket wiring, repository writes, packet send ordering, and Java runtime capture out of scope.

## What Changed

- Added `PrivateStorePurchasePlanService`.
- Added `PrivateStorePurchasePlanServiceTests`.
- The planner models Java private-store purchase guards and mutation intent:
  - seller/buyer online and same-race guard
  - empty bought-item guard
  - buyer free-slot guard with Java dice-inventory message
  - Java-style unchecked price accumulation plus `price < 0` dupe guard
  - insufficient Kinah guard
  - seller item lookup and changed-count audit guard
  - missing target template guard before reward add
  - seller source item update/delete intent
  - packed-item `packCount - 1` source passed into buyer add planning
  - buyer reward add planning through `InventoryAddService.CreateAddItemPlan(..., sourceItem)`
  - buyer/seller Kinah update intent
  - seller personal-shop notification messages
  - close-store intent when no sold items remain

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PrivateStorePurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~PrivateStoreItemValidationPlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests|FullyQualifiedName~PrivateStoreOpenGuardPlanServiceTests|FullyQualifiedName~PrivateStoreOpenPlanServiceTests|FullyQualifiedName~PrivateStoreClosePlanServiceTests|FullyQualifiedName~InventoryAddServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused private-store purchase slice passed with 6 tests.
- Related private-store/inventory planner slice passed with 60 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4733 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The planner is not wired into `CM_BUY_ITEM` or any live socket handler.
- Live private-store map mutation, seller/buyer inventory mutation, packet fanout, audit logging, repository persistence, transaction behavior, and rollback behavior remain unimplemented.
- Java partial-processing behavior after earlier successful bought items is not live; this planner blocks unsafe partial edges until runtime mutation ordering can be staged.
- Java `ItemService.addItem` expirable registration, add/update packet types, and DAO behavior remain outside this unit.

## Parity Table Updates

- Added Session 1882 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `PrivateStoreService.sellStoreItem`
  - `TradePSItem.decreaseCount` / seller source decrement planning
  - `ItemService.addItem(Player, Item, long)` composition from private store into `InventoryAddService`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: if JDK 25 and Maven are available, return to the condition preview Java capture draft and produce runtime output; otherwise inspect Java `RepurchaseService.repurchaseFromShop` and add a small non-live repurchase source-item clone planner using the same `InventoryAddService` source-item path.

Safe alternative candidates:

- Wire `PrivateStorePurchasePlanService` only after repository/packet mutation ordering is scoped and tests can cover no-partial-send behavior.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
