# Phase 6 Session 2332 Handoff - Materialize Static Handler Objects

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2332-Completion.md`
- `docs/Phase-6-Session-2332-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2332, C# instance spawning now materializes Java `handler="STATIC"` spawn groups as `WorldStaticObject` entries when item templates are available.

Relevant completed portal/instance/spawn slices:

- Fresh portal allocation calls `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.
- Instance NPC spawns filter by nonzero difficulty id and use the allocated runtime instance id.
- Static door templates load from `staticdoor_templates`, and instance spawning seeds per-instance static door open/closed state.
- Static handler spawn groups resolve `ItemTemplateTable.GetItemTemplate(spawn.NpcId)`, create `WorldStaticObject`, position it in the target instance, and preserve static id/placeable state.

Still not proven or not implemented:

- Static object known-list visibility and packet fanout.
- Static object controller/interaction behavior.
- Java `StaticDoor` world object materialization and `StaticDoorService.openStaticDoor(...)`.
- Java housing spawning, event spawns, walker organization, temporary spawn scheduling nuances, and full object materialization.
- Java `InstanceHandler.onInstanceCreate()` and empty-instance checker behavior.
- Alliance/league live fanout and real-client/encrypted socket bytes for portal branches.

## Commits Made

- `60317339b [Phase 6][UOW-2331] Seed instance static door states`
- `[Phase 6][UOW-2332] Materialize static handler objects`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/WorldStaticObject.cs`
- `dotnetConversion/src/Aion.GameServer/World/World.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `docs/Phase-6-Session-2332-Completion.md`
- `docs/Phase-6-Session-2332-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.spawnengine.StaticObjectSpawnManager.spawnTemplate` | `WorldNpcSpawnService` static handler branch | Service Boundary | Partial | Unit Tested | Partial Parity | C# now resolves item templates, materializes static objects, positions them in the target instance, and preserves static id/placeable state. Pool ordering, known lists, packet fanout, and controller interaction remain unverified or missing. |
| `com.aionemu.gameserver.model.gameobjects.StaticObject` | `Aion.GameServer.Model.GameObjects.WorldStaticObject` | Model | Partial | Unit Tested | Partial Parity | C# stores object id, template id, item template, position, static id, and spawn location. Java controller, object template inheritance, known-list, and visibility behavior remain missing. |
| `com.aionemu.gameserver.world.World` static object storage | `Aion.GameServer.World.World.GetStaticObjects` plus `TryAddObject` | World State | Partial | Unit Tested | Partial Parity | C# can store/query static objects separately from NPC scans. Region/known-list behavior remains missing. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_MaterializesStaticHandlerObjectsLikeJavaStaticObjectSpawnManager|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `StaticObjectSpawnManager.spawnTemplate(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: new live world object type and instance-spawn side effect.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed behavior and closest adjacent spawn contracts.

## Next Sequential UOW

Recommended next production scope: inspect Java instance creation callbacks or event-specific instance spawn behavior and choose the smaller implementable slice.

Candidate A: Java `InstanceHandler.onInstanceCreate()`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`

C# artifacts likely involved:

- Existing instance handler/dynamic handler infrastructure if present.
- `WorldMapInstanceRuntimeState` and portal allocation path.
- Focused tests under `Aion.GameServer.Tests`.

Candidate B: Java event-specific instance spawns.

Java artifacts:

- `SpawnEngine.spawnEventSpawns(...)`
- `EventService.getActiveEvents()`
- event template spawn data.

Risks:

- C# may not yet have enough event/instance-handler infrastructure; if so, document the blocker and pick the other candidate.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService" --no-restore
```

Narrow after Work Discovery to the exact edited test class and one adjacent spawn/allocation test. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

## Safe Candidates

- Java `InstanceHandler.onInstanceCreate()` bridge/planner.
- Event-specific instance spawn branch for Java `spawnEventSpawns(...)`.
- Static object known-list/visibility model if C# known-list infrastructure already supports non-NPC visible objects.
- League leave/disband snapshot clearing for Java `PlayerAlliance.setLeague(null)` lifecycle branches.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
