# Phase 6BY Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BX and covers Sessions 492-493.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1041 tests.

---

## Recent Work Completed

- Wired express-mail postman spawn to revalidate PVP/FORT counters after direct C# world insertion.
- Wired express-mail postman dismissal/removal to clear modeled PVP/FORT counters when the postman is removed from world storage.
- Added real Java static-data coverage for postman spawn/dismiss inside `PVP_87_210040000`.
- Added optional `CreaturePvpZoneCounterService` wiring to `PlayerEnterWorldService`.
- Cleared modeled PVP/FORT counters when enter-world rollback removes a player after post-world-insert online marking fails.
- Cleared modeled PVP/FORT counters from `PlayerEnterWorldService` catch rollback and service-level `LeaveWorldAsync` removals, protecting service use that bypasses the connection-level cleanup.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 493 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `6e05cc3aa` - `Clean pvp counters for postman removals`
- `f56967b21` - `Clear pvp counters on enter-world rollback`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `CM_READ_EXPRESS_MAIL` postman dismissal | `GameServerConnection.HandleReadExpressMailAsync` / `DismissPostmanAsync` | Partial | Integration Tested | Partial Parity | Action 0 postman dismissal now clears modeled PVP/FORT counters. Java cooldown task identity, flight restriction ordering, controller delete callbacks, and known-list side effects remain incomplete. |
| `VisibleObjectSpawner.spawnPostman` | `GameServerConnection.SpawnPostmanAsync` / `PostmanNpc.Create` | Partial | Integration Tested | Partial Parity | Postman direct world insertion now revalidates counters against real Java PVP static data. Java `GeoService.getClosestCollision`, `PlayerAwareKnownList`, `EffectController`, and full controller setup remain unported. |
| `SpawnEngine.bringIntoWorld` / `World.spawn` | Direct postman `_world.TryAddObject` revalidation hook | Partial | Integration Tested | Intentional Difference | C# calls modeled revalidation immediately because full map regions and zone instances are not yet ported. Live encrypted socket ordering is not verified. |
| `VisibleObjectController.delete` / `World.despawn` | Direct postman `_world.TryRemoveObject` cleanup hook | Partial | Integration Tested | Intentional Difference | C# clears counters directly on removal. Java `onBeforeDespawn`, known-list removal, zone handlers, controller callbacks, and respawn/controller tasks remain missing. |
| `PlayerEnterWorldService.enterWorld` failure path | `PlayerEnterWorldService.EnterWorldAsync` rollback cleanup | Partial | Regression Tested | Partial Parity | Online-mark failure after world insertion now removes the player and clears modeled counters. Java account refresh, passkey, punishment, multi-client, packet response, and retail enter-world sequence remain partial. |
| `PlayerController.delete` failure cleanup | `PlayerEnterWorldService.EnterWorldAsync` catch/rollback branches | Partial | Regression Tested | Intentional Difference | Java deletes through the controller; C# directly removes from world storage and clears counters until controller/map-region layers are available. |
| `PlayerLeaveWorldService.leaveWorld` | `PlayerEnterWorldService.LeaveWorldAsync` | Partial | Unit Tested | Partial Parity | Service-level logout removal now clears modeled counters, supplementing connection-level cleanup. Java logout save ordering, task cleanup, known-list updates, and zone leave callbacks remain incomplete. |
| `Creature.revalidateZones` / `MapRegion.revalidateZones` | `CreaturePvpZoneRevalidationService` + `CreaturePvpZoneCounterService` | Partial | Unit + Integration Tested | Partial Parity | Postman spawn and player rollback/removal direct paths now feed or clear modeled counters. Zone priority, full-map zones, level checks, neighboring regions, queued scheduler, and handler callbacks remain open. |
| `PvPZoneInstance.onEnter/onLeave` | `CreaturePvpZoneCounterService` through postman/player hooks | Partial | Unit + Regression Tested | Partial Parity | Tests prove postman PVP entry/leave cleanup and player rollback cleanup. Java handler ordering and side effects remain unverified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1041 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: kisk save-failure rollback regression, generic direct world-removal cleanup audit, Java zone handlers/controller callbacks, queued zone scheduler/levels, `GeoService.getClosestCollision`, full controller delete side effects, full known-list/region side effects, and live socket/persistence ordering
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change zone revalidation, remaining direct-removal cleanup, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- C# postman placement still uses direct heading math from the previous port. Java also runs `GeoService.getClosestCollision`, which is not represented in this slice.
- C# postman/player cleanup clears modeled counters directly and does not execute Java `onLeaveZone` handlers, controller callbacks, quest/material handlers, fortress observers, instance callbacks, or known-list region cleanup.
- The player-enter rollback test preloads counters directly because the full Java `CM_LEVEL_READY` / map-region player spawn sequence is not ported.
- Revalidation and cleanup remain immediate in C#; Java zone work uses map regions plus queued `ZoneUpdateService` behavior in many movement/controller paths. Timing and batching parity are not verified.
- The new tests do not validate live encrypted socket ordering or live Java side-by-side runtime behavior.

---

## Next Unit Of Work

Recommended next unit: add a dedicated kisk save-failure rollback regression for `GameServerConnection.CompleteToyPetSpawnUseItemAsync`, then continue the broader audit of generic `World.TryRemoveObject` callers that can remove creature-like objects outside `WorldNpcSpawnService`.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
   - `game-server/src/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/PvPZoneInstance.java`
2. Re-read C#:
   - `GameServerConnection.CompleteToyPetSpawnUseItemAsync`
   - `PlayerEnterWorldService.SaveItemUseSourceMutationAsync`
   - `CreaturePvpZoneRevalidationService`
   - `CreaturePvpZoneCounterService`
   - existing kisk tests in `GameServerConnectionFlightZoneFanoutTests`
3. Keep the next unit narrow:
   - inject or construct a failing persistence fixture for source-item mutation
   - let the kisk add to the world and enter PVP/FORT counters first
   - force source mutation failure
   - assert world removal, ID release if observable, no runtime kisk registration, source item unchanged, and empty counters

Strong alternatives:

1. Continue generic direct-removal auditing with `rg "TryRemoveObject" dotnetConversion/src/Aion.GameServer` and add targeted cleanup where removed objects can be `WorldNpc`, `PostmanNpc`, or future creature-like objects.
2. Add a compact formation route-walker or formation variant PVP/SIEGE regression now that shared hooks are wired.
3. Wire remaining teleport/map-change completion paths outside kisk revive.
4. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~RiftServiceTests|FullyQualifiedName~RiftManagerServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 492-493, `docs/Phase-6BX-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
