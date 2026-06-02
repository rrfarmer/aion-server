# Phase 6 Session 2333 Handoff - Spawn Solo Fresh Portal Instances

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2333-Completion.md`
- `docs/Phase-6-Session-2333-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2333, C# solo fresh portal allocation now invokes instance spawning before transfer when static data and `WorldNpcSpawnService` are available.

Relevant completed portal/instance/spawn slices:

- Fresh group portal allocation spawns instance NPCs, static doors, and static handler objects.
- Fresh solo portal allocation now spawns instance NPCs/static objects through the same `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)` bridge.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static door templates load from `staticdoor_templates`, and instance spawning seeds per-instance static door open/closed state.
- Static handler spawn groups resolve item templates and create `WorldStaticObject` entries.

Still not proven or not implemented:

- Higher-level solo portal difficulty propagation for Beshmundir-style difficulty selection.
- Java `InstanceHandler.onInstanceCreate()` and `onInstanceDestroy()` lifecycle callbacks.
- Empty-instance checker behavior.
- Event-specific instance spawns and custom instance handler suppliers.
- Static object known-list visibility, packet fanout, and controller/interaction behavior.
- Alliance/league live fanout and real-client/encrypted socket bytes for portal branches.

## Commits Made

- `863be0be5 [Phase 6][UOW-2332] Materialize static handler objects`
- `[Phase 6][UOW-2333] Spawn solo fresh portal instances`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2333-Completion.md`
- `docs/Phase-6-Session-2333-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` solo fresh allocation branch | `GameServerConnection.QueueAllocatedInstancePortalTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | C# now spawns instance NPCs before solo fresh transfer when static data/spawn service are available. Higher-level solo difficulty propagation and real-client branch coverage remain pending. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` spawn-before-transfer behavior | `InstanceRuntimeService.CreatePortalTransferInstance` plus `GameServerConnection.QueueAllocatedInstancePortalTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | Allocation carries difficulty id and the connection invokes instance spawning before transfer. Java `onInstanceCreate()` and empty-instance checker are still missing. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService|FullyQualifiedName~WorldNpcSpawnServiceTests.SpawnWorldNpcsForInstance_FiltersByDifficultyAndUsesInstanceIdLikeJavaSpawnInstance" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for the `PortalService.port(...)` plus `InstanceService.getNextAvailableInstance(...)` path. Java source review was used as source-of-truth evidence.

Broad-validation trigger: live portal allocation side effect and world-state spawn side effect.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the edited connection branch plus adjacent spawn filtering behavior.

## Next Sequential UOW

Recommended next production scope: add the Java `InstanceHandler.onInstanceCreate()` lifecycle hook in a small, deferred C# form that preserves Java ordering.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService" --no-restore
```

Narrow after Work Discovery to the exact edited test and one adjacent allocation/spawn test. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the hook enables live dynamic handlers, scheduler behavior, or connection dispatch outside the focused allocation path.

## Safe Candidates

- Java `InstanceHandler.onInstanceCreate()` lifecycle hook, fired after C# instance spawning.
- Higher-level Beshmundir solo difficulty propagation into `PortalEntryPlanResult`.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.
- Static object known-list/visibility model if C# known-list infrastructure already supports non-NPC visible objects.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
