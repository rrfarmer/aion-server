# Phase 6 Session 2331 Completion - Seed Instance Static Door States

## Scope

Added the minimal C# data and runtime state needed to mirror Java `StaticDoorSpawnManager.spawnTemplate(instance)` door-state seeding during normal instance spawning.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/StaticDoorSpawnManager.java`
- `game-server/src/com/aionemu/gameserver/dataholders/StaticDoorData.java`
- `game-server/src/com/aionemu/gameserver/model/templates/staticdoor/StaticDoorTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/staticdoor/StaticDoorState.java`

Java behavior used:

- `SpawnEngine.spawnInstance(...)` calls `StaticDoorSpawnManager.spawnTemplate(instance)` before iterating spawn groups when no event template is active.
- `StaticDoorSpawnManager.spawnTemplate(instance)` reads `DataManager.STATICDOOR_DATA.getStaticDoors(instance.getMapId())`.
- Each static door is initialized from its template state, then `GeoService.setDoorState(mapId, instanceId, staticId, staticDoor.isOpen())` is called.
- Java `StaticDoorState.OPENED` is bit `1 << 0`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated; it remains a historical archive.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticDoorTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/StaticPlaceableStateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSpawnService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSpawnServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- Added `StaticDoorTable` and `StaticDoorSummary` projection for Java `staticdoor_templates`.
- `StaticData.LoadFromCacheAsync(...)` now loads `<staticdoor_templates><world world="..."><staticdoor ... /></world></staticdoor_templates>`.
- Extended `IStaticPlaceableStateService` with per-world/per-instance/per-door `SetDoorState(...)` and `GetDoorState(...)`.
- `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` now seeds static door state from `StaticDoorTable` before materializing NPC spawns.
- Fresh portal allocation now passes `staticData.StaticDoors` into the instance spawn bridge.

Known limitations:

- C# still does not materialize Java `StaticDoor` world objects or known-list entries.
- Static door key/open interaction via `StaticDoorService.openStaticDoor(...)` remains deferred.
- This only mirrors Java `GeoService.setDoorState(...)` door-state seeding.
- Event-template instance spawns still skip static-door seeding, as Java only calls `StaticDoorSpawnManager.spawnTemplate(instance)` when `eventTemplate == null`.

## Validation Decision

- Changed surface: production static data projection plus live instance-spawn side effect.
- Specific behavior/contract: C# instance spawning loads Java-shaped static door templates and records the Java `GeoService.setDoorState(mapId, instanceId, staticId, isOpen)` equivalent for the allocated instance.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `StaticDoorSpawnManager.spawnTemplate(instance)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: live side-effect enabling in the instance spawn path plus a static data projection.
- Broad .NET decision: skipped full project/solution validation because the effect is isolated to `StaticData`, `StaticPlaceableStateService`, `WorldNpcSpawnService`, and the fresh allocation call. The focused command compiled the affected project and directly covered loader-to-allocation and direct service behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.StaticDoorData` | `Aion.GameServer.Dataholders.StaticDoorTable` / `StaticDoorSummary` | Dataholder | Partial | Unit Tested Through Loader | Partial Parity | C# now loads static door world/id/key/position/state fields and indexes by world/id. Duplicate-world and duplicate-door exception behavior is not yet mirrored. |
| `com.aionemu.gameserver.model.templates.staticdoor.StaticDoorTemplate` | `Aion.GameServer.Dataholders.StaticDoorSummary` | DTO | Partial | Unit Tested Through Loader | Partial Parity | Core template fields are projected. Java inheritance from `VisibleObjectTemplate` and template id/name/l10n behavior are represented only by constants and summary fields. |
| `com.aionemu.gameserver.model.templates.staticdoor.StaticDoorState` | `StaticDoorSummary.IsOpen` | Enum/Flag | Partial | Unit Tested | Partial Parity | C# uses Java `OPENED` bit `1 << 0` to derive open state. CLICKABLE/CLOSEABLE/ONEWAY interactions are not yet modeled. |
| `com.aionemu.gameserver.spawnengine.StaticDoorSpawnManager.spawnTemplate` | `WorldNpcSpawnService.SpawnStaticDoorsForInstance` plus `IStaticPlaceableStateService.SetDoorState` | Service Boundary | Partial | Unit Tested | Partial Parity | C# now seeds per-instance static door open/closed state before NPC spawning. It does not create `StaticDoor` world objects, known-list entries, packets, key checks, or full geo collision behavior. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` normal spawn side effects | `GameServerConnection.QueuePortalTeamContinueTransferAsync` -> `WorldNpcSpawnService.SpawnWorldNpcsForInstance` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh portal allocation passes static door data into the spawn bridge. Other Java spawn side effects remain partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager` | Unit | Java source review of `StaticDoorSpawnManager.spawnTemplate` and `StaticDoorState` | Instance spawning records open/closed state for static doors in the target map/instance and ignores other maps. | Focused C# unit test plus Java source review. | Does not materialize static door objects or validate key/open interaction. |
| `QueuePortalContinueTransferAsync_FreshGroupAllocationSpawnsDifficultyFilteredNpcsLikeJavaInstanceService` | Unit | Java source review of `InstanceService`, `SpawnEngine`, and `StaticDoorSpawnManager` | Fresh portal allocation loads `staticdoor_templates`, seeds door state for allocated instance id `2`, and keeps NPC difficulty filtering intact. | Focused C# allocation test plus Java source review. | Does not cover real socket bytes or static-door object visibility. |
| `SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance` | Unit | Java source review of `SpawnEngine.spawnInstance` | Existing instance NPC difficulty filtering and instance-id placement still pass. | Focused adjacent regression test. | Does not inspect static doors. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported/extended in this UOW: 5
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Java `StaticDoor` world object materialization and known-list visibility remain unported.
- `StaticDoorService.openStaticDoor(...)` key checks, open/close state mutation, and `SM_EMOTION` broadcast remain deferred.
- Java `SpawnEngine.spawnInstance(...)` event spawns, housing spawns, walker organization, temporary scheduling nuances, non-NPC static objects, `InstanceHandler.onInstanceCreate()`, and empty-instance checker behavior remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2331] Seed instance static door states
```

## Next Recommended UOW

Continue from Java `SpawnEngine.spawnInstance(...)` side effects. The next safest production slice is likely Java `StaticObjectSpawnManager.spawnTemplate(spawn, instanceId)` for spawn groups with `handler="STATIC"`, after confirming C# item/static-object data and world insertion support.
