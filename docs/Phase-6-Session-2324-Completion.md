# Phase 6 Session 2324 Completion - Portal Difficulty Metadata

## Scope

Implemented difficulty id propagation for fresh portal instance allocation metadata, starting with Beshmundir's accepted difficulty response.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `BeshmundirsWalkAI.acceptRequest` calls `moveToInstance(responder, (byte) 2)` for question id `902050`.
- `BeshmundirsWalkAI.moveToInstance` passes the difficulty byte to `PortalService.port(...)`.
- `PortalService.port(...)` passes difficulty into `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)`.
- `InstanceService.getNextAvailableInstance(...)` passes `difficultyId` into `SpawnEngine.spawnInstance(...)` and logs it at instance creation.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingKiskBindRequest.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Implemented:

- `PendingBeshmundirDifficultyEnterRequest` now carries `DifficultyId`.
- Beshmundir difficulty selection records Java's accepted-response difficulty `2`.
- Accepted Beshmundir response passes the stored difficulty into the portal-use movement helper.
- Fresh group allocation copies nonzero difficulty onto `PortalTeamEntryPlan`, `GroupPortalAllocationPlan`, and `WorldMapInstanceRuntimeState`.
- Runtime world-map allocation helpers accept optional difficulty without changing existing default `0` callers.

Known limitations:

- C# still does not spawn instance NPCs through Java-equivalent `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- Registered-instance transfers do not mutate existing instance difficulty metadata, matching Java where difficulty matters at creation time.
- Java alternate `acceptRequest` difficulty `1` branch remains unreachable from the currently registered Beshmundir question id.

## Validation Decision

- Changed surface: production Beshmundir question-response boundary plus shared world runtime allocation metadata.
- Specific behavior/contract: Java difficulty `2` from accepted Beshmundir question id is preserved through fresh group allocation metadata and runtime instance state.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldMapRuntimeStateTable_AllocatesNextInstanceIdsLikeJavaWorldMap|FullyQualifiedName~WorldMapRuntimeStateTable_TracksInstanceRegistrationAndCapacitySlice" --no-restore
```

Result: initial run failed at compile because `Assert.Equal(2, byte?)` selected an incompatible overload. The assertion was corrected to use `(byte)2`.

- Final focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldMapRuntimeStateTable_AllocatesNextInstanceIdsLikeJavaWorldMap|FullyQualifiedName~WorldMapRuntimeStateTable_TracksInstanceRegistrationAndCapacitySlice|FullyQualifiedName~InstanceRuntimeService_CreatesAndReusesRegisteredInstances" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime handler/allocation branch; Java source review was the practical source-of-truth evidence.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: shared runtime allocation metadata was extended, but focused tests covered the changed allocation helpers, direct group allocation, and Beshmundir boundary. No world-state concurrency behavior, packet primitive, serialization primitive, persistence, or scheduler code changed.
- Broad .NET decision: skipped full project/solution validation after the final focused command passed and supplied the compile signal for the affected project.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.acceptRequest` / `moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` / `HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted question id `902050` now carries Java difficulty `2` into portal-use allocation. Alternate difficulty `1` remains unreachable in current Java branch. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Fresh group allocation now passes `PortalTeamEntryPlan.DifficultyId` into runtime instance allocation. Member solo-instance scan and alliance/league paths remain unported. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.Services.InstanceRuntimeService` / `Aion.GameServer.World.WorldMapRuntimeState*` | Runtime State Allocation | Partial | Focused Unit Tested | Partial Parity | Runtime instances now preserve difficulty metadata. C# still lacks Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` side effects and instance-handler lifecycle. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.acceptRequest`, `moveToInstance`, and `PortalService.port` | Accepted Beshmundir response allocates a group instance whose runtime difficulty id is `2`. | Focused C# runtime packet execution plus Java source review. | Does not cover Java spawn filtering or real-client encrypted bytes. |
| `QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers` | Boundary Runtime | Java source review of `PortalService.port` and `InstanceService.getNextAvailableInstance` | Generic fresh group allocation stores nonzero difficulty on allocated runtime instance and allocation plan metadata. | Focused C# runtime test plus Java source review. | Does not cover spawn engine or alliance/league paths. |
| `WorldMapRuntimeStateTable_AllocatesNextInstanceIdsLikeJavaWorldMap` / `WorldMapRuntimeStateTable_TracksInstanceRegistrationAndCapacitySlice` / `InstanceRuntimeService_CreatesAndReusesRegisteredInstances` | Unit | Java source review of `WorldMapInstanceFactory` and `WorldMapInstance` storage behavior | Runtime allocation helpers preserve optional difficulty while retaining existing id, owner, capacity, registration, and reuse behavior. | Focused C# unit tests plus Java source review. | Does not cover Java instance handler lifecycle or auto-destroy task scheduling. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Java group member solo-instance scan for the `!instanceGroupReq` path remains unported.
- Alliance/league fresh allocation remains unsupported.
- Java range observer auto-deny behavior for AI requests remains unported.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2324] Carry portal difficulty into allocation
```

## Next Recommended UOW

Implement Java group member solo-instance scan for grouped portals when `instanceGroupReq` is false, or begin the spawn-engine difficulty filtering slice if the next session wants to consume the newly preserved difficulty metadata.
