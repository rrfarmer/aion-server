# Phase 6 Session 1881 Completion - Inventory Source Item Clone Planner

Date: 2026-05-31
Unit of Work: UOW-1881
Status: Completed

## Scope

- Performed Work Discovery around Java `ItemService.addItem` source-item behavior.
- Added planner-level C# support for Java `ItemService.addItem(player, sourceItem)` non-stackable clone semantics.
- Added focused tests for copied and deliberately non-copied fields.
- Kept live caller wiring, packets, persistence, and Java runtime capture out of scope.

## What Changed

- Updated `InventoryAddService.CreateAddItemPlan` with an optional `sourceItem` parameter.
- Added `CopyNonStackableSourceItemInfo` for reviewed Java `ItemService.copyItemInfo` parity.
- Copied non-stackable source state for color, creator, soul-bind, enchant, enchant bonus, skin, optional sockets, tune count, random bonus, tempering, amplified state, buff skill, manastones, godstone, and idian stone.
- Preserved fresh target identity/state for object id, item id, count, owner, location, slot, expiration, activation count, and persistent state.
- Left fusion attributes, charge, and fusion stones uncopied, matching Java `copyItemInfo`.
- Preserved stackable source-item behavior by not copying source metadata for stackable new rows.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InventoryAddServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InventoryAddServiceTests|FullyQualifiedName~AssemblyItemServiceTests|FullyQualifiedName~ExpExtractServiceTests|FullyQualifiedName~EnchantServiceTests|FullyQualifiedName~WorldNpcLootServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused inventory-add slice passed with 10 tests.
- Related reward/inventory packet slice passed with 472 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4727 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- The optional `sourceItem` parameter is not yet wired into a live caller.
- Java `ExpireTimerTask.registerExpirable`, packet add/update types, DAO persistence, transaction behavior, and dice-inventory messaging remain outside this unit.
- Java `setItemColor` to C# `Color`/`ColorExpires` mapping is source-reviewed but not runtime-proved.
- Live reward object-id allocation, persistence, packet bytes, rollback behavior, and full source-item clone workflows remain unverified.

## Parity Table Updates

- Added Session 1881 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `ItemService.addItem(Player, Item)` non-stackable source clone planner support
  - stackable source-item no-copy guard behavior
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: if JDK 25 and Maven are available, return to the condition preview Java capture draft and produce runtime output; otherwise inspect a concrete Java source-item clone caller such as `RepurchaseService` or `PrivateStoreService` and decide whether the new planner parameter can be wired safely with packet/persistence tests.

Safe alternative candidates:

- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
