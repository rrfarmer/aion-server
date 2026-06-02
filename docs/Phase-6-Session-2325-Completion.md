# Phase 6 Session 2325 Completion - Group Member Instance Scan

## Scope

Implemented Java `PortalService.port(...)` group member solo-instance scan for grouped portal entry when the default group requirement is bypassed.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- When `instanceGroupReq == false`, a grouped player first checks the registered team instance.
- If no team instance exists, Java scans `group.getMembers()` and reuses the first member object registration found for the target map.
- If no member registration exists, Java allocates a new instance and registers the group.
- Reusing a member solo registration does not register the team id.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Implemented:

- `PortalEntryValidationService.ValidatePortalEntryPlan(...)` accepts `bypassGroupRequirement`.
- Group-sized portal validation allows bypassed group requirements before building the team plan.
- Unsupported group portal planning scans `CurrentTeamMemberObjectIds` for a solo registered instance when no team instance exists and the group requirement was bypassed.
- `PortalTeamEntryPlan.RegisteredInstanceFromMemberScan` marks Java's member-scan reuse branch.
- Group continuation preserves member-scan metadata and transfers into the found member instance without registering the team id.
- `PlayerEnterWorldService.PreparePortalEntryAsync(...)` forwards the bypass flag by name-position in the validation call to avoid accidental parameter drift.

Known limitations:

- The live source of `bypassGroupRequirement` from Java admin/membership permissions is not wired into portal interaction yet; the production path now supports the branch once that source is supplied.
- The Java solo-player `!instanceGroupReq` group-sized branch remains unported.
- Alliance/league portal allocation remains unsupported.

## Validation Decision

- Changed surface: production portal validation and group continuation metadata for the Java `instanceGroupReq == false` branch.
- Specific behavior/contract: a grouped player with bypassed group requirement reuses a member's solo registered instance before allocating a new group instance and does not register the team id to that reused instance.
- Initial focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ValidatePortalEntryPlan_GroupMemberFindsRegisteredTeamInstanceBeforeBlockedFanout" --no-restore
```

Result: failed 1, passed 3. The transfer behavior reused the member instance, but the member-scan report omitted candidate object ids for the successful scan state.

- Final focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers|FullyQualifiedName~ValidatePortalEntryPlan_GroupMemberFindsRegisteredTeamInstanceBeforeBlockedFanout" --no-restore
```

Result: passed 4, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: shared team-plan shape changed, but focused tests covered validation, continuation, the adjacent registered-team branch, and fresh group allocation.
- Broad .NET decision: skipped full project/solution validation after focused tests passed and compiled the affected project.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch with `instanceGroupReq == false` | `Aion.GameServer.Services.PortalEntryValidationService` / `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | C# can now scan group member object registrations and reuse the first found member instance without team registration. Live permission source still needs wiring. |
| `com.aionemu.gameserver.services.instance.InstanceService.getRegisteredInstance` | `Aion.GameServer.World.WorldMapRuntimeStateTable.GetRegisteredInstance` | Runtime State Lookup | Partial | Focused Unit/Boundary Tested | Partial Parity | Lookup behavior is reused for team id and member object ids. Java runtime group aggregate itself is still only represented by C# snapshots. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava` | Unit | Java source review of `PortalService.port` | Bypassed grouped portal validation scans member object ids, finds member object registration, marks registered-instance transfer, and leaves team id unregistered. | Focused C# unit test plus Java source review. | Does not prove live permission wiring. |
| `QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam` | Boundary Runtime | Java source review of `PortalService.port` transfer branch | Continuation transfers into the member-registered instance, registers the entering player object, preserves existing member registration, and does not register team id. | Focused C# boundary test plus Java source review. | Does not cover real-client bytes or full Java fixture execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported/extended in this UOW: 2
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Live admin/membership source for `instanceGroupReq == false` remains unwired.
- Java solo-player `!instanceGroupReq` group-sized allocation branch remains unported.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Alliance/league fresh allocation remains unsupported.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2325] Reuse group member solo portal instance
```

## Next Recommended UOW

Wire Java's `instanceGroupReq == false` source from admin/membership permissions into live portal interaction if the current C# player permission model can represent it. If that source is not yet modeled, implement the no-group `!instanceGroupReq` group-sized portal branch or begin spawn-engine difficulty filtering.
