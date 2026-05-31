# Phase 6 Session 1883 Completion - Repurchase Source Item Planner

Date: 2026-05-31
Unit of Work: UOW-1883
Status: Completed

## Scope

- Performed Work Discovery around Java `RepurchaseService.repurchaseFromShop`.
- Added a non-live C# planner for repurchase behavior.
- Composed the UOW-1881 `InventoryAddService` source-item clone path into repurchase item add planning.
- Kept live socket wiring, repository writes, packet send ordering, audit logging, and Java runtime capture out of scope.

## What Changed

- Added `RepurchasePlanService`.
- Added `RepurchasePlanServiceTests`.
- The planner models Java repurchase guards and mutation intent:
  - caller-supplied `PlayerRestrictions.canTrade(player)` gate
  - ordered requested object ids from the repurchase list
  - per-request inventory-full precheck with Java dice-inventory message and break
  - missing repurchase item skip
  - insufficient Kinah skip-and-continue behavior
  - Kinah update intent after affordability passes
  - reward add planning through `InventoryAddService.CreateAddItemPlan(..., sourceItem, allowInventoryOverflow: true)`
  - repurchase-set removal intent after successful add planning
  - conservative non-live missing-template and add-failure blocks

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~PrivateStorePurchasePlanServiceTests|FullyQualifiedName~InventoryAddServiceTests|FullyQualifiedName~PrivateStoreItemValidationPlanServiceTests|FullyQualifiedName~PrivateStoreSellNotificationPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused repurchase slice passed with 14 tests.
- Related repurchase/private-store/inventory planner slice passed with 47 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4741 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The planner is not wired into a live socket handler or repurchase packet workflow.
- Java `PlayerRestrictions.canTrade` side effects are not ported here; the planner receives the gate result as an input.
- Live repurchase-set mutation, inventory mutation, packet fanout, audit logging for insufficient Kinah, repository persistence, transaction behavior, and rollback behavior remain unimplemented.
- Missing-template and add-failure paths are conservative planner blocks, not verified live Java exception/partial-mutation behavior.
- Java `ItemService.addItem` expirable registration, add/update packet types, and DAO behavior remain outside this unit.

## Parity Table Updates

- Added Session 1883 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `RepurchaseService.repurchaseFromShop`
  - `RepurchaseList` requested object-id planning
  - `ItemService.addItem(Player, Item)` composition from repurchase into `InventoryAddService`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: if JDK 25 and Maven are available, return to the condition preview Java capture draft and produce runtime output; otherwise inspect Java repurchase packet/state surfaces (`SM_REPURCHASE`, `RepurchaseService.addRepurchaseItems`, and any C# packet equivalents) before considering live repurchase wiring.

Safe alternative candidates:

- Wire `RepurchasePlanService` only after repository/packet mutation ordering and rollback behavior are scoped.
- Wire `PrivateStorePurchasePlanService` only after repository/packet mutation ordering is scoped and tests can cover no-partial-send behavior.
- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
