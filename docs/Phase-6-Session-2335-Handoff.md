# Phase 6 Session 2335 Handoff - Carry Portal Difficulty Through Solo Plans

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2335-Completion.md`
- `docs/Phase-6-Session-2335-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2335, C# portal entry plans now preserve a selected difficulty id for ordinary allowed solo plans and still preserve it for team plans.

Relevant completed portal/instance/spawn slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Fresh solo allocation can receive difficulty from `PortalEntryPlanResult.DifficultyId`.
- Beshmundir difficulty selection now sets both top-level plan difficulty and team-plan difficulty when applicable.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static door and static handler object materialization are modeled for instance spawning.

Still not proven or not implemented:

- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.
- `InstanceHandler.onInstanceDestroy()` and empty-instance checker behavior.
- Event-specific instance spawns and custom instance handler suppliers.
- Static object known-list visibility, packet fanout, and controller/interaction behavior.
- Real-client/encrypted socket bytes for Beshmundir and portal branches.

## Commits Made

- `a22d282ae [Phase 6][UOW-2334] Add instance create lifecycle hook`
- `[Phase 6][UOW-2335] Carry portal difficulty through solo plans`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2335-Completion.md`
- `docs/Phase-6-Session-2335-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `GameServerConnection.ApplyBeshmundirDifficulty` plus portal continuation | AI/Service Boundary | Partial | Unit Tested | Partial Parity | C# now preserves Beshmundir-selected difficulty on top-level portal plans and team plans. Dynamic AI class loading and real-client interaction remain pending. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` difficulty propagation | `PortalEntryPlanResult.DifficultyId` and `QueuePortalContinueTransferAsync` | Service Boundary | Partial | Unit Tested | Partial Parity | Fresh solo allocation can now receive selected difficulty from the plan, matching Java's `difficult` flow. Other portal branches remain partially modeled. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_SoloInstanceAllocatesRegistersAndAppliesCooldownAfterTeleport|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests.ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptAllocatesGroupInstanceWhenNoneRegistered" --no-restore
```

Initial result: failed 1, passed 1 due an adjacent stale `StaticData` reflection fixture missing `StaticDoorTable`.

Final result after helper fix: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `BeshmundirsWalkAI` plus `PortalService.port(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the edited solo branch plus adjacent Beshmundir branch.

## Next Sequential UOW

Recommended next production scope: inspect Java instance destroy lifecycle and C# instance removal support, then port the smallest safe `onInstanceDestroy()` slice if available.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/InstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/instance/handlers/GeneralInstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/TemporarySpawnEngine.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Narrow after Work Discovery to exact destroy/removal tests. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the unit enables live scheduler cleanup, object deletion, or broader world-state mutation.

## Safe Candidates

- `InstanceHandler.onInstanceDestroy()` plus modeled instance removal/destroy lifecycle.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.
- Static object known-list/visibility model if C# known-list infrastructure already supports non-NPC visible objects.
- Real-client Beshmundir/portal smoke once the portal/runtime branch is mature enough.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
