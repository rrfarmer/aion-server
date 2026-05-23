# Phase 6CG Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CF and covers Sessions 508-509.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1056 tests.

---

## Recent Work Completed

- Modeled the Java delayed teleport map/instance-change instance-open notification from `TeleportService.SpawnTask.run`.
- Added `SmSystemMessage.InstanceDungeonOpenedForSelf(worldId)` for Java `STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF(worldId)` with message id `1400640`.
- Preserved Java `WorldMapType.isPersonal()` exclusion for the four source-listed personal housing worlds, so personal instance maps skip the instance-open message.
- Added delayed teleport regressions for a normal instance map receiving the message and a personal instance map skipping it.
- Modeled the Java delayed teleport dead-player fallback: if the player is dead when `CM_TELEPORT_ANIMATION_DONE` consumes the pending teleport, C# clears the pending teleport, sends `SmPlayerInfo`, and does not move to the destination.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 509 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `ba44283f5` - `Send delayed teleport instance open message`
- `0094a7e00` - `Handle dead delayed teleport fallback`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `TeleportService.SpawnTask.run` | `GameServerConnection.HandleTeleportAnimationDoneAsync` + `SendDelayedTeleportCompletionPacketsAsync` | Partial | Regression Tested | Partial Parity | C# now covers instance-open message ordering and dead-player delayed fallback. Instance-exists fallback, full `World.spawn`, pet movement/spawn, legion/conqueror/instance callbacks, protection task, and effect icon refresh remain incomplete. |
| `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF` | `SmSystemMessage.InstanceDungeonOpenedForSelf` | Complete | Unit Tested | Partial Parity | Message id `1400640` and world-id parameter serialization are deterministic-tested in C#. No Java-generated packet vector or live socket capture was run. |
| `WorldMapType.getWorld(int).isPersonal()` | `GameServerConnection.IsPersonalWorld` | Partial | Regression Tested | Partial Parity | C# preserves the Java personal-world exclusion for `700020000`, `710020000`, `720010000`, and `730010000`. This is a narrow local helper, not a full shared `WorldMapType` port. |
| `DataManager.WORLD_MAPS_DATA` / `WorldMapTemplate.isInstance()` | `StaticData.WorldMaps` / `WorldMapSummary.IsInstance` | Partial | Regression Tested | Partial Parity | Teleport branch uses static `IsInstance` and tests verify both `300030000` and `720010000` are instance maps. Static data still does not carry Java's personal-world bit. |
| `CM_TELEPORT_ANIMATION_DONE` | `GameServerConnection.HandleTeleportAnimationDoneAsync` | Partial | Regression Tested | Partial Parity | Handler covers no-pending, normal completion, packet-order branch, instance-open message, and dead-player fallback. It remains a direct async C# flow rather than Java controller `FutureTask` scheduling. |
| `PlayerController.getAndRemoveTask(TaskId.TELEPORT)` | `PlayerTeleportService.CancelPendingTeleport` | Partial | Regression Tested | Partial Parity | Pending teleport can now be consumed without movement for fallback. General controller task registry, cancellation, and Java `FutureTask` behavior remain missing. |
| `Player.isDead()` | `Player.IsInState(PlayerCreatureState.Dead)` plus HP check | Partial | Regression Tested | Needs Verification | C# treats either `Dead` state or `LifeStats.CurrentHp <= 0` as dead for delayed teleport fallback. Java `isDead()` internals were not separately ported in this window. |
| `SM_PLAYER_INFO` | `SmPlayerInfo` | Partial | Unit + Regression Tested | Needs Verification | Dead fallback verifies packet object emission, and earlier sessions cover port-animation byte source-derived behavior. Java-generated golden vectors/live frames remain missing. |
| `World.spawn(Player)` | No full C# equivalent | Not Started | No Tests | Unknown | Java respawns the player after fallback. C# only sends the modeled `SmPlayerInfo`; spawned-state transitions, known-list rebuild, visibility fanout, zone callbacks, and object lifecycle remain open. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1056 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: Java instance lifecycle/`InstanceService.instanceExists`, full `World.spawn` side effects, general controller task scheduler/futures, Java-generated packet/live socket validation, full dead-state predicate parity, production teleport caller wiring, full shared `WorldMapType` metadata, broader map/instance spawn side effects, pet/legion/conqueror/instance callbacks, and broader `CM_LEVEL_READY` service fanout
- Estimated overall migration completion: Phase 6 remains about 56% complete; this handoff closes two narrow delayed-teleport side effects while real teleport callers, full spawn/despawn lifecycle, pets, legion/instance callbacks, generic object lifecycle, zone handlers, AI, rewards, team distribution, effects, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- `QueueDelayedTeleportAsync` remains an internal modeled helper and is not wired from a real Java-equivalent teleporter, portal, item, command, or event path.
- Java `InstanceService.instanceExists(worldId, instanceId)` fallback is still missing because C# does not yet have a real player-instance lifecycle/registry.
- The dead-player fallback sends `SmPlayerInfo` but does not implement Java `World.spawn(player)` side effects.
- The system-message and player-info assertions are C# deterministic checks, not Java-generated golden vectors or live encrypted client captures.
- The personal-world exclusion is local to the teleport branch; future callers needing map-type metadata should introduce a shared `WorldMapType`-equivalent model instead of duplicating ids.
- C# still uses a typed pending teleport record rather than Java `FutureTask` under `TaskId.TELEPORT`.

---

## Next Unit Of Work

Recommended next unit: continue delayed teleport fallback parity by introducing a minimal instance-existence runtime model for `InstanceService.instanceExists(worldId, instanceId)`, then cover the destroyed-instance fallback.

Suggested shape:

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`, especially `SpawnTask.run`
   - `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
   - Java world/instance classes that back `InstanceService.instanceExists`
2. Re-read C#:
   - `GameServerConnection.HandleTeleportAnimationDoneAsync`
   - `PlayerTeleportService`
   - `GameServerRuntimeContext`
   - `WorldMapRuntimeStateTable` / `WorldMapRuntimeState`
3. Keep the unit narrow:
   - add only enough runtime instance state to let tests mark a pending destination instance as missing
   - default behavior should preserve current tests until a real instance lifecycle exists
   - on missing instance, consume pending teleport, send `SmPlayerInfo`, keep the original position, and document that full `World.spawn` remains missing
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative packet-focused unit:

1. Add Java-generated or source-derived golden-vector coverage for `SM_TELEPORT_LOC`, `SM_DELETE`, `SM_PLAYER_INFO`, and `SM_SYSTEM_MESSAGE` id `1400640`.
2. Keep vector scope tiny and explicitly mark any non-Java-runtime expectations as source-derived rather than verified parity.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerTeleportServiceTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 508-509, `docs/Phase-6CF-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
