# Phase 6 Session 2327 Completion - Portal No-Group Bypass Allocation

## Scope

Implemented Java `PortalService.port(...)` group-sized no-group branch when `instanceGroupReq == false`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

Java behavior used:

- For `maxPlayers` `3` or `6`, Java enters the group-sized branch when a group exists or `instanceGroupReq == false`.
- When no group exists and `instanceGroupReq == false`, Java looks up a registered instance by `player.getObjectId()`.
- If no instance exists, Java allocates with `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)` and does not call `registerTeam`.
- Transfer then registers the player object id through the common transfer path.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- Added `PortalTeamEntryKind.PlayerObject` for Java's group-sized player-object registration path.
- Bypassed, ungrouped, group-sized portal validation now creates a player-object plan using `player.ObjectId`.
- Group-sized continuation now accepts player-object plans.
- Fresh player-object allocation creates the instance and transfers the player without registering a team id.
- Existing transfer registration registers the player object id, preserving Java's later lookup behavior.

Known limitations:

- Alliance/league portal allocation remains unsupported.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Real-client/encrypted socket bytes remain unverified.

## Validation Decision

- Changed surface: production portal validation, group-sized continuation, and focused tests.
- Specific behavior/contract: for maxPlayers `3` or `6`, no group present, and `instanceGroupReq == false`, C# allocates/transfers through the player object id path without team registration.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_GroupBypassWithoutGroupCreatesPlayerObjectPlanLikeJava|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassWithoutGroupUsesPlayerObjectRegistrationLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassWithoutGroupAllocatesPlayerObjectInstance|FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Result: passed 7, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime portal branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is isolated to portal planning/continuation and focused tests compiled the affected project/dependencies.
- Broad .NET decision: skipped full project/solution validation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group-sized no-group `!instanceGroupReq` branch | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now allocates/transfers group-sized instances for ungrouped bypass players without registering a team id. Alliance/league and spawn side effects remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Allocation is reused for player-object group-sized branch and preserves max-player capacity. Java spawn engine lifecycle remains unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.register` / `registerTeam` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.Register` / `RegisterTeamId` | Runtime State Registration | Partial | Focused Boundary Tested | Partial Parity | Player-object branch avoids team registration and relies on transfer-time player registration for lookup, matching reviewed Java branch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ValidatePortalEntryPlan_GroupBypassWithoutGroupUsesPlayerObjectRegistrationLikeJava` | Unit | Java source review of `PortalService.port` | Bypassed ungrouped maxPlayers 6 portal creates a player-object plan with no members and fresh allocation disposition. | Focused C# unit test plus Java source review. | Does not cover real-client packet bytes. |
| `PreparePortalEntry_GroupBypassWithoutGroupCreatesPlayerObjectPlanLikeJava` | Boundary Unit | Java source review of `PortalService.port` and previous threshold wiring | Live preparation reaches the player-object branch when membership bypass disables group requirement. | Focused C# boundary test plus Java source review. | Does not cover admin-enter-all side effects outside group requirement. |
| `QueuePortalContinueTransferAsync_GroupBypassWithoutGroupAllocatesPlayerObjectInstance` | Boundary Runtime | Java source review of `PortalService.port`, `InstanceService.getNextAvailableInstance`, and `WorldMapInstance.register` | Continuation allocates maxPlayers 6, does not register team id, registers player object id, queues teleport, and persists cooldown. | Focused C# boundary test plus Java source review. | Does not cover Java spawn engine side effects. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Alliance/league fresh allocation remains unsupported.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Other Java admin/membership portal requirement bypasses remain partial.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2327] Allocate no-group bypass portal instances
```

## Next Recommended UOW

Implement alliance/league fresh allocation for Java `PortalService.port(...)` default branch, or begin spawn-engine difficulty filtering using the already preserved `WorldMapInstanceRuntimeState.DifficultyId`.
