# Phase 6 Session 2325 Handoff - Group Member Instance Scan

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2325-Completion.md`
- `docs/Phase-6-Session-2325-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2325, Java group member solo-instance scan when `instanceGroupReq == false`.

Completed portal slices relevant to the next work:

- Fresh group portal continuation allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.
- Beshmundir accepted difficulty response carries Java difficulty `2` into fresh portal allocation metadata.
- Bypassed group portal validation can scan member object ids for a solo registered instance before fresh group allocation.
- Bypassed group portal continuation transfers into the found member instance and does not register the team id.

Still not proven:

- Live admin/membership source for `instanceGroupReq == false`.
- No-group `!instanceGroupReq` branch for group-sized portals.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
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
- `f449bcee2 [Phase 6][UOW-2324] Carry portal difficulty into allocation`
- `[Phase 6][UOW-2325] Reuse group member solo portal instance`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2325-Completion.md`
- `docs/Phase-6-Session-2325-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch with `instanceGroupReq == false` | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# can now scan group member object registrations and reuse the first found member instance without team registration. Live permission source still needs wiring. |
| `com.aionemu.gameserver.services.instance.InstanceService.getRegisteredInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.GetRegisteredInstance` | Runtime State Lookup | Partial | Focused Unit/Boundary Tested | Partial Parity | Lookup behavior is reused for team id and member object ids. Java runtime group aggregate itself is still only represented by C# snapshots. |

## Validation From Last UOW

Initial focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ValidatePortalEntryPlan_GroupMemberFindsRegisteredTeamInstanceBeforeBlockedFanout" --no-restore
```

Result: failed 1, passed 3. The member-scan success report omitted candidate object ids. Runtime reuse behavior was already correct.

Final focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ValidatePortalEntryPlan_GroupMemberFindsRegisteredTeamInstanceBeforeBlockedFanout" --no-restore
```

Result: passed 4, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: shared team-plan shape changed, but focused tests covered validation, continuation, the adjacent registered-team branch, and fresh group allocation.

Broad .NET decision: skipped full project/solution validation after focused tests passed and supplied the compile signal for the affected project.

## Next Sequential UOW

Recommended next production scope: wire Java's `instanceGroupReq == false` source into live portal interaction.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- Java admin/membership permission classes referenced by `AdminConfig.INSTANCE_ENTER_ALL` and `MembershipConfig.INSTANCES_GROUP_REQ`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- Any existing C# membership/admin permission model
- Focused tests in `dotnetConversion/tests/Aion.GameServer.Tests`

Specific behavior to prove: the live portal entry preparation supplies `bypassGroupRequirement: true` only for the C# equivalent of Java's admin or membership bypass.

Focused C# command should start with the exact new tests plus the existing scan coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

## Safe Candidates

- Wire live admin/membership source for `instanceGroupReq == false`.
- Implement Java no-group `!instanceGroupReq` group-sized portal branch.
- Spawn-engine difficulty filtering using `WorldMapInstanceRuntimeState.DifficultyId`.
- Alliance/league fresh allocation after group behavior remains stable.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
