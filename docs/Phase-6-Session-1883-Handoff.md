# Phase 6 Session 1883 Handoff - Repurchase Source Item Planner

Date: 2026-05-31
Unit of Work: UOW-1883
Status: Completed

## What Changed

- Added `RepurchasePlanService`.
- Added `RepurchasePlanServiceTests`.
- The planner is non-live and source-reviewed against Java `RepurchaseService.repurchaseFromShop`.
- It composes UOW-1881 source-item clone support through `InventoryAddService.CreateAddItemPlan(..., sourceItem, allowInventoryOverflow: true)`.
- Covered Java repurchase behavior:
  - caller-supplied `PlayerRestrictions.canTrade(player)` gate
  - requested repurchase object ids in caller order
  - inventory-full precheck before each requested id and `STR_MSG_DICE_INVEN_ERROR` break
  - missing repurchase item skip
  - insufficient Kinah skip-and-continue behavior
  - Kinah update intent for successful repurchases
  - source-item reward add planning
  - allow-overflow behavior after the Java inventory precheck passes
  - repurchase-set removal intent after successful add planning
  - conservative missing-template and add-failure blocks
- Kept this work isolated. No live socket handler wiring, repository writes, packet fanout, Java runtime capture, audit logging, or real repurchase state mutation was enabled.

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
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
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

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InventoryAddService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InventoryAddServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PrivateStorePurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1883-Completion.md`
- `docs/Phase-6-Session-1883-Handoff.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- If continuing repurchase work, inspect:
  - Java `RepurchaseService.addRepurchaseItems`
  - Java `RepurchaseService.removeRepurchaseItems`
  - Java `RepurchaseService.getRepurchaseItems`
  - Java `SM_REPURCHASE`
  - Java `RepurchaseList`
  - C# packet equivalents around repurchase/buy item handling
  - C# `RepurchasePlanService`
- If JDK 25 and Maven become available, prioritize the condition preview Java capture draft before further source-only work.
