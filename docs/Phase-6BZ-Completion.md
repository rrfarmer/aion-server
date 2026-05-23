# Phase 6BZ Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BY and covers Sessions 494-495.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1043 tests.

---

## Recent Work Completed

- Added a dedicated kisk item-use save-failure rollback regression for the branch wired in Session 491.
- Made `EmptyPlayerEnterWorldRepository` configurable for `SaveItemUseSourceMutationAsync` success/failure, preserving default success behavior.
- Extended the connection test harness so `GameServerConnection.CompleteToyPetSpawnUseItemAsync` can run with a real `PlayerEnterWorldService` and a failing source-item mutation repository.
- Verified rollback after kisk world insertion for real Java PVP geometry in `PVP_87_210040000`.
- Verified rollback after kisk world insertion for real Java FORT/SIEGE geometry in `ABYSS_CASTLE_AREA_2011_210050000`.
- Both rollback regressions verify world removal, empty modeled counters, unchanged source item inventory, no runtime kisk registration, no NPC visibility refresh, and object ID release/reuse where directly asserted.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 495 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `9e2908ec5` - `Test kisk rollback pvp cleanup`
- `dffcb7b0c` - `Cover kisk rollback siege counters`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `ToyPetSpawnAction.act` | `GameServerConnection.CompleteToyPetSpawnUseItemAsync` | Partial | Regression Tested | Partial Parity | Kisk save-failure rollback now has dedicated coverage for modeled PVP and SIEGE counters. Java decreases inventory before spawn; C# currently adds world/runtime plan first and compensates on failed persistence, an intentional ordering difference tracked in progress notes. |
| `VisibleObjectSpawner.spawnKisk` | `PlayerKiskSpawnService.CreatePlan` + `GameServerConnection.RevalidateKiskCreaturePvpZones` | Partial | Integration + Regression Tested | Partial Parity | Spawn placement feeds real Java PVP and FORT geometry before rollback cleanup. Full `KiskController`, known-list, AI, dialog, team/legion ownership, and controller callbacks remain incomplete. |
| `KiskService.regKisk` | `PlayerKiskRegistry.RegisterKisk` | Partial | Regression Tested | Partial Parity | Failed source persistence does not register runtime kisk state. Successful registration is covered by earlier PVP spawn regression, but live team/legion behavior remains broad future work. |
| `World.spawn` / `World.despawn` | Direct `_world.TryAddObject` / rollback `_world.TryRemoveObject` in `CompleteToyPetSpawnUseItemAsync` | Partial | Regression Tested | Intentional Difference | C# directly revalidates after add and clears modeled counters after rollback because full Java map regions/zone instances are not ported. Java zone handlers and controller callbacks are not executed. |
| `FortressLocation` / FORT zone static data | `StaticData.CreaturePvpZones` + `CreaturePvpZoneType.Siege` | Partial | Regression Tested | Partial Parity | Real `ABYSS_CASTLE_AREA_2011_210050000` FORT geometry is exercised for kisk rollback cleanup. Fortress observers, ownership, shield, balance, and vulnerability behavior remain unported. |
| `IDFactory.releaseId` | `Aion.GameServer.Utils.IdFactory.IDFactory.ReleaseId` rollback path | Partial | Regression Tested | Partial Parity | PVP rollback regression confirms the kisk object ID is reusable after rollback. Startup preload and invalid-ID edge cases are not revalidated in this window. |
| `PvPZoneInstance.onEnter/onLeave` | `CreaturePvpZoneCounterService` through kisk rollback hooks | Partial | Unit + Regression Tested | Partial Parity | Tests prove rollback clears modeled PVP and SIEGE counters after the kisk entered real Java zone geometry. Java handler ordering and side effects remain unverified. |
| `InventoryDAO` / `Storage.decreaseByObjectId` persistence boundary | `EmptyPlayerEnterWorldRepository.SaveItemUseSourceMutationResult` test fixture + `PlayerEnterWorldService.SaveItemUseSourceMutationAsync` | Partial | Regression Tested | Needs Verification | The fixture isolates failed source persistence without a live database. Actual MySQL transaction/autocommit behavior and Java inventory mutation ordering were not side-by-side validated. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1043 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: Java/C# item-use persistence ordering difference, full fortress/siege side effects, Java zone handlers/controller callbacks, dedicated `KiskController` behavior, live DB failure simulation, Java queued scheduler/levels, generic direct-removal audit edges, and live socket ordering
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change zone revalidation, remaining direct-removal cleanup, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- The rollback tests validate C# compensation around the current C# persistence ordering. Java `ToyPetSpawnAction` decreases inventory before spawning the kisk; C# persists source mutation after world add and rolls back on failure.
- Rollback cleanup is tested with real PVP and FORT/SIEGE geometry, but it still clears modeled counters directly rather than executing Java zone `onLeave` handlers, controller callbacks, quest/material handlers, fortress observers, or instance callbacks.
- The FORT/SIEGE test validates only modeled counter cleanup. Full siege ownership, shields, buffs, vulnerability, and fortress handler behavior remain unported.
- Live database transaction/autocommit behavior, encrypted packet ordering, item-use observer cancellation ordering, and full kisk controller/dialog/death behavior remain unverified.
- The generic `World.TryRemoveObject` audit is not complete; remaining review should focus on direct service use of kisk lifetime removal and rollback branches in `WorldNpcSpawnService`.

---

## Next Unit Of Work

Recommended next unit: continue the generic direct world-removal audit by reviewing `WorldNpcSpawnService` rollback paths and `PlayerKiskLifetimeService.DespawnExpiredKisk` direct service usage for any remaining stale counter edge cases outside the already-covered cleanup boundary.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/PvPZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/spawnengine/InstanceWalkerFormations.java`
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Kisk.java`
   - `game-server/src/com/aionemu/gameserver/services/KiskService.java`
2. Re-read C#:
   - `WorldNpcSpawnService.RollBackSpawnedInactiveVariants`
   - `WorldNpcSpawnService.RollBackInactiveVariantSpawn`
   - `WorldNpcSpawnService.RollBackFormationSwap`
   - `PlayerKiskLifetimeService.DespawnExpiredKisk`
   - `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync`
   - `CreaturePvpZoneCounterService`
3. Keep the next unit narrow:
   - choose one direct removal edge where a creature can already have modeled counters
   - preserve existing success-path ordering
   - add a focused regression proving stale counters cannot survive rollback/removal
   - update `docs/PHASE-6-PROGRESS.md` with a parity table before committing

Strong alternatives:

1. Add a compact formation route-walker or formation variant PVP/SIEGE regression now that shared hooks are wired.
2. Wire remaining teleport/map-change completion paths outside kisk revive.
3. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
4. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~PlayerKiskRemovalRuntimeCleanupServiceTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~WorldNpcWalkerPlacementApplicationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 494-495, `docs/Phase-6BY-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
