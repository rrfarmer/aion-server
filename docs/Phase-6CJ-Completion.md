# Phase 6CJ Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CI and covers Session 512.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1061 tests.

---

## Recent Work Completed

- Added map-local next-instance-id allocation to `WorldMapRuntimeState`, seeded from Java `WorldMap.getInstanceCount()` behavior (`twin_count` or `1`), so dynamic runtime instances follow the default instances.
- Added `CreateNextWorldMapInstance` on `WorldMapRuntimeState` and `WorldMapRuntimeStateTable`, mirroring the allocation/store shape of Java `WorldMapInstanceFactory.createWorldMapInstance`.
- Introduced `InstanceRuntimeService` as a narrow C# service slice for Java `InstanceService.getNextAvailableInstance`, `getNextAvailableInstance(worldId, player)`, and `getOrRegisterInstance`.
- Added tests for registration-on-create, registered-instance reuse, next id allocation, non-instance guard behavior, and missing-map guard behavior.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 512 with migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `7a3c4a07b` - `Add instance runtime allocation service`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `WorldMap.getNextInstanceId()` | `WorldMapRuntimeState.GetNextInstanceId` | Partial | Unit Tested | Partial Parity | C# increments a map-local counter seeded from `twin_count` or `1`. Java beginner twin count metadata is still absent from `WorldMapSummary`. |
| `WorldMapInstanceFactory.createWorldMapInstance` | `WorldMapRuntimeState.CreateNextWorldMapInstance` / `WorldMapRuntimeStateTable.CreateNextWorldMapInstance` | Partial | Unit Tested | Partial Parity | C# allocates and stores narrow runtime instances. It does not select 2D vs 3D, call `InstanceEngine`, construct handlers, spawn contents, or initialize regions/zones. |
| `InstanceService.getNextAvailableInstance(int, int, byte, Function, int, boolean)` | `InstanceRuntimeService.GetNextAvailableInstance` | Partial | Unit Tested | Partial Parity | C# validates map existence and instance-map status, then creates a runtime instance. Difficulty id, handler supplier, max-player config, auto-destroy, active events, spawn engine calls, `onInstanceCreate`, empty-instance scheduling, Panesterra restrictions, and logging are missing. |
| `InstanceService.getNextAvailableInstance(int, Player)` | `InstanceRuntimeService.GetNextAvailableInstanceForPlayer` | Partial | Unit Tested | Partial Parity | C# creates a runtime instance and registers the player object id. It does not yet read `INSTANCE_COOLTIME_DATA.getMaxMemberCount(worldId, race)`. |
| `InstanceService.getOrRegisterInstance(int, Player)` | `InstanceRuntimeService.GetOrRegisterInstance` | Partial | Unit Tested | Partial Parity | C# returns an existing registered runtime instance or creates/registers a new one. Player race, team registration, instance cooldowns, full-instance fallback, personal owner routing, and callbacks remain missing. |
| `InstanceService.getRegisteredInstance(int, int)` | `WorldMapRuntimeStateTable.GetRegisteredInstance` | Partial | Unit Tested | Partial Parity | Reuse behavior is tested through `GetOrRegisterInstance`; C# scans narrow runtime entries only. |
| `WorldMapTemplate.isInstance()` | `WorldMapSummary.IsInstance` | Partial | Unit Tested | Partial Parity | `GetNextAvailableInstance` rejects non-instance maps. Panesterra `WorldType` behavior is not modeled because static summaries do not carry `WorldType`. |
| `java.lang.UnsupportedOperationException` | `Aion.GameServer.Services.UnsupportedOperationException` | Refactored | Unit Tested | Intentional Difference | C# uses a small local exception type for the Java guard meaning. It is not Java reflection-compatible. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1061 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full Java `InstanceService`, `WorldMapInstanceFactory` handler/engine integration, beginner twin count metadata, Panesterra/WorldType metadata, instance cooldown/member-count config, full `WorldMapInstance` object lifecycle, spawn/event/temporary spawn integration, and production teleport/instance caller wiring
- Estimated overall migration completion: Phase 6 remains about 56% complete; this adds another instance runtime building block but the broader game-core remains open.

---

## Important Limits

- This is still not a full Java `InstanceService`.
- No difficulty id, handler supplier, event spawns, `SpawnEngine`, `InstanceHandler`, auto-destroy scheduling, Panesterra checks, or logging were ported.
- The service takes object ids and explicit max-player values; it does not yet consume full `Player`, race, team, membership, or instance cooldown/member-count config.
- Beginner twin count metadata is not in `WorldMapSummary`; maps depending on it remain needs-verification.
- Threading differs: C# uses `Interlocked` and locks, while Java uses `AtomicInteger` and concurrent collections.
- No serialization, database persistence, date/time handling, precision/rounding, reflection behavior, or packet wire format changed in this window.

---

## Next Unit Of Work

Recommended next unit: add a tiny `Player`-aware wrapper for `GetNextAvailableInstance(worldId, Player)` that reads max-member data from `InstanceCooltimeTable` where possible.

Suggested shape:

1. Re-read Java:
   - `InstanceService.getNextAvailableInstance(int worldId, Player player)`
   - `DataManager.INSTANCE_COOLTIME_DATA.getMaxMemberCount(worldId, player.getRace())`
2. Re-read C#:
   - `InstanceRuntimeService`
   - `InstanceCooltimeTable` / `InstanceCooltimeSummary`
   - `Player` race/object id fields
   - `WorldMapRuntimeStateTable`
3. Keep it narrow:
   - add a helper overload that accepts `Player`, `InstanceCooltimeTable`, and world map runtime state
   - use max-member count where available, otherwise document fallback behavior
   - do not add cooldown lockouts, instance entry validation, handlers, spawns, or auto-destroy tasks
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative unit:

- Add Java-generated or source-derived golden-vector coverage for delayed teleport packet fields: `SM_TELEPORT_LOC`, `SM_DELETE`, `SM_PLAYER_INFO`, and `SM_SYSTEM_MESSAGE` id `1400640`.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 511-512, `docs/Phase-6CI-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
