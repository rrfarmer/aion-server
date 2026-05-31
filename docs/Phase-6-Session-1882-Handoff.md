# Phase 6 Session 1882 Handoff - Private Store Source Item Purchase Planner

Date: 2026-05-31
Unit of Work: UOW-1882
Status: Completed

## What Changed

- Added `PrivateStorePurchasePlanService`.
- Added `PrivateStorePurchasePlanServiceTests`.
- The planner is non-live and source-reviewed against Java `PrivateStoreService.sellStoreItem`.
- It composes UOW-1881 source-item clone support through `InventoryAddService.CreateAddItemPlan(..., sourceItem)`.
- Covered Java private-store purchase behavior:
  - seller/buyer online and race guard
  - empty bought-item guard
  - buyer free-slot guard and `STR_MSG_DICE_INVEN_ERROR`
  - unchecked Java `long` price accumulation plus `price < 0` dupe guard
  - buyer Kinah affordability
  - seller item lookup and changed-count audit guard
  - seller item decrement/delete intent
  - packed-item `packCount - 1` source item for buyer add
  - buyer add/update planning
  - seller personal-shop notification messages
  - buyer/seller Kinah update intent
  - close-store intent when the sold list becomes empty
- Kept this work isolated. No live `CM_BUY_ITEM` wiring, repository writes, packet fanout, Java runtime capture, or real private-store state mutation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The planner is not wired into `CM_BUY_ITEM` or a live socket handler.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PrivateStorePurchasePlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InventoryAddService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InventoryAddServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1882-Completion.md`
- `docs/Phase-6-Session-1882-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing source-item clone work, inspect:
  - Java `RepurchaseService.repurchaseFromShop`
  - Java `RepurchaseList`
  - Java `ItemService.addItem(Player, Item)`
  - C# `InventoryAddService`
  - C# kinah mutation helpers
  - private-store purchase planner tests for reusable source-item clone expectations
- If JDK 25 and Maven become available, prioritize the condition preview Java capture draft before further source-only work.
