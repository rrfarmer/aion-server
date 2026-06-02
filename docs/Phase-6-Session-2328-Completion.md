# Phase 6 Session 2328 Completion - Portal Alliance And League Allocation

## Scope

Implemented the Java `PortalService.port(...)` default branch for alliance-sized portal allocation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAlliance.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- For `maxPlayers` greater than `6`, Java reads `player.getPlayerAlliance()`.
- If an alliance exists and `alliance.getLeague()` is non-null, Java uses the league object id for registered-instance lookup and fresh team registration.
- If an alliance exists without a league, Java uses the alliance object id.
- If no alliance exists and `instanceGroupReq == false`, Java uses `player.getObjectId()` and allocates without `registerTeam`.
- Fresh alliance or league allocation calls `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)` and then `instance.registerTeam(team)`.
- Transfer registers the player object id through the common transfer path.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerAllianceSnapshot.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueInvitePlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLeagueInvitePlannerTests.cs`

Implemented:

- Added `PlayerAllianceSnapshot.LeagueId` as the narrow C# bridge for Java `PlayerAlliance.getLeague()`.
- Added `PlayerAllianceRuntime.SetLeagueId(...)` and made league invite acceptance update alliance snapshots when a league is created or joined.
- Alliance-sized portal validation now creates:
  - `Alliance` plans keyed by alliance object id.
  - `League` plans keyed by league object id when present.
  - `PlayerObject` plans when group requirement is bypassed and no alliance exists.
- Team continuation now accepts `Alliance` and `League` portal plans.
- Fresh alliance/league allocation registers the team id before transfer.
- Player-object default-branch bypass allocation still does not register a team id.

Known limitations:

- Alliance/league live fanout remains unported; current continuation transfers the requesting player and records the team instance.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Real-client/encrypted socket bytes remain unverified.

## Validation Decision

- Changed surface: production alliance/league snapshot metadata, portal validation, team-sized continuation, and focused tests.
- Specific behavior/contract: Java `PortalService.port(...)` default branch uses alliance id, league id when present, or player object id when the group requirement is bypassed and no alliance exists; fresh alliance/league allocation registers the team id and transfer registers the player object id.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceMemberUsesAllianceObjectIdLikeJavaDefaultBranch|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceMemberInLeagueUsesLeagueObjectIdLikeJavaDefaultBranch|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceBypassWithoutAllianceUsesPlayerObjectIdLikeJavaDefaultBranch|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_AlliancePlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 4, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused C# bridge command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLeagueInvitePlannerTests.CreatePendingRequestResponsePlan_AcceptCreatesLeagueWhenRequesterHasNoLeagueLikeJavaEvent" --no-restore
```

Result: passed 1, failed 0, skipped 0.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime portal branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is isolated to portal planning/continuation and alliance/league snapshot metadata; focused tests supplied the compile signal for the affected project/dependencies.
- Broad .NET decision: skipped full project/solution validation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` alliance default branch | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now plans and allocates alliance, league, and no-alliance bypass default-branch instances. Live fanout and spawn side effects remain partial. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.getLeague` / `setLeague` | `Aion.GameServer.Model.GameObjects.PlayerAllianceSnapshot.LeagueId` / `Aion.GameServer.Services.PlayerAllianceRuntime.SetLeagueId` | Model / Runtime Bridge | Partial | Unit Tested | Partial Parity | C# snapshots now preserve the league id needed by portal allocation. Full Java league lifecycle remains partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Allocation is reused for alliance/league default-branch instances and preserves max-player/difficulty metadata. Java spawn engine lifecycle remains unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.registerTeam` / `register` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.RegisterTeamId` / `Register` | Runtime State Registration | Partial | Focused Boundary Tested | Partial Parity | Fresh alliance/league allocation registers the team id; transfer registers the player object id. Player-object bypass avoids team registration. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ValidatePortalEntryPlan_AllianceMemberUsesAllianceObjectIdLikeJavaDefaultBranch` | Unit | Java source review of `PortalService.port` | Alliance portal plan uses alliance object id and member snapshot metadata. | Focused C# unit test plus Java source review. | Does not cover real-client packet bytes. |
| `ValidatePortalEntryPlan_AllianceMemberInLeagueUsesLeagueObjectIdLikeJavaDefaultBranch` | Unit | Java source review of `PortalService.port` and `PlayerAlliance.getLeague` | League portal plan uses league object id and resolves a registered team instance. | Focused C# unit test plus Java source review. | Does not cover full Java league lifecycle. |
| `ValidatePortalEntryPlan_AllianceBypassWithoutAllianceUsesPlayerObjectIdLikeJavaDefaultBranch` | Unit | Java source review of `PortalService.port` | Bypassed no-alliance default branch uses player object id and no members. | Focused C# unit test plus Java source review. | Does not cover admin-enter-all side effects outside group requirement. |
| `QueuePortalContinueTransferAsync_AlliancePlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers` | Boundary Runtime | Java source review of `PortalService.port`, `InstanceService.getNextAvailableInstance`, and `WorldMapInstance.registerTeam/register` | Fresh alliance allocation registers team id, registers player id on transfer, preserves difficulty, sends teleport/cooldown. | Focused C# boundary test plus Java source review. | Does not cover live fanout or Java spawn engine side effects. |
| `CreatePendingRequestResponsePlan_AcceptCreatesLeagueWhenRequesterHasNoLeagueLikeJavaEvent` | Unit | Java source review of league invite accept flow | League creation/join updates alliance snapshots with the league id used by portal allocation. | Focused C# unit test plus Java source review. | Does not cover every league leave/disband branch. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported/extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Alliance/league live fanout remains unsupported.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- League leave/disband snapshot clearing needs a targeted production UOW before claiming full league lifecycle parity.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2328] Allocate alliance portal instances
```

## Next Recommended UOW

Implement spawn-engine difficulty filtering using the already preserved `WorldMapInstanceRuntimeState.DifficultyId`, or add a narrow league leave/disband snapshot cleanup UOW if portal alliance/league lifecycle metadata needs to be hardened before spawn work.
