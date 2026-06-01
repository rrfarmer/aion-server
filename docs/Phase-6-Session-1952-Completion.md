# Phase 6 Session 1952 Completion - SM_REPURCHASE Snapshot Ordering Evidence

Date: 2026-06-01
Unit of Work: UOW-1952
Status: Completed

## Scope

- Captured the remaining ordering boundary around Java `SM_REPURCHASE(Player, npcId)` snapshot serialization.
- Verified that Java packet item row order follows the current `RepurchaseService.getRepurchaseItems(playerId)` set iteration.
- Locked the C# disabled snapshot adapter to preserve supplied fact order, without pretending it ports Java live `HashSet` state.

## Work Discovery

- Re-read Session 1951 completion and handoff.
- Inspected Java `RepurchaseService.addRepurchaseItems`, `RepurchaseService.getRepurchaseItems`, `SM_REPURCHASE(Player, npcId)`, and `SM_REPURCHASE.writeImpl`.
- Inspected Java `AionObject.hashCode/equals`, confirming object-id based `HashSet` behavior.
- Reviewed C# `RepurchasePacketSnapshotPlanService` and its tests.

## Changes

- Added Java test `constructor_usesRepurchaseServiceSetIterationOrderForItems`.
- The Java test inserts two synthetic repurchase items through `RepurchaseService.addRepurchaseItems`, captures the service set iteration order, serializes `SM_REPURCHASE(Player, npcId)`, parses the packet item object IDs, and compares packet order to service set order.
- Added C# test `CreateDisabledPlan_PreservesSuppliedRepurchaseItemOrder`.
- The C# test composes a two-item disabled snapshot and parses the packet to prove the adapter preserves supplied order.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_REPURCHASE_GoldenTest` passed with 4 test methods.
- Focused C# repurchase snapshot/packet slice passed with 8 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.
- Broad C# game-server suite passed with 4995 tests.

## Known Gaps

- Java runtime coverage proves packet order follows service set iteration, but does not prove stable business ordering for every object-id set or JVM implementation.
- C# remains supplied-order only; it does not port Java singleton map state, `HashSet` replacement, or live mutation timing.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_REPURCHASE_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePacketSnapshotPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1952-Completion.md`
- `docs/Phase-6-Session-1952-Handoff.md`

## Parity Position

- Partial Parity for the repurchase packet snapshot ordering boundary.
- Java runtime evidence now covers the service-set-to-packet order relationship.
- C# supplied-order behavior is unit tested and remains intentionally non-live until repurchase singleton state is ported.
