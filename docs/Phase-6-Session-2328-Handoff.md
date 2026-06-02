# Phase 6 Session 2328 Handoff - Portal Alliance And League Allocation

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2328-Completion.md`
- `docs/Phase-6-Session-2328-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2328, Java alliance/league `PortalService.port(...)` default-branch allocation.

Completed portal slices relevant to the next work:

- Fresh group portal continuation allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.
- Beshmundir accepted difficulty response carries Java difficulty `2` into fresh portal allocation metadata.
- Bypassed group portal validation scans member object ids for a solo registered instance before fresh group allocation.
- Bypassed group portal continuation transfers into the found member instance and does not register the team id.
- Live preparation derives `bypassGroupRequirement` from Java-shaped access/membership thresholds.
- Bypassed, ungrouped, group-sized portal entry allocates/transfers through the player object id path without team registration.
- Alliance-sized portal validation now uses alliance id, league id when present, or player object id when group requirement is bypassed and no alliance exists.
- Fresh alliance/league portal continuation allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves difficulty metadata.

Still not proven:

- Alliance/league live fanout.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- League leave/disband snapshot clearing for every Java lifecycle branch.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `c95ad6c8b [Phase 6][UOW-2319] Transfer registered group portals`
- `9d49df0a2 [Phase 6][UOW-2320] Move Beshmundir non-leader into open instance`
- `03fd0b3fa [Phase 6][UOW-2321] Move Beshmundir accepted difficulty response`
- `dd42ddc77 [Phase 6][UOW-2322] Allocate fresh group portal instances`
- `cc83107b0 [Phase 6][UOW-2323] Prove Beshmundir fresh group allocation`
- `f449bcee2 [Phase 6][UOW-2324] Carry portal difficulty into allocation`
- `db322f7cb [Phase 6][UOW-2325] Reuse group member solo portal instance`
- `888432ded [Phase 6][UOW-2326] Wire portal group bypass thresholds`
- `fb273cd01 [Phase 6][UOW-2327] Allocate no-group bypass portal instances`
- `[Phase 6][UOW-2328] Allocate alliance portal instances`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerAllianceSnapshot.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueInvitePlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLeagueInvitePlannerTests.cs`
- `docs/Phase-6-Session-2328-Completion.md`
- `docs/Phase-6-Session-2328-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` alliance default branch | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now plans and allocates alliance, league, and no-alliance bypass default-branch instances. Live fanout and spawn side effects remain partial. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.getLeague` / `setLeague` | `Aion.GameServer.Model.GameObjects.PlayerAllianceSnapshot.LeagueId` / `Aion.GameServer.Services.PlayerAllianceRuntime.SetLeagueId` | Model / Runtime Bridge | Partial | Unit Tested | Partial Parity | C# snapshots now preserve the league id needed by portal allocation. Full Java league lifecycle remains partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Allocation is reused for alliance/league default-branch instances and preserves max-player/difficulty metadata. Java spawn engine lifecycle remains unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.registerTeam` / `register` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.RegisterTeamId` / `Register` | Runtime State Registration | Partial | Focused Boundary Tested | Partial Parity | Fresh alliance/league allocation registers the team id; transfer registers the player object id. Player-object bypass avoids team registration. |

## Validation From Last UOW

Focused C# portal/allocation validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceMemberUsesAllianceObjectIdLikeJavaDefaultBranch|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceMemberInLeagueUsesLeagueObjectIdLikeJavaDefaultBranch|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_AllianceBypassWithoutAllianceUsesPlayerObjectIdLikeJavaDefaultBranch|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_AlliancePlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 4, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused C# league snapshot bridge validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLeagueInvitePlannerTests.CreatePendingRequestResponsePlan_AcceptCreatesLeagueWhenRequesterHasNoLeagueLikeJavaEvent" --no-restore
```

Result: passed 1, failed 0, skipped 0.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime portal branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation after focused tests passed and supplied the compile signal for the affected project/dependencies.

## Next Sequential UOW

Recommended next production scope: implement spawn-engine difficulty filtering using `WorldMapInstanceRuntimeState.DifficultyId`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
- Java spawn templates/static data classes used by `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- C# static spawn data holders/loaders if present
- Existing tests around portal allocation and world map runtime state

Specific behavior to prove: when a fresh instance is allocated with a nonzero difficulty id, C# should filter or record spawn activation according to Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` behavior rather than merely carrying metadata.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_AlliancePlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Add exact new spawn-engine test names after implementation. This recipe proves the existing allocation callers still preserve difficulty metadata before adding spawn-specific assertions.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected for a narrow spawn-filtering unit unless the implementation crosses shared static-data loader boundaries. If it does, write the trigger before any full project/solution validation.

## Safe Candidates

- Implement spawn-engine difficulty filtering using `WorldMapInstanceRuntimeState.DifficultyId`.
- Add targeted league leave/disband snapshot clearing for Java `PlayerAlliance.setLeague(null)` lifecycle branches.
- Continue documenting real-client/encrypted socket gaps only when tied to a concrete runtime slice.

Avoid:

- Full .NET project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
