# Phase 6 Session 2334 Handoff - Add Instance Create Lifecycle Hook

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2334-Completion.md`
- `docs/Phase-6-Session-2334-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2334, C# fresh portal allocation paths now have a one-shot `onInstanceCreate`-style lifecycle hook fired after instance spawning and before transfer.

Relevant completed portal/instance/spawn slices:

- Fresh group portal allocation spawns instance NPCs, static doors, and static handler objects.
- Fresh solo portal allocation spawns instance NPCs/static objects through `WorldNpcSpawnService.SpawnWorldNpcsForInstance(...)`.
- Fresh solo/group allocation paths call `WorldMapInstanceRuntimeState.NotifyInstanceCreated()` after the C# spawn step.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static door templates load from `staticdoor_templates`, and instance spawning seeds per-instance static door open/closed state.
- Static handler spawn groups resolve item templates and create `WorldStaticObject` entries.

Still not proven or not implemented:

- Dynamic Java instance handler discovery and per-map handler class selection.
- `InstanceHandler.onInstanceDestroy()` and other handler callbacks.
- Empty-instance checker behavior.
- Higher-level solo portal difficulty propagation for Beshmundir-style difficulty selection.
- Event-specific instance spawns and custom instance handler suppliers.
- Static object known-list visibility, packet fanout, and controller/interaction behavior.

## Commits Made

- `faa3d4df3 [Phase 6][UOW-2333] Spawn solo fresh portal instances`
- `[Phase 6][UOW-2334] Add instance create lifecycle hook`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/World/IInstanceLifecycleHandler.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeStateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2334-Completion.md`
- `docs/Phase-6-Session-2334-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` was already dirty and should remain unstaged unless the user explicitly asks to edit it.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.instance.handlers.InstanceHandler.onInstanceCreate` | `Aion.GameServer.World.IInstanceLifecycleHandler.OnInstanceCreate` | Interface | Partial | Unit Tested | Partial Parity | C# has the creation callback surface and invokes it once after fresh allocation spawns. Other Java handler methods and dynamic handler discovery remain missing. |
| `com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler.onInstanceCreate` | `Aion.GameServer.World.GeneralInstanceLifecycleHandler.OnInstanceCreate` | Handler | Complete | Unit Tested | Verified Parity | Java method is a no-op; C# no-op behavior is deterministic and covered by allocation callback tests. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance` lifecycle ordering | `GameServerConnection` fresh allocation paths plus `WorldMapInstanceRuntimeState.NotifyInstanceCreated` | Service Boundary | Partial | Unit Tested | Partial Parity | C# fresh portal paths now notify after spawn and before transfer. Non-portal instance creation, event/custom handler suppliers, empty checker, and destroy lifecycle remain pending. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_NotifiesInstanceCreateOnceLikeJavaInstanceService|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Hygiene:

```powershell
git diff --check
```

Result: passed; only Git line-ending warnings were reported.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `InstanceService.getNextAvailableInstance(...)` lifecycle ordering. Java source review was used as source-of-truth evidence.

Broad-validation trigger: live portal allocation lifecycle side effect.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the one-shot hook plus fresh allocation ordering.

## Next Sequential UOW

Recommended next production scope: carry Beshmundir-style difficulty selection through higher-level solo portal entry plans so solo fresh allocations can use selected difficulty without direct helper injection.

Java artifacts:

- `game-server/data/handlers/ai/instance/beshmundir/BeshmundirsWalkAI.java` or equivalent Beshmundir AI handler path.
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests" --no-restore
```

Narrow after Work Discovery to the exact Beshmundir/portal-difficulty test plus the adjacent solo allocation test. Do not run a full project/solution test unless a broad trigger is named first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the change touches packet primitives, persistence, scheduler behavior, or live dispatch beyond the focused portal branch.

## Safe Candidates

- Higher-level Beshmundir solo difficulty propagation into `PortalEntryPlanResult`.
- `InstanceHandler.onInstanceDestroy()` plus remove/destroy instance lifecycle if a small C# destroy path is available.
- Event-specific instance spawn branch for Java `SpawnEngine.spawnEventSpawns(...)`.
- Static object known-list/visibility model if C# known-list infrastructure already supports non-NPC visible objects.

Avoid:

- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
