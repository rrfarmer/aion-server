# Phase 6 Session 2322 Completion - Fresh Group Portal Allocation

## Scope

Implemented fresh group-instance allocation for the Java `PortalService.port(...)` group path when max players is `3` or `6`, the player has a group/team id, and no registered team instance exists yet.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

Java behavior used:

- Group paths with no registered group instance call `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)`.
- Java immediately calls `WorldMapInstance.registerTeam(group)`.
- Java then checks `instance.getPlayersInside().size() < maxPlayers` and uses the normal `transfer(...)` helper.
- `transfer(...)` sets the start position if missing, registers the player object id, teleports with `FADE_OUT_BEAM`, and applies entrance cooldown when not reentering.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- `QueuePortalTeamContinueTransferAsync` now receives `WorldMapRuntimeStateTable`.
- Fresh group plans allocate the next runtime instance, register the team id, then continue through the same transfer/cooldown path as registered team instances.
- Existing registered group transfer and registered group reentry behavior remains covered by focused adjacent tests.

Known limitations:

- Difficulty id is still not propagated into C# runtime allocation state.
- Java's optional member solo-instance scan for `!instanceGroupReq` remains unported.
- Alliance/league fresh allocation remains unsupported.
- Real-client/encrypted socket bytes for these branches remain unverified.

## Validation Decision

- Changed surface: production connection dispatch using shared world runtime state for group portal continuation.
- Specific behavior/contract: Java `PortalService.port(...)` fresh group branch allocates an instance, registers the group/team id, transfers the entering player, and applies cooldown.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Adjacent focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists" --no-restore
```

Result: passed 4, failed 0, skipped 0. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime portal branch; Java source review was the practical source-of-truth evidence.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: shared world runtime state is used, but the change did not alter world state internals; it only invokes existing `CreateNextWorldMapInstance` and `RegisterTeamId` from the portal continuation path.
- Broad .NET decision: skipped full project/solution validation after focused tests covered fresh group allocation, registered group transfer, registered group reentry, and the closest Beshmundir question-response caller.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Fresh group allocation for maxPlayers `3`/`6` now allocates a runtime instance, registers the team id, transfers the player, and applies cooldown. Difficulty id, optional member solo-instance scan, alliance/league allocation, and real-client bytes remain unverified. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(int, byte, int)` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` via `QueuePortalTeamContinueTransferAsync` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Existing map-local instance allocation is now used for group portal continuation. Spawn engine, instance handler lifecycle, auto-destroy, and difficulty-specific spawning remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.registerTeam` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.RegisterTeamId` | Runtime State | Partial | Focused Boundary Tested | Partial Parity | Fresh group portal allocation now calls the existing team registration primitive and verifies team/player registration. Full Java team object storage is not represented. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers` | Boundary Runtime | Java source review of `PortalService.port`, `InstanceService`, and `WorldMapInstance.registerTeam` | Fresh group portal plan allocates instance id `2`, registers team id `88001`, registers player `1001`, queues teleport, and applies cooldown. | Focused C# runtime test plus Java source review. | Does not cover difficulty-specific spawn data, member solo-instance scan, alliance/league allocation, or real-client encrypted bytes. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Beshmundir leader difficulty acceptance with no registered instance now has the generic allocation primitive available, but the end-to-end accepted-response path has not yet been tested against a no-registered-instance setup.
- Difficulty id propagation into fresh allocation remains unported.
- Java group member solo-instance scan for the `!instanceGroupReq` path remains unported.
- Alliance/league fresh allocation remains unsupported.
- Java range observer auto-deny behavior for AI requests remains unported.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2322] Allocate fresh group portal instances
```

## Next Recommended UOW

Use the new fresh group allocation path to implement and prove Beshmundir leader difficulty acceptance when no registered group instance exists. Keep the scope limited to the accepted question response creating the group instance through the generic portal-use path.
