# Phase 6BR Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BQ and covers Sessions 447-457.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 974 tests.

---

## Recent Work Completed

- Added `WorldMapRuntimeStateTable` and wired it into `GameServerRuntimeContext` as the map-id runtime owner for Java `WorldMap.worldOptions`.
- Wired live runtime-world-option consumers for map-default flight revalidation, ride-start restriction, and kisk/bind-stone placement restriction.
- Added Java `<toypetspawn>` item metadata parsing and the first delayed `ToyPetSpawnAction.act` kisk slice: 10s item-use scheduling, source item consume/delete persistence, lightweight `WorldNpc` kisk spawn, owner registry registration, and NPC visibility refresh.
- Added runtime kisk stats/state from Java NPC `<kisk_stats>` plus `SM_KISK_UPDATE`.
- Added first kisk bind behavior: owner auto-bind for solo kisks, owner bindstone question for multi-member kisks, `CM_QUESTION_RESPONSE` acceptance, bind-point packet, bind messages, and bind animation broadcast.
- Added kisk lifetime/despawn cleanup: scheduled 7200s removal, registry/world cleanup, object-id release, online bound/pending state cleanup, and known-list refresh for `SM_DELETE` deltas.
- Added previous-kisk member removal on rebind.
- Added cautious `Kisk.canBind` authorization for duplicate/full checks, use masks `0` unrestricted, `1` race, `2` legion, `3` solo, and owner-only allowances for `4` group / `5` alliance until team membership lookup exists.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 457 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `94638c004` - `Add world map runtime state registry`
- `000332d85` - `Use runtime world flags for flight revalidation`
- `53ee0083a` - `Use runtime world flags for ride restriction`
- `9f01a324c` - `Use runtime world flags for kisk restrictions`
- `4794c4e5b` - `Add runtime kisk ownership registry`
- `905ca45a2` - `Add lightweight kisk spawn action`
- `62d0935fd` - `Add kisk runtime stats and update packet`
- `81212f6d8` - `Add kisk bind request flow`
- `9c720efc1` - `Schedule kisk lifetime despawn`
- `10e7dfc1b` - `Remove previous kisk binding on rebind`
- `498ad88f0` - `Add kisk bind authorization checks`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `world.WorldMap.worldOptions` / `World.getWorldMap(int)` | `WorldMapRuntimeStateTable` / `GameServerRuntimeContext.WorldMapStates` | Partial | Unit + Integration Tested | Partial Parity | Runtime option lookup and set/remove mutation exist by map id. Java world-map instances and object iteration remain absent. |
| `WorldMap.isFlightAllowed` | `PlayerZoneStateService` via runtime map state | Partial | Unit + Regression Tested | Partial Parity | Map-default flight now reads mutable runtime flags. Polygon `FLY` / `NO_FLY` membership remains separate and zone-type driven. |
| `RideAction.canAct` / `WorldMap.canRide` | `PlayerRideRestrictionService` / `HandleRideUseItemAsync` | Partial | Unit + Packet Tested | Partial Parity | Ride start checks runtime/static map `RIDE` flags and Java config gate. Per-zone ride membership and enter-zone dismount are still pending. |
| `ToyPetSpawnAction.canAct` | `PlayerKiskSpawnRestrictionService` / `HandleToyPetSpawnUseItemAsync` | Partial | Unit + Packet Tested | Partial Parity | Flying, instance map, already-installed kisk, and map `BIND` restrictions are implemented. Full per-zone bind membership is pending. |
| `ToyPetSpawnAction.act` / `VisibleObjectSpawner.spawnKisk` | `PlayerKiskSpawnService` / `CompleteToyPetSpawnUseItemAsync` | Partial | Unit + Regression Tested | Partial Parity | Delayed use, source consume/delete, lightweight `WorldNpc` spawn, heading offset, registry registration, and visibility refresh are ported. Dedicated Java `Kisk` object/controller remains absent. |
| `KiskStatsTemplate` / `Kisk` runtime counters | `KiskStatsSummary` / `PlayerKiskRuntimeState` | Partial | Unit + Integration Tested | Partial Parity | Use mask, max/current members, resurrections, owner race/legion, and lifetime state are modeled. HP/death/controller state remains absent. |
| `SM_KISK_UPDATE` | `SmKiskUpdate` | Ported | Packet Tested | Verified Parity | Opcode `144` and the eight Java integer fields are covered by deterministic packet tests. Fanout remains partial. |
| `KiskService.onBind` / `KiskAI` owner path | `PlayerKiskBindService`, `PendingKiskBindRequest`, `GameServerConnection` bind helpers | Partial | Unit + Packet Tested | Partial Parity | Owner post-spawn bind/dialog is implemented, including old-kisk removal when resolvable. Non-owner dialog start is still absent. |
| `Kisk.KiskLifeTask` / `KiskController.delete` | `PlayerKiskLifetimeService` + scheduled connection cleanup | Partial | Unit Tested | Partial Parity | Lifetime removal cleans registry/world/id state and refreshes visibility. Java controller/death/offline member side effects remain pending. |
| `Kisk.canBind` / `Kisk.isUseAllowed` | `PlayerKiskAuthorizationService` | Partial | Unit Tested | Partial Parity | Masks `0`-`3` are covered; masks `4`/`5` allow owner only until real group/alliance member lookup exists. |
| `Kisk.broadcastKiskUpdate` | No complete C# equivalent | Not Started | No Tests | Needs Verification | Member and same-race known-list fanout remain a recommended next slice. |
| `KiskAI.handleDialogStart` for non-owner players | No complete C# equivalent | Not Started | No Tests | Needs Verification | General NPC dialog interaction must route kisk objects through the authorization service. |

