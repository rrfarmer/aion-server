# Phase 6 Session 2342 Handoff - Resolve Bind Location Destination

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2342-Completion.md`
- `docs/Phase-6-Session-2342-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2342, modeled Java bind-location destination selection for `TeleportService.moveToBindLocation(...)`.

Relevant completed portal/instance/spawn/destroy/teleport slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Instance-exit static data loads from cache rows and resolves destination-vs-bind-fallback plans.
- Destroyed-player forced-exit plans compose force-message intent with Java-shaped exit resolution.
- Bind-location destination resolution now uses player bind point first, initial race spawn second, and Java's instance-id rule for `teleportTo(player, worldId, x, y, z, h)`.

Still not proven or not implemented:

- Composition of instance-exit bind fallback with the bind-location destination plan.
- Live forced-exit packet send and teleport dispatch.
- Exact Java missing-initial-data failure behavior.
- Live game-server destroy path wiring with all cleanup callbacks.
- Walker formation cleanup.
- Empty-instance checker scheduling and cancellation.

## Commits Made

- `[Phase 6][UOW-2342] Resolve bind location destination`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerTeleportServiceTests.cs`
- `docs/Phase-6-Session-2342-Completion.md`
- `docs/Phase-6-Session-2342-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerTeleportServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `TeleportService.moveToBindLocation(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed non-live resolver behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToBindLocation` | `Aion.GameServer.Services.PlayerTeleportService.ResolveBindLocation` | Service Plan | Partial | Unit Tested | Partial Parity | C# plans bind-point and initial-spawn destinations with Java instance-id rules. Live `teleportTo` mutation, packet fanout, and exact missing-data exception behavior are not implemented. |
| `com.aionemu.gameserver.dataholders.PlayerInitialData.getSpawnLocation` | `Aion.GameServer.Dataholders.PlayerInitialDataTable.GetSpawnLocation` | Dataholder | Partial | Unit Tested | Partial Parity | Existing C# dataholder supplies race spawn rows used by the bind resolver. This UOW tests resolver consumption, not full JAXB/runtime data parity. |

## Next Sequential UOW

Recommended next production scope: compose instance-exit bind fallback with `PlayerTeleportService.ResolveBindLocation(...)`, so destroyed-player forced-exit plans include either an instance-exit destination or a concrete bind fallback destination.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`
- `game-server/src/com/aionemu/gameserver/dataholders/PlayerInitialData.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerTeleportServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy|FullyQualifiedName~PlayerTeleportServiceTests.ResolveBindLocation" --no-restore
```

Behavior to prove: a destroyed-player forced-exit plan resolves Java `moveToInstanceExit` success to the instance-exit destination and resolves Java bind fallback to a concrete bind-location destination.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none for another non-live planner. If live packet fanout, teleport mutation, shared runtime state, or persistence is enabled, document the trigger and start with the narrow live-boundary tests.

## Safe Candidates

- Compose bind fallback destination into destroyed-player forced-exit plans.
- Add a narrow live forced-exit adapter if packet-send and teleport boundaries are already modeled.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, and handler notification.
- Walker formation cleanup on instance destroy.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

