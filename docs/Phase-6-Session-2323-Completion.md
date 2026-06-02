# Phase 6 Session 2323 Completion - Beshmundir Fresh Allocation Boundary

## Scope

Verified that Beshmundir's accepted difficulty response reaches the fresh group allocation path added in UOW-2322 when no group instance is registered yet.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`

Java behavior used:

- `SELECT_NONE_1` / `SELECT_NONE_2` register question id `902050`.
- Accepted question response calls `moveToInstance(responder, (byte) 2)` for the registered question id.
- `moveToInstance` resolves the portal-use path and calls `PortalService.port(...)`.
- `PortalService.port(...)` fresh group branch allocates a group instance and registers the team before transfer.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Added:

- `ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered`
- `CreateBeshmundirPortalRuntimeContextWithoutRegisteredInstance`

No production code was changed in this UOW. The production behavior under proof was added in UOW-2322; this unit verifies the Beshmundir question-response boundary uses that generic portal path.

Known limitations:

- Java difficulty id `2` is still not propagated into C# fresh allocation/spawn metadata.
- The test does not compare encrypted real-client bytes.
- Java range observer auto-deny behavior for AI requests remains unported.

## Validation Decision

- Changed surface: Beshmundir boundary tests and local runtime fixture only.
- Specific behavior/contract: accepted Beshmundir difficulty response with no registered group instance allocates a group instance, registers the team id, transfers the leader, removes the pending request, and applies cooldown.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Adjacent focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceAllocatesRegistersTeamAndTransfers" --no-restore
```

Result: passed 3, failed 0, skipped 0.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime handler branch; Java source review was the practical source-of-truth evidence.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. This UOW changed tests/docs only and used the production path added in UOW-2322.
- Broad .NET decision: skipped full project/solution validation; focused boundary tests cover the changed fixture and specific Beshmundir behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.acceptRequest` / `moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` / `HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted response now has focused evidence for both registered-instance transfer and no-registered-instance fresh group allocation. Difficulty id `2` is still not propagated into allocation metadata. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalTeamContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Beshmundir boundary now proves the generic fresh allocation path is reached through portal-use handling. Member solo-instance scan and alliance/league paths remain unported. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.moveToInstance` and `PortalService.port` | Accepted Beshmundir response removes request, resolves portal-use path, allocates instance id `2`, registers team id `0x0708090A`, registers leader, queues teleport, and applies cooldown. | Focused C# runtime packet execution plus Java source review. | Does not cover Java difficulty id propagation, range observer auto-deny, or real-client encrypted bytes. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported/extended in this UOW: 0 production artifacts; 2 artifacts verified through boundary test coverage
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Difficulty id propagation into fresh allocation remains unported.
- Java group member solo-instance scan for the `!instanceGroupReq` path remains unported.
- Alliance/league fresh allocation remains unsupported.
- Java range observer auto-deny behavior for AI requests remains unported.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2323] Prove Beshmundir fresh group allocation
```

## Next Recommended UOW

Implement a production-code slice for difficulty id propagation into fresh portal allocation metadata, starting with the Java Beshmundir accepted-response path where the registered question id passes difficulty `2`.
