# Phase 6 Session 2340 Completion - Resolve Instance Exit Fallback

## Scope

Ported a modeled instance-exit destination lookup for Java `TeleportService.moveToInstanceExit(...)` without enabling live teleport dispatch.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/portal/InstanceExit.java`

Java behavior used:

- `InstanceExitData.afterUnmarshal(...)` groups `instance_exit` rows by `instance_id`.
- `InstanceExitData.getInstanceExit(worldId, race)` returns the first grouped exit whose race is `PC_ALL` or the player's race.
- `InstanceExit.race` defaults to `PC_ALL`; `h` defaults to Java byte `0`.
- `TeleportService.moveToInstanceExit(...)` teleports to the exit only when `InstanceService.instanceExists(exitWorld, 1)` is true.
- Missing exits, or exits whose target world instance `1` does not exist, fall back to `moveToBindLocation(...)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Added:

- `dotnetConversion/src/Aion.GameServer/Dataholders/InstanceExitTable.cs`

Updated:

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- Added `InstanceExitTable` and `InstanceExitSummary` for Java-shaped instance-exit rows.
- Loaded `<instance_exit>` cache rows into `StaticData.InstanceExits`, including Java defaults for race and heading.
- Added `InstanceRuntimeService.ResolveInstanceExit(...)` to model Java exit-destination vs bind-location fallback decisions.
- Added focused tests for XML loading defaults, race matching, target-world existence, and fallback reasons.

Known limitations:

- The plan does not move a player to the resolved destination.
- Bind-location destination resolution remains outside this UOW.
- Live forced-exit send and teleport dispatch remain pending.

## Validation Decision

- Changed surface: static data parser plus modeled instance-exit resolver.
- Specific behavior/contract: Java `instance_exit` rows are grouped by instance world id, race defaults to `PC_ALL`, heading defaults to `0`, exact race or `PC_ALL` exits are selected in source order, and Java `moveToInstanceExit` falls back to bind location when no exit or no exit-world instance `1` exists.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.StaticData_LoadsInstanceExitSummariesWithJavaRaceFallbacks|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `InstanceExitData.getInstanceExit(...)` or `TeleportService.moveToInstanceExit(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This UOW adds a non-live static-data table and resolver plan; it does not enable live teleport dispatch or shared packet/runtime side effects.
- Broad .NET decision: skipped full project/solution validation; the filtered command built the affected project and covered the edited behavior.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.InstanceExitData` | `Aion.GameServer.Dataholders.InstanceExitTable` | Dataholder | Partial | Unit Tested | Partial Parity | C# groups exits by instance world id and preserves source order. `getInstanceExit` behavior for `PC_ALL`, exact race, and missing rows is tested from reviewed Java logic. JAXB lifecycle and Java enum parsing are not runtime-compared. |
| `com.aionemu.gameserver.model.templates.portal.InstanceExit` | `Aion.GameServer.Dataholders.InstanceExitSummary` | DTO | Partial | Unit Tested | Partial Parity | Scalar attributes and Java defaults for `race=PC_ALL` and `h=0` are covered by static-data loading test. Setter/getter API parity is not needed for current C# immutable summary shape. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.ResolveInstanceExit` | Service Plan | Partial | Unit Tested | Partial Parity | C# resolves destination vs bind fallback using Java's `InstanceService.instanceExists(exitWorld, 1)` condition. Live teleport mutation and bind-location resolution remain pending. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StaticData_LoadsInstanceExitSummariesWithJavaRaceFallbacks` | Unit | Java source review | Static cache rows load into `InstanceExitTable`, preserve race-specific exits, default race to `PC_ALL`, default heading to `0`, and return null for missing source world ids. | Focused C# test plus Java source review. | Does not runtime-compare against JAXB output. |
| `InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService` | Unit | Java source review | Resolver chooses race destination, PC_ALL destination, missing-exit bind fallback, and missing exit-world-instance bind fallback. | Focused C# test plus Java source review. | Does not dispatch live teleport or resolve actual bind coordinates. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live player teleport dispatch for forced exits.
- Bind-location coordinate resolution and packet behavior.
- Live destroy wiring with forced-exit plans, temporary cleanup, non-player cleanup, and handler notification.
- Empty-instance checker scheduling/cancellation.
- Walker formation cleanup on instance destroy.

## Commit

Commit message:

```text
[Phase 6][UOW-2340] Resolve instance exit fallback
```

