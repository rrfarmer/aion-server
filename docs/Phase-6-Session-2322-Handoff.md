# Phase 6 Session 2322 Handoff - Fresh Group Portal Allocation

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2322-Completion.md`
- `docs/Phase-6-Session-2322-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2322, fresh group portal allocation.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir grouped non-leader open-instance `INSTANCE_ENTRY`: resolves `portal_use` path and transfers into the registered group instance.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Beshmundir accepted difficulty response: removes the pending request and moves through the same portal-use path when a registered group instance exists.
- Registered group portal continuation: transfers player into existing team instance and applies/skips cooldown according to reentry.
- Fresh group portal continuation: allocates the next runtime instance, registers the team id, transfers the player, and applies cooldown.

Still not proven:

- Beshmundir leader difficulty acceptance when no registered instance exists.
- Difficulty-specific fresh instance selection.
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
- `[Phase 6][UOW-2322] Allocate fresh group portal instances`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2322-Completion.md`
- `docs/Phase-6-Session-2322-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Fresh group allocation for maxPlayers `3`/`6` now allocates a runtime instance, registers the team id, transfers the player, and applies cooldown. Difficulty id, optional member solo-instance scan, alliance/league allocation, and real-client bytes remain unverified. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(int, byte, int)` | `Aion.GameServer.World.WorldMapRuntimeStateTable.CreateNextWorldMapInstance` via `QueuePortalTeamContinueTransferAsync` | Runtime State Allocation | Partial | Focused Boundary Tested | Partial Parity | Existing map-local instance allocation is now used for group portal continuation. Spawn engine, instance handler lifecycle, auto-destroy, and difficulty-specific spawning remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.registerTeam` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.RegisterTeamId` | Runtime State | Partial | Focused Boundary Tested | Partial Parity | Fresh group portal allocation now calls the existing team registration primitive and verifies team/player registration. Full Java team object storage is not represented. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists" --no-restore
```

Result: passed 4, failed 0, skipped 0.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime portal branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: shared world runtime state is used, but world state internals were not changed.

Broad .NET decision: skipped full project/solution validation after focused tests covered fresh group allocation, registered group transfer, registered group reentry, and the closest Beshmundir accepted-response caller.

## Next Sequential UOW

Recommended next runtime scope: Beshmundir leader difficulty acceptance with no registered group instance.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Specific behavior to prove: accepted Beshmundir difficulty question response with a valid group and no registered team instance should resolve the portal-use path, allocate the new group instance through `QueuePortalTeamContinueTransferAsync`, register the team id, transfer the leader, and apply cooldown.

Focused C# command should start with:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Replace the Beshmundir placeholder with the exact new test name after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected if the next UOW only exercises the existing generic allocation path from the Beshmundir question-response boundary.

## Safe Candidates

- Beshmundir leader difficulty acceptance with no registered instance, using the newly ported generic fresh group allocation.
- Difficulty id propagation into fresh allocation, scoped to Beshmundir/portal path.
- Java group member solo-instance scan for grouped portals when `instanceGroupReq` is false.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Alliance/league portal allocation until group allocation remains stable.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
