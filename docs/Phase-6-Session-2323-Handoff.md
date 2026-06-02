# Phase 6 Session 2323 Handoff - Beshmundir Fresh Allocation Boundary

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2323-Completion.md`
- `docs/Phase-6-Session-2323-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2323, Beshmundir accepted difficulty response boundary proof for fresh group allocation.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir grouped non-leader open-instance `INSTANCE_ENTRY`: resolves `portal_use` path and transfers into the registered group instance.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Beshmundir accepted difficulty response with registered group instance: removes the pending request and transfers through the portal-use path.
- Fresh group portal continuation: allocates the next runtime instance, registers the team id, transfers the player, and applies cooldown.
- Beshmundir accepted difficulty response with no registered group instance: now has focused boundary evidence that it allocates the group instance through the generic portal-use path.

Still not proven:

- Difficulty-specific fresh instance selection / spawn metadata.
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
- `[Phase 6][UOW-2323] Prove Beshmundir fresh group allocation`

## Files Changed In Last UOW

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2323-Completion.md`
- `docs/Phase-6-Session-2323-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.acceptRequest` / `moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` / `HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted response now has focused evidence for both registered-instance transfer and no-registered-instance fresh group allocation. Difficulty id `2` is still not propagated into allocation metadata. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Beshmundir boundary now proves the generic fresh allocation path is reached through portal-use handling. Member solo-instance scan and alliance/league paths remain unported. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 3, failed 0, skipped 0.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime handler branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none. This UOW changed tests/docs only and used the production path added in UOW-2322.

Broad .NET decision: skipped full project/solution validation; focused boundary tests covered the changed fixture and specific Beshmundir behavior.

## Next Sequential UOW

Recommended next production scope: difficulty id propagation into fresh portal allocation metadata.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingKiskBindRequest.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Specific behavior to prove: Java Beshmundir accepted question id `902050` passes difficulty `2` into `PortalService.port(...)`; C# should carry that difficulty through the pending request and fresh allocation metadata without changing existing registered-instance transfer behavior.

Focused C# command should start with:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~WorldMapInstanceRuntimeState" --no-restore
```

Narrow or replace the `WorldMapInstanceRuntimeState` placeholder with the exact new/edited test name after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: possible if world runtime state constructors or allocation APIs change. Start with focused C# tests and document whether a wider run is still needed.

## Safe Candidates

- Difficulty id propagation into fresh allocation metadata, scoped to Beshmundir and generic portal allocation.
- Java group member solo-instance scan for grouped portals when `instanceGroupReq` is false.
- Alliance/league fresh allocation after group behavior remains stable.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
