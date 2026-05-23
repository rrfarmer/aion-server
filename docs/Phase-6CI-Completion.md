# Phase 6CI Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CH and covers Session 511.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1059 tests.

---

## Recent Work Completed

- Introduced `WorldMapInstanceRuntimeState`, a narrow C# runtime stand-in for Java `WorldMapInstance` identity, owner, registration, tracked players, and max-player/full checks.
- Extended `WorldMapRuntimeState` and `WorldMapRuntimeStateTable` so explicitly added instances can be retrieved, registered object ids can be scanned like `InstanceService.getRegisteredInstance`, and explicit removal still drives delayed teleport fallback.
- Kept the runtime object intentionally small: no handlers, zones/regions, object iteration, temporary spawn cleanup, registered team object, empty-instance task, quest-id tracking, door state, or nearby quest scheduling were added.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 511 with migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `7f15eef1b` - `Add world map instance runtime state`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `WorldMapInstance` | `WorldMapInstanceRuntimeState` | Partial | Unit Tested | Partial Parity | C# models instance id, owner id, `isPersonal`, max-player/full checks, registered ids, and player count. Regions/zones, object dictionaries, handlers, teams, tasks, quest ids, doors, iteration, timestamps, and lifecycle side effects remain missing. |
| `WorldMap2DInstance` | `WorldMapInstanceRuntimeState` | Partial | Unit Tested | Needs Verification | Owner/personal semantics are modeled. 2D regions, zone filtering, coordinate lookup, and neighbor wiring are not ported. |
| `WorldMap3DInstance` | No dedicated C# equivalent yet | Not Started | No Tests | Unknown | Reshanta-specific 3D region behavior remains unported. |
| `WorldMapInstanceFactory` | `WorldMapRuntimeState.AddWorldMapInstance` / `WorldMapRuntimeStateTable.AddWorldMapInstance` | Partial | Unit Tested | Partial Parity | C# creates a minimal runtime instance entry. It does not call `InstanceEngine`, create handlers, select 2D vs 3D, spawn contents, or allocate ids with `WorldMap.getNextInstanceId`. |
| `WorldMap.addInstance(int, WorldMapInstance)` | `WorldMapRuntimeState.AddWorldMapInstance` | Partial | Unit Tested | Partial Parity | Explicitly added runtime instances are stored and retrievable; adding clears prior explicit removal. |
| `WorldMap.getWorldMapInstance(int)` | `WorldMapRuntimeState.TryGetWorldMapInstance` / `WorldMapRuntimeStateTable.TryGetWorldMapInstance` | Partial | Unit Tested | Partial Parity | Lookup returns explicitly stored instance entries. The broader `InstanceExists` path remains permissive until full lifecycle exists. |
| `InstanceService.getRegisteredInstance(int, int)` | `WorldMapRuntimeStateTable.GetRegisteredInstance` | Partial | Unit Tested | Partial Parity | C# scans explicitly stored runtime instances for registered object ids. Registered teams and team-disband state are not modeled. |
| `WorldMapInstance.register/isRegistered/getRegisteredCount` | `WorldMapInstanceRuntimeState.Register` / `IsRegistered` / `RegisteredCount` | Partial | Unit Tested | Partial Parity | Registered id storage is covered. C# uses locks and snapshots rather than Java concurrent sets. |
| `WorldMapInstance.getPlayerCount/isFull` | `WorldMapInstanceRuntimeState.PlayerCount` / `IsFull` | Partial | Unit Tested | Partial Parity | C# models player ids only and uses Java's `maxPlayers > 0 && getPlayerCount() >= maxPlayers` formula. It does not store `Player` objects or update fly-zone/leave-time side effects. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1059 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full Java `WorldMapInstance`, `WorldMap2DInstance` regions/zones, `WorldMap3DInstance`, `WorldMapInstanceFactory` handler/engine integration, full `InstanceService`, empty-instance destroy scheduling, temporary spawn/object lifecycle callbacks, and production teleport/instance caller wiring
- Estimated overall migration completion: Phase 6 remains about 56% complete; this handoff adds useful instance runtime scaffolding but broad game-core systems remain open.

---

## Important Limits

- This is still not a full Java `WorldMapInstance` port.
- Instance id allocation via Java `WorldMap.getNextInstanceId` is not modeled; callers still supply ids.
- C# `InstanceExists` remains intentionally permissive unless an instance id is explicitly removed, preserving modeled teleport tests until a real lifecycle exists.
- Threading differs: C# uses a private lock and snapshots, while Java uses concurrent maps/sets.
- No handlers, regions, zones, object iteration, registered teams, empty-instance destroy tasks, temporary spawn cleanup, quest ids, door state, or nearby quest updates exist yet.
- No serialization, database persistence, date/time handling, precision/rounding, reflection behavior, or packet wire format changed in this window.

---

## Next Unit Of Work

Recommended next unit: add instance id allocation and a narrow `GetNextAvailableInstance` / `GetOrRegisterInstance` slice modeled after Java `InstanceService`.

Suggested shape:

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
   - `game-server/src/com/aionemu/gameserver/world/WorldMap.java`
   - `game-server/src/com/aionemu/gameserver/world/WorldMapInstanceFactory.java`
2. Re-read C#:
   - `WorldMapRuntimeState`
   - `WorldMapRuntimeStateTable`
   - `WorldMapInstanceRuntimeState`
   - `GameServerRuntimeContext`
3. Keep it narrow:
   - add next-instance-id allocation equivalent to `WorldMap.getNextInstanceId`
   - add a small service/helper that creates a runtime instance with owner/maxPlayers and registers a player id
   - add `GetOrRegisterInstance` behavior over explicit runtime instances
   - do not model handlers, spawns, auto-destroy tasks, teams, or instance cooldown/max-member data in the same unit
   - update `docs/PHASE-6-PROGRESS.md` with the required parity table before committing

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
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 510-511, `docs/Phase-6CH-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
