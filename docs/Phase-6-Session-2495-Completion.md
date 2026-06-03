# Phase 6 Session 2495 Completion

## UOW

[Phase 6] UOW-2495: Add Vortex stopInvasion runtime snapshot collector preview

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexStopInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added a non-live `VortexStopInvasionRuntimeSnapshotCollectorService`.
- The collector translates a `VortexInvasionSnapshot` plus available player, invader-Kisk, spawned-NPC, and invader-alliance candidates into `VortexStopInvasionSnapshotRequest`.
- The collector derives passed-player ids from the runtime snapshot, matching the Java stop/kick dependency on `VortexLocation.vortexController.passedPlayers`.
- Player and alliance candidates are filtered to runtime invader ids; Kisk and spawned-NPC candidates are treated as already scoped to the Vortex location, matching Java's location-owned `invadersKisks` and `spawned` collections.
- Scope remains metadata-only. It does not read or mutate live world maps, send packets, teleport players, mutate alliances, mutate participant maps, mutate passed-player maps, sync passed-player state, kill Kisks, despawn NPCs, or spawn NPCs.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `StopRuntimeSnapshotCollector_CapturesJavaStopInputsAndFeedsCoordinatorWithoutLiveExecution` | Unit | `Invasion.stopInvasion`, `Invasion.kickPlayer`, and `VortexLocation` source review | Collector captures Java-relevant stop inputs and feeds coordinator kick/removal metadata | Focused C# test validates invader filtering, passed-player ids, invader Kisk metadata, spawned-NPC metadata, alliance filtering, coordinator stop order, kick/removal plan, sync count, and no-live flags | Candidate objects must still be supplied by future production adapters |
| `StopRuntimeSnapshotCollector_MissingSnapshotReturnsEmptyRequest` | Unit | `VortexService.stopInvasion` missing-active guard source review | Missing runtime snapshot produces an inert empty request | Focused C# test validates no invader, Kisk, spawned-NPC, alliance, or passed-player snapshots are emitted | Does not exercise production runtime lookup |

## Validation Decision

- Changed surface: non-live Vortex stop snapshot collection metadata and focused tests.
- Specific behavior/contract: C# stopInvasion snapshot collection captures Java-relevant stop/kick inputs for coordinator metadata while preserving disabled live side effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 92 tests. Existing nullable/analyzer warnings were emitted from unrelated files.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex stop/removal fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live snapshot collection metadata and tests, without enabling live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, spawn, scheduler dispatch, or zone-player/Kisk map mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited collector/coordinator metadata path.
- Why this scope is sufficient: the new code is an inert collector over supplied snapshot and candidate collections.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.stopInvasion` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService` | Runtime collector | Partial | Unit Tested | Partial Parity | C# collects stop-time metadata inputs for coordinator plans. Live stop side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexStopInvasionRuntimeSnapshotCollectorService` | Runtime collector | Partial | Unit Tested | Partial Parity | C# supplies invader, alliance, and passed-player snapshots needed by kick/removal metadata. Live packet, teleport, alliance, participant, passed-player, and sync mutation remain disabled. |
| `com.aionemu.gameserver.model.vortex.VortexLocation` | `Aion.GameServer.Services.VortexInvasionSnapshot` plus collector candidates | Runtime metadata | Partial | Unit Tested | Partial Parity | C# runtime snapshot provides participant and passed-player ids; Kisk, spawned-NPC, and alliance candidates still come from supplied adapter inputs. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `Aion.GameServer.Services.VortexStopInvaderKiskSnapshot` via collector | Runtime metadata | Partial | Unit Tested | Partial Parity | C# collector carries Kisk death intent metadata only. Live Kisk controller death remains disabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live side effects remain disabled across packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, Kisk death, despawn, and spawn.
- The collector depends on supplied candidate collections for players, invader Kisks, spawned NPCs, and alliance metadata.
- Production adapters still need to provide real Vortex location/player/world/alliance/Kisk/spawn inputs before live use.
