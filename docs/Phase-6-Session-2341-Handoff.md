# Phase 6 Session 2341 Handoff - Compose Destroy Player Exit Resolution

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2341-Completion.md`
- `docs/Phase-6-Session-2341-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2341, composed destroyed-player force-message plans with instance-exit destination/fallback planning.

Relevant completed portal/instance/spawn/destroy slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Instance-exit static data loads from cache rows and resolves destination-vs-bind-fallback plans.
- Destroyed-player forced-exit plans now compose force-message intent with Java-shaped exit resolution.

Still not proven or not implemented:

- Live forced-exit packet send and teleport dispatch.
- Bind-location coordinate resolution.
- Live game-server destroy path wiring with all cleanup callbacks.
- Walker formation cleanup.
- Empty-instance checker scheduling and cancellation.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.

## Commits Made

- `[Phase 6][UOW-2341] Compose destroy player exit resolution`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2341-Completion.md`
- `docs/Phase-6-Session-2341-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy" --no-restore
```

Result: passed 3, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for this destroy-player branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed non-live planner behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitResolutionPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# composes Java's forced-leave packet intent with Java-shaped exit destination/fallback planning for players in the destroyed instance. Live packet send and teleport mutation remain pending. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.ResolveInstanceExit` | Service Plan | Partial | Unit Tested | Partial Parity | Destination/fallback branch is tested, but live teleport and bind-location coordinate resolution remain pending. |

## Next Sequential UOW

Recommended next production scope: inspect whether C# already has a safe live packet-send/teleport boundary for forced instance exits. If ready, add a narrow adapter that consumes `InstancePlayerForcedExitResolutionPlan`; if not ready, port bind-location resolution first.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- bind-location Java artifacts discovered during Work Discovery

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs` if present/relevant
- game connection/session packet send boundary if already modeled
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- closest packet/teleport adapter tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy" --no-restore
```

If the next UOW enables a live adapter, add only the nearest adapter/packet-send test class to that filter. Do not run a full project/solution test unless a broad trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none for another non-live planner. If live packet fanout, teleport mutation, shared runtime state, or persistence is enabled, document the trigger and start with the narrow live-boundary tests.

## Safe Candidates

- Port bind-location resolution if live teleport dispatch is not ready.
- Add a narrow live forced-exit adapter if packet-send and teleport boundaries are already modeled.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, and handler notification.
- Walker formation cleanup on instance destroy.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

