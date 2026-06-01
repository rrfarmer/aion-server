# Phase 6 Session 1951 Completion - SM_REPURCHASE Snapshot Adapter

Date: 2026-06-01
Unit of Work: UOW-1951
Status: Completed

## Scope

- Added a non-live C# adapter for the Java `SM_REPURCHASE(Player, npcId)` constructor boundary.
- The adapter builds `SmRepurchase` from supplied `RepurchaseService.getRepurchaseItems`-equivalent snapshot facts without querying singleton state or sending packets.
- Kept parity claims conservative: this unit improves snapshot composition, not live repurchase behavior.

## Work Discovery

- Re-read Session 1950 completion and handoff.
- Inspected Java `SM_REPURCHASE(Player, int)`, `RepurchaseService.getRepurchaseItems`, and `DialogService` BUY_AGAIN.
- Reviewed C# `SmRepurchase`, `NpcDialogServiceSelectPlanService`, `RepurchasePlanService`, and existing repurchase/dialog tests.

## Changes

- Added `RepurchasePacketSnapshotPlanService`.
- Added `RepurchasePacketSnapshotPlan` and `RepurchasePacketSnapshotPlanStatus`.
- The snapshot adapter:
  - accepts target object id and supplied `RepurchaseSourceItem` facts
  - resolves templates through `ItemTemplateTable`
  - builds the existing `SmRepurchase` packet when all templates are present
  - blocks with explicit missing template IDs when any template is missing
  - records live singleton query and send flags as not run
- Extended `NpcDialogServiceSelectPlanService` so BUY_AGAIN descriptors can carry the snapshot plan and composed packet.
- Added focused tests for empty snapshot bytes, simple non-equipment Java-golden bytes, missing-template blocking, and dialog descriptor composition.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused C# slice passed with 26 tests.
- Focused Java `SM_REPURCHASE_GoldenTest` passed with 3 test methods.
- Java/Maven reactor test run passed with 1 commons test and 22 game-server tests.
- Broad C# game-server suite passed with 4994 tests.

## Known Gaps

- The adapter uses supplied facts and does not query live singleton repurchase state.
- Java live `HashSet` iteration order from `RepurchaseService.addRepurchaseItems` remains unverified.
- Live BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Advanced item-info blob state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePacketSnapshotPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePacketSnapshotPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1951-Completion.md`
- `docs/Phase-6-Session-1951-Handoff.md`

## Parity Position

- Partial Parity for disabled repurchase packet snapshot composition.
- The composed packet output reuses existing Java golden byte evidence for empty and simple non-equipment snapshots.
- No verified parity is claimed for live singleton state, live send ordering, live iteration order, or persistence behavior.
