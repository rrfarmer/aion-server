# Phase 6 Session 2324 Handoff - Portal Difficulty Metadata

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2324-Completion.md`
- `docs/Phase-6-Session-2324-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2324, difficulty metadata propagation into fresh portal allocation.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir grouped non-leader open-instance `INSTANCE_ENTRY`: resolves `portal_use` path and transfers into the registered group instance.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Beshmundir accepted difficulty response with registered group instance: removes the pending request and transfers through the portal-use path.
- Beshmundir accepted difficulty response with no registered group instance: allocates the group instance through the generic portal-use path.
- Fresh group portal continuation: allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.

Still not proven:

- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- Java member solo-instance scan for grouped portals when default group requirement is disabled.
- Alliance/league portal allocation.
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
- `[Phase 6][UOW-2324] Carry portal difficulty into allocation`

## Files Changed In Last UOW

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
- `docs/Phase-6-Session-2324-Completion.md`
- `docs/Phase-6-Session-2324-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.acceptRequest` / `moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` / `HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted question id `902050` now carries Java difficulty `2` into portal-use allocation. Alternate difficulty `1` remains unreachable in current Java branch. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Fresh group allocation now passes `PortalTeamEntryPlan.DifficultyId` into runtime instance allocation. Member solo-instance scan and alliance/league paths remain unported. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` | `Aion.GameServer.Services.InstanceRuntimeService` / `Aion.GameServer.World.WorldMapRuntimeState*` | Runtime State Allocation | Partial | Focused Unit Tested | Partial Parity | Runtime instances now preserve difficulty metadata. C# still lacks Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` side effects and instance-handler lifecycle. |

## Validation From Last UOW

Initial focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldMapRuntimeStateTable_AllocatesNextInstanceIdsLikeJavaWorldMap|FullyQualifiedName~WorldMapRuntimeStateTable_TracksInstanceRegistrationAndCapacitySlice" --no-restore
```

Result: failed at compile because `Assert.Equal(2, byte?)` selected an incompatible overload. The assertion was corrected to use `(byte)2`.

Final focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~WorldMapRuntimeStateTable_AllocatesNextInstanceIdsLikeJavaWorldMap|FullyQualifiedName~WorldMapRuntimeStateTable_TracksInstanceRegistrationAndCapacitySlice|FullyQualifiedName~InstanceRuntimeService_CreatesAndReusesRegisteredInstances" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime handler/allocation branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: shared runtime allocation metadata was extended, but focused tests covered the changed allocation helpers, direct group allocation, and Beshmundir boundary.

Broad .NET decision: skipped full project/solution validation after the focused command passed and supplied the compile signal for the affected project.

## Next Sequential UOW

Recommended next production scope: Java group member solo-instance scan for grouped portals when `instanceGroupReq` is false.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/model/team2/group/PlayerGroup.java` or current Java group runtime classes
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`

Specific behavior to prove: Java group portal branch with `instanceGroupReq == false`, a group present, and no registered team instance scans each group member's solo registration before allocating a new group instance.

Focused C# command should start with:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~PortalEntryValidationService" --no-restore
```

Replace or narrow the `PortalEntryValidationService` placeholder with the exact new/edited test name after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: possible if shared team-plan shape changes beyond group portal validation. Start with focused C# tests and document whether wider validation is still needed.

## Safe Candidates

- Java group member solo-instance scan for grouped portals when `instanceGroupReq` is false.
- Spawn-engine difficulty filtering using the newly preserved `WorldMapInstanceRuntimeState.DifficultyId`.
- Alliance/league fresh allocation after group behavior remains stable.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
