# Phase 6 Session 2342 Completion - Resolve Bind Location Destination

## Scope

Ported a non-live bind-location destination resolver for Java `TeleportService.moveToBindLocation(...)`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/PlayerInitialData.java`

Java behavior used:

- `moveToBindLocation(player)` uses `player.getBindPoint()` when present.
- Without a bind point, Java uses `DataManager.PLAYER_INITIAL_DATA.getSpawnLocation(player.getRace())`.
- Java `teleportTo(player, worldId, x, y, z, h)` keeps the current instance id when teleporting within the same world and uses instance `1` when crossing worlds.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerTeleportServiceTests.cs`

Implemented:

- Added `PlayerTeleportService.ResolveBindLocation(...)`.
- Added `BindLocationResolutionPlan` and `BindLocationResolutionStatus`.
- Added focused tests for player bind point precedence, initial spawn fallback, same-world instance retention, cross-world instance `1`, and missing modeled initial data.

Known limitations:

- This is a destination plan only; it does not mutate player position.
- Missing initial spawn data is recorded as a modeled-data gap instead of reproducing Java's likely runtime failure path.
- The instance-exit fallback plan has not yet been composed with the bind-location destination plan.

## Validation Decision

- Changed surface: non-live bind-location destination resolver plus adjacent teleport service tests.
- Specific behavior/contract: Java bind fallback chooses player bind point before race initial spawn and applies Java `teleportTo(player, worldId, x, y, z, h)` instance-id rules.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerTeleportServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `TeleportService.moveToBindLocation(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This UOW adds a non-live resolver only.
- Broad .NET decision: skipped full project/solution validation; the filtered command built the affected project and covered the edited service behavior.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToBindLocation` | `Aion.GameServer.Services.PlayerTeleportService.ResolveBindLocation` | Service Plan | Partial | Unit Tested | Partial Parity | C# plans bind-point and initial-spawn destinations with Java instance-id rules. Live `teleportTo` mutation, packet fanout, and exact missing-data exception behavior are not implemented. |
| `com.aionemu.gameserver.dataholders.PlayerInitialData.getSpawnLocation` | `Aion.GameServer.Dataholders.PlayerInitialDataTable.GetSpawnLocation` | Dataholder | Partial | Unit Tested | Partial Parity | Existing C# dataholder supplies race spawn rows used by the bind resolver. This UOW tests resolver consumption, not full JAXB/runtime data parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ResolveBindLocationUsesPlayerBindPointBeforeInitialSpawnLikeJavaMoveToBindLocation` | Unit | Java source review | Player bind point wins over initial race spawn and cross-world target uses instance `1`. | Focused C# test plus Java source review. | No live teleport. |
| `ResolveBindLocationUsesInitialSpawnWhenPlayerHasNoBindPointLikeJavaMoveToBindLocation` | Unit | Java source review | Initial race spawn is used when no bind point exists and same-world target keeps current instance id. | Focused C# test plus Java source review. | No live teleport. |
| `ResolveBindLocationRecordsMissingInitialSpawnWhenModeledDataIsUnavailable` | Unit | C# modeled-data guard with Java source review | Missing C# initial data is surfaced explicitly instead of hidden. | Focused C# test. | Java's exact failure path for missing required XML data is not reproduced. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 2
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Compose instance-exit bind fallback with bind-location destination resolution.
- Live forced-exit packet send and teleport dispatch.
- Live destroy wiring with all cleanup callbacks and handler notifications.
- Walker formation cleanup on instance destroy.
- Empty-instance checker scheduling/cancellation.

## Commit

Commit message:

```text
[Phase 6][UOW-2342] Resolve bind location destination
```

