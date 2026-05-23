# Phase 6CH Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CG and covers Session 510.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1058 tests.

---

## Recent Work Completed

- Added a minimal runtime instance-existence hook to `WorldMapRuntimeStateTable` for the Java lookup surface used by `InstanceService.instanceExists(worldId, instanceId)`.
- `WorldMapRuntimeState` now tracks explicitly removed instance ids, normalizes instance id `0` to `1` like Java `WorldMap.getWorldMapInstance`, and can restore removed ids.
- Extended delayed teleport animation completion so the destroyed-instance fallback branch now consumes pending teleport, sends `SmPlayerInfo`, and keeps the original player position.
- Kept modeled instance existence permissive by default unless explicitly removed, because C# still lacks Java's full dynamic `WorldMapInstance` lifecycle and existing modeled teleport tests use synthetic instance ids.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 510 with the required migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `1b48e0e69` - `Handle destroyed instance teleport fallback`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `TeleportService.SpawnTask.run` | `GameServerConnection.HandleTeleportAnimationDoneAsync` | Partial | Regression Tested | Partial Parity | Destroyed-instance delayed fallback now follows Java's packet/state shape for `!InstanceService.instanceExists`: consume pending teleport, send `SmPlayerInfo`, and do not move. Full `World.spawn`, action abort parity, pet movement/spawn, legion/conqueror/instance callbacks, protection task, and effect icon refresh remain incomplete. |
| `InstanceService.instanceExists(int, int)` | `WorldMapRuntimeStateTable.InstanceExists` | Partial | Unit + Regression Tested | Partial Parity | Minimal lookup returns false for explicitly removed destination instances and false for unknown maps. It does not implement registration, ownership, difficulty, empty-instance destroy scheduling, handler callbacks, or player/team membership. |
| `WorldMap.getWorldMapInstance(int)` | `WorldMapRuntimeState.InstanceExists` | Partial | Unit Tested | Partial Parity | C# normalizes instance id `0` to `1` and tracks removed ids. Unlike Java's real concurrent map of `WorldMapInstance` objects, default modeled maps are present unless explicitly removed. |
| `WorldMap.removeWorldMapInstance(int)` | `WorldMapRuntimeState.RemoveWorldMapInstance` / `WorldMapRuntimeStateTable.RemoveWorldMapInstance` | Partial | Unit + Regression Tested | Partial Parity | Explicit removal drives the teleport fallback. Java also destroys temporary spawns, moves players, deletes objects, invokes handlers, and tears down walker formations; those side effects remain unported. |
| `WorldMap.addInstance(int, WorldMapInstance)` | `WorldMapRuntimeState.AddWorldMapInstance` / `WorldMapRuntimeStateTable.AddWorldMapInstance` | Partial | Unit Tested | Needs Verification | C# restores an explicitly removed id for modeled lookups only. It does not create or store real `WorldMapInstance` objects, handlers, max-player limits, registered players, owner ids, or spawned objects. |
| `CM_TELEPORT_ANIMATION_DONE` | `GameServerConnection.HandleTeleportAnimationDoneAsync` | Partial | Regression Tested | Partial Parity | Handler covers no-pending, normal completion, dead-player fallback, and destroyed-instance fallback. Threading remains direct async C# flow rather than Java controller `FutureTask` scheduling under `TaskId.TELEPORT`. |
| `SM_PLAYER_INFO` | `SmPlayerInfo` | Partial | Regression Tested | Needs Verification | Destroyed-instance fallback verifies packet object emission. Java-generated golden vectors/live encrypted frame validation remain missing. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1058 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full Java `InstanceService`, real `WorldMapInstance` objects/registry, full `World.spawn` fallback side effects, general controller task scheduler/futures, Java-generated packet/live socket validation, production teleport caller wiring, and instance handler/temporary spawn destroy callbacks
- Estimated overall migration completion: Phase 6 remains about 56% complete; this handoff closes one narrow delayed-teleport fallback while broad instance, world lifecycle, teleport caller, object visibility, AI, rewards, team, effects, dynamic handler, and quest systems remain open.

---

## Important Limits

- This is not a full Java `InstanceService` or `WorldMapInstance` port.
- Default instance existence is intentionally permissive unless explicitly removed; this preserves existing modeled coverage but is not the final Java-equivalent lifecycle.
- Destroyed-instance fallback sends `SmPlayerInfo` but does not implement Java `World.spawn(player)` side effects.
- `QueueDelayedTeleportAsync` remains an internal modeled helper and is not wired from a real Java-equivalent teleporter, portal, item, command, or event path.
- The pending teleport model remains a typed C# field rather than Java `FutureTask` under `TaskId.TELEPORT`.
- Packet evidence remains object-level or C# deterministic serialization; Java-generated golden vectors/live encrypted client captures are still missing.

---

## Next Unit Of Work

Recommended next unit: either deepen instance lifecycle parity with a small `WorldMapInstance` runtime object, or switch to packet certainty with Java-generated golden-vector coverage for delayed teleport packet fields.

Option A, small instance runtime object:

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
   - `game-server/src/com/aionemu/gameserver/world/WorldMap.java`
   - `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
   - `game-server/src/com/aionemu/gameserver/world/WorldMapInstanceFactory.java`
2. Re-read C#:
   - `WorldMapRuntimeState`
   - `WorldMapRuntimeStateTable`
   - any housing or rift services using runtime world maps
3. Keep the unit narrow:
   - introduce only enough `WorldMapInstance`-like runtime data for instance id, owner id, registered player ids, and max-player/full checks
   - do not attempt handler callbacks, temporary spawns, or object iteration in the same unit
   - add unit tests and update the parity table before committing

Option B, packet golden vectors:

1. Re-read Java packet sources:
   - `SM_TELEPORT_LOC.java`
   - `SM_DELETE.java`
   - `SM_PLAYER_INFO.java`
   - `SM_SYSTEM_MESSAGE.java`
2. Re-read C# packet tests around `SmTeleportLoc`, `SmDelete`, `SmPlayerInfo`, and `SmSystemMessage`.
3. Add a tiny source-derived or Java-generated reference-vector slice for teleport animation byte, object delete animation byte, player port-animation byte, and system message `1400640`.
4. Be explicit in docs whether evidence is Java-runtime-generated or only source-derived.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 508-510, `docs/Phase-6CG-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