Metrics from the current handoff window:

- Total focused sessions covered: 11
- Total commits covered: 11
- Current full validation baseline: 974 tests passing
- Total artifacts with verified packet parity in this window: `SM_KISK_UPDATE` plus bind-related packet/message constants already captured in `PHASE-6-PROGRESS.md`
- Total blocked artifacts: group/alliance kisk use-mask member checks are blocked on richer team membership lookup
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; non-owner kisk interaction, kisk fanout/resurrection/controller state, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full socket-order harnesses, full zone lifecycle handlers, full movement-controller parity, full audit subsystem, full transform model, full stat-function/effect resolution, attack-speed extraction, DP cap extraction, group/alliance/GM state fanout, live HP/MP/FP max-resource lookup, full reward-loop orchestration, team distribution, PVP AP/XP reward branches, quest reward pipeline, full effect runtime, scheduled callbacks, AI handlers, team loot, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Kisk spawn is still a lightweight `WorldNpc` plus runtime state, not a dedicated Java `Kisk`/`SummonedObject` with controller, life stats, death state, AI, or attackability.
- Non-owner players cannot start kisk dialog yet. Owner bind happens only through the post-spawn path and pending bindstone question.
- `Kisk.canBind` group/alliance masks are owner-only in C# for now. Real member checks need group/alliance membership lookup by creator id.
- `Kisk.broadcastKiskUpdate` is not fully ported. C# sends key updates to the binding player but does not yet broadcast old/new updates to members and same-race known-list observers.
- Offline-bound member maps, login/logout kisk rebind, resurrection prompt options, bind-point reset packets, and kisk cooldown update fanout remain pending.
- Kisk lifetime scheduling is wired, but tests validate deterministic cleanup directly rather than waiting for a two-hour timer.
- Runtime world-option set/remove still has no live admin or script caller outside tests.
- Reflection is not used. New serialization is packet-tested where packet writers were added. Date/time is in-memory runtime lifetime only. Threading uses C# concurrent dictionaries and `ThreadPoolManager`, not Java's exact controller/task-id lifecycle.

---

## Next Unit Of Work

Recommended next unit: continue kisk lifecycle parity by adding non-owner `KiskAI.handleDialogStart` routing through NPC dialog interaction. Use `PlayerKiskAuthorizationService` for duplicate/full/no-authority decisions, and keep group/alliance member checks explicitly partial until team membership lookup can prove creator membership.

Strong alternatives:

1. Implement `Kisk.broadcastKiskUpdate` fanout for member and same-race known-list recipients.
2. Add resurrection-side kisk behavior from `PlayerReviveService`, `SM_DIE`, and `TeleportService.sendKiskBindPoint`.
3. Add admin zone-info output as the next runtime world-option consumer.
4. Deepen ride parity with `PlayerController.onEnterZone` dismount once enough zone-entry membership can be represented.

Suggested order:

1. Re-read:
   - `game-server/data/handlers/ai/KiskAI.java`
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Kisk.java`
   - `game-server/src/com/aionemu/gameserver/services/KiskService.java`
   - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
   - current C# `GameServerConnection`, `PlayerKiskAuthorizationService`, `PlayerKiskBindService`, `PlayerKiskRegistry`, `PlayerKiskLifetimeService`, `NpcDialog*` services, and kisk tests.
2. Pick one focused Java-parity unit.
3. Keep Java breadcrumbs in code for each behavior copied.
4. Update `PHASE-6-PROGRESS.md` with the next session, migration parity table, tests, risks, metrics, and next recommended unit.
5. Commit the unit before moving on.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskAuthorizationServiceTests|FullyQualifiedName~PlayerKiskBindServiceTests|FullyQualifiedName~PlayerKiskRegistryTests|FullyQualifiedName~PlayerKiskLifetimeServiceTests|FullyQualifiedName~PlayerKiskSpawnServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskAuthorizationServiceTests|FullyQualifiedName~PlayerKiskLifetimeServiceTests|FullyQualifiedName~PlayerKiskBindServiceTests|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~PlayerKiskRegistryTests|FullyQualifiedName~PlayerKiskSpawnRestrictionServiceTests|FullyQualifiedName~PlayerRideRestrictionServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts|FullyQualifiedName=Aion.GameServer.Tests.GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 447-457, `docs/Phase-6BQ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
