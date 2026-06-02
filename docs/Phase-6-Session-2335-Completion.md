# Phase 6 Session 2335 Completion - Carry Portal Difficulty Through Solo Plans

## Scope

Carried the Java Beshmundir/portal difficulty byte through C# solo portal plans so fresh solo instance allocation can use the selected difficulty without direct helper injection.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

Java behavior used:

- `BeshmundirsWalkAI.moveToInstance(player, difficult)` calls `PortalService.port(portalPath, player, getOwner(), difficult)`.
- Java `PortalService.port(...)` passes `difficult` into `InstanceService.getNextAvailableInstance(...)` for fresh solo, group, and alliance instance allocation.
- Fresh C# solo allocation already accepts a difficulty id; the missing piece was preserving it in the ordinary allowed portal plan.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added `DifficultyId` to `PortalEntryPlanResult`.
- `PortalEntryPlanResult.Allowed(...)` can now carry a difficulty id for ordinary solo/open-world continuation plans.
- `ApplyBeshmundirDifficulty(...)` now sets both the top-level plan difficulty and, when present, the team-plan difficulty.
- Solo instance continuation passes `preparation.EntryPlan.DifficultyId` into `QueueAllocatedInstancePortalTransferAsync(...)`.
- Repaired the Beshmundir boundary test `StaticData` reflection fixture to include the already-ported `StaticDoorTable` constructor parameter.

Known limitations:

- This preserves the selected difficulty through the modeled plan; real-client Beshmundir encrypted interaction remains unvalidated.
- Group/alliance paths still use `PortalTeamEntryPlan.DifficultyId`; this unit keeps that behavior but does not broaden fanout.
- Non-Beshmundir dynamic AI difficulty handlers are still not ported.

## Validation Decision

- Changed surface: production portal-plan DTO and connection allocation branch plus focused tests.
- Specific behavior/contract: Java `difficult` selected by Beshmundir/portal logic should survive plan preparation and reach fresh instance allocation for solo and group branches.
- Initial focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_SoloInstanceAllocatesRegistersAndAppliesCooldownAfterTeleport|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests.ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered" --no-restore
```

Initial result: failed 1, passed 1. Failure was `TargetParameterCountException` in `GameServerConnectionFindGroupBoundaryTests.CreateStaticDataForPortalTest`, caused by the helper missing the existing `StaticDoorTable` constructor parameter.

After fixing the adjacent helper, the same focused command was rerun.

Final result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Hygiene command:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `BeshmundirsWalkAI` plus `PortalService.port(...)`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is a narrow portal-plan metadata propagation and focused tests compiled the affected project.
- Broad .NET decision: skipped full project/solution validation because the focused command directly covered the edited solo allocation branch and the adjacent Beshmundir fresh group branch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `GameServerConnection.ApplyBeshmundirDifficulty` plus portal continuation | AI/Service Boundary | Partial | Unit Tested | Partial Parity | C# now preserves Beshmundir-selected difficulty on top-level portal plans and team plans. Dynamic AI class loading and real-client interaction remain pending. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` difficulty propagation | `PortalEntryPlanResult.DifficultyId` and `QueuePortalContinueTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh solo allocation can now receive selected difficulty from the plan, matching Java's `difficult` flow. Other portal branches remain partially modeled. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `QueuePortalContinueTransferAsync_SoloInstanceAllocatesRegistersAndAppliesCooldownAfterTeleport` | Unit | Java source review of `PortalService.port` | An allowed solo portal plan with difficulty `2` allocates an instance whose `DifficultyId` is `2`. | Focused C# unit test plus Java source review. | Does not cover encrypted real-client packets. |
| `ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered` | Unit | Java source review of `BeshmundirsWalkAI` | Beshmundir difficulty accept still allocates a fresh group instance with difficulty `2`. | Focused adjacent regression plus Java source review. | Group fanout remains limited to modeled behavior. |

## Remaining Gaps

- Real-client Beshmundir interaction validation.
- Dynamic Java AI handler loading.
- Other difficulty-selecting instance handlers.
- `InstanceHandler.onInstanceDestroy()` and empty-instance checker behavior.
- Event-specific instance spawns for Java `SpawnEngine.spawnEventSpawns(...)`.

## Commit

Commit message:

```text
[Phase 6][UOW-2335] Carry portal difficulty through solo plans
```

## Next Recommended UOW

Continue with `InstanceHandler.onInstanceDestroy()` plus remove/destroy instance lifecycle if a small C# destroy path is available, or inspect event-specific instance spawns if destroy lifecycle is not ready.
