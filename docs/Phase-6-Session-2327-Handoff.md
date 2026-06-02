# Phase 6 Session 2327 Handoff - Portal No-Group Bypass Allocation

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2327-Completion.md`
- `docs/Phase-6-Session-2327-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2327, Java no-group `!instanceGroupReq` group-sized portal allocation.

Completed portal slices relevant to the next work:

- Fresh group portal continuation allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.
- Beshmundir accepted difficulty response carries Java difficulty `2` into fresh portal allocation metadata.
- Bypassed group portal validation scans member object ids for a solo registered instance before fresh group allocation.
- Bypassed group portal continuation transfers into the found member instance and does not register the team id.
- Live preparation derives `bypassGroupRequirement` from Java-shaped access/membership thresholds.
- Bypassed, ungrouped, group-sized portal entry now allocates/transfers through the player object id path without team registration.

Still not proven:

- Alliance/league portal allocation.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
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
- `[Phase 6][UOW-2327] Allocate no-group bypass portal instances`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2327-Completion.md`
- `docs/Phase-6-Session-2327-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group-sized no-group `!instanceGroupReq` branch | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now allocates/transfers group-sized instances for ungrouped bypass players without registering a team id. Alliance/league and spawn side effects remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Allocation is reused for player-object group-sized branch and preserves max-player capacity. Java spawn engine lifecycle remains unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.register` / `registerTeam` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.Register` / `RegisterTeamId` | Runtime State Registration | Partial | Focused Boundary Tested | Partial Parity | Player-object branch avoids team registration and relies on transfer-time player registration for lookup, matching reviewed Java branch. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_GroupBypassWithoutGroupCreatesPlayerObjectPlanLikeJava|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassWithoutGroupUsesPlayerObjectRegistrationLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassWithoutGroupAllocatesPlayerObjectInstance|FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Result: passed 7, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime portal branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation after focused tests passed and supplied the compile signal for the affected project.

## Next Sequential UOW

Recommended next production scope: implement alliance/league fresh allocation for Java `PortalService.port(...)` default branch.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAlliance.java`
- Java league/general team classes used by `PlayerAlliance.getLeague()`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Specific behavior to prove: default branch uses alliance object id, league object id when present, or player object id when group requirement is bypassed and no alliance exists; fresh allocation registers the team only when a team exists.

Focused C# command should start with:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassWithoutGroupAllocatesPlayerObjectInstance|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassWithoutGroupUsesPlayerObjectRegistrationLikeJava" --no-restore
```

Add exact new alliance/league test names after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected for a narrow portal planner/continuation unit. If live side effects expand beyond portal preparation/continuation, document the trigger before broad validation.

## Safe Candidates

- Implement alliance/league fresh allocation for portal default branch.
- Spawn-engine difficulty filtering using `WorldMapInstanceRuntimeState.DifficultyId`.
- Continue documenting real-client/encrypted socket gaps only when tied to a concrete runtime slice.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
