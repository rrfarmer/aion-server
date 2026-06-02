# Phase 6 Session 2331 Handoff - Seed Instance Static Door States

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2331-Completion.md`
- `docs/Phase-6-Session-2331-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2331, C# instance spawning now loads Java `staticdoor_templates` and records per-instance static door open/closed state before NPC spawn materialization.

Relevant completed portal/instance/spawn slices:

- Fresh group, alliance, league, and player-object portal continuation can allocate the next runtime instance, register the relevant team/player id, transfer the current player, apply cooldown, and preserve nonzero difficulty metadata.
- `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` filters nonzero spawn difficulty ids by runtime instance difficulty and places spawned NPCs into the runtime instance id.
- Fresh portal allocation calls `SpawnWorldNpcsForInstance(...)`.
- C# now loads `StaticData.StaticDoors` from `staticdoor_templates`.
- `SpawnWorldNpcsForInstance(...)` seeds static door state with the Java `OPENED` bit into `IStaticPlaceableStateService.SetDoorState(worldId, instanceId, staticId, isOpen)`.

Still not proven or not implemented:

- Java `StaticDoor` world object materialization and known-list visibility.
- `StaticDoorService.openStaticDoor(...)`, key checks, state mutation, and `SM_EMOTION` broadcasts.
- Java static object spawns for spawn groups with `handler="STATIC"`.
- Java housing spawning, event spawns, walker organization, temporary spawn scheduling nuances, non-NPC object side effects, and full object materialization.
- Java `InstanceHandler.onInstanceCreate()` and empty-instance checker behavior.
- Alliance/league live fanout.
- Real-client/encrypted socket bytes for portal branches.

## Commits Made

- `b8c7c528b [Phase 6][UOW-2329] Add spawn instance difficulty bridge`
- `4f2bf3495 [Phase 6][UOW-2330] Spawn fresh portal instances`
- `[Phase 6][UOW-2331] Seed instance static door states`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticDoorTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/StaticPlaceableStateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2331-Completion.md`
- `docs/Phase-6-Session-2331-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.StaticDoorData` | `Aion.GameServer.Dataholders.StaticDoorTable` / `StaticDoorSummary` | Dataholder | Partial | Unit Tested Through Loader | Partial Parity | C# now loads static door world/id/key/position/state fields and indexes by world/id. Duplicate-world and duplicate-door exception behavior is not yet mirrored. |
| `com.aionemu.gameserver.model.templates.staticdoor.StaticDoorTemplate` | `Aion.GameServer.Dataholders.StaticDoorSummary` | DTO | Partial | Unit Tested Through Loader | Partial Parity | Core template fields are projected. Java inheritance from `VisibleObjectTemplate` and template id/name/l10n behavior are represented only by constants and summary fields. |
| `com.aionemu.gameserver.model.templates.staticdoor.StaticDoorState` | `StaticDoorSummary.IsOpen` | Enum/Flag | Partial | Unit Tested | Partial Parity | C# uses Java `OPENED` bit `1 << 0` to derive open state. CLICKABLE/CLOSEABLE/ONEWAY interactions are not yet modeled. |
| `com.aionemu.gameserver.spawnengine.StaticDoorSpawnManager.spawnTemplate` | `WorldNpcSpawnService.SpawnStaticDoorsForInstance` plus `IStaticPlaceableStateService.SetDoorState` | Service Boundary | Partial | Unit Tested | Partial Parity | C# now seeds per-instance static door open/closed state before NPC spawning. It does not create `StaticDoor` world objects, known-list entries, packets, key checks, or full geo collision behavior. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` normal spawn side effects | `GameServerConnection.QueuePortalTeamContinueTransferAsync` -> `WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh portal allocation passes static door data into the spawn bridge. Other Java spawn side effects remain partial. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `StaticDoorSpawnManager.spawnTemplate(instance)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: live side-effect enabling in the instance spawn path plus a static data projection.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered direct service behavior plus loader-to-allocation behavior. No packet primitive, serialization helper, persistence schema, hosted startup lifecycle, or shared scheduler primitive changed.

## Next Sequential UOW

Recommended next production scope: inspect and, if feasible, port Java `StaticObjectSpawnManager.spawnTemplate(spawn, instanceId)` behavior for spawn groups with `handler="STATIC"`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/StaticObjectSpawnManager.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/StaticObject.java`
- `game-server/src/com/aionemu/gameserver/model/templates/VisibleObjectTemplate.java`
- Java item/static object template lookup reached through `DataManager.ITEM_DATA.getItemTemplate(spawn.getNpcId())`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- Existing C# world object models under `dotnetConversion/src/Aion.GameServer/Model/GameObjects`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`

Specific behavior to prove: when Java spawn groups use `handler="STATIC"`, C# should either materialize equivalent static objects in the target instance or record the closest available static-object side effect with explicit gaps documented.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests" --no-restore
```

Narrow after Work Discovery to the exact new static-object test name plus the closest adjacent `WorldNpcSpawnServiceTests` instance-spawn test. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none expected for discovery or a narrow static-object planner/state test. If the next unit creates new live world object types, changes shared world insertion, or alters packet visibility, document that trigger before broad validation.

## Safe Candidates

- Java `StaticObjectSpawnManager.spawnTemplate(...)` branch for `handler="STATIC"`.
- Java `InstanceHandler.onInstanceCreate()` bridge/planner if C# instance-handler infrastructure is already present.
- Event-specific instance spawn branch for Java `spawnEventSpawns(...)` if matching C# event/spawn data exists.
- League leave/disband snapshot clearing for Java `PlayerAlliance.setLeague(null)` lifecycle branches.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
