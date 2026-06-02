# Phase 6 Session 2343 Handoff - Compose Forced Exit Destination

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2343-Completion.md`
- `docs/Phase-6-Session-2343-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2343, composed destroyed-player forced-exit plans with final modeled destinations for both Java instance-exit success and Java bind fallback.

Relevant completed portal/instance/spawn/destroy/teleport slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)`.
- Instance-exit static data loads from cache rows and resolves destination-vs-bind-fallback plans.
- Bind-location destination resolution uses player bind point first, initial race spawn second, and Java's instance-id rule.
- Destroyed-player forced-exit teleport plans now include the modeled final destination when available.

Still not proven or not implemented:

- Live forced-exit packet send and teleport dispatch.
- Live game-server destroy path wiring with all cleanup callbacks.
- Walker formation cleanup.
- Empty-instance checker scheduling and cancellation.
- Exact Java missing-initial-data failure behavior.

## Commits Made

- `[Phase 6][UOW-2343] Compose forced exit destination`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2343-Completion.md`
- `docs/Phase-6-Session-2343-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitResolutionPlansComposesMessageAndExitLikeJavaDestroy|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitTeleportPlansComposesBindFallbackDestinationLikeJava|FullyQualifiedName~PlayerTeleportServiceTests.ResolveBindLocation" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for the combined destroy/teleport/bind branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed non-live planner behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.destroyInstance` player branch | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitTeleportPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# models force message, instance-exit destination, and bind fallback destination for destroyed-instance players. Live packet send, teleport mutation, and destroy-path integration remain pending. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.CreatePlayerForcedExitTeleportPlans` | Service Plan | Partial | Unit Tested | Partial Parity | C# composes valid exit-world destination with bind fallback. Runtime teleport behavior and bind missing-data exception behavior are not proven. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToBindLocation` | `Aion.GameServer.Services.PlayerTeleportService.ResolveBindLocation` | Service Plan | Partial | Unit Tested | Partial Parity | Destination selection is tested; live mutation is pending. |

## Next Sequential UOW

Recommended next production scope: inspect the C# packet-send and teleport mutation boundaries and add the narrowest live adapter that can consume `InstancePlayerForcedExitTeleportPlan`, if those boundaries are ready. If live wiring is still too broad, port Java `WalkerFormator.onInstanceDestroy(worldId, instanceId)` cleanup as another destroy-path parity slice.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/spawnengine/WalkerFormator.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- packet-send boundary discovered during Work Discovery
- walker/route services discovered during Work Discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- closest adapter or walker cleanup tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitTeleportPlansComposesBindFallbackDestinationLikeJava" --no-restore
```

If the next UOW enables a live adapter or walker cleanup, add only the nearest adapter/walker test class to that filter. Document the broad-validation trigger if live packet fanout, teleport mutation, shared runtime state, or persistence is enabled.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none for another non-live planner or walker cleanup plan. Live packet fanout or teleport mutation is a broad-validation trigger; start focused and document it before any wider run.

## Safe Candidates

- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are already modeled.
- Port walker formation cleanup on instance destroy.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, handler notification, and walker cleanup.
- Empty-instance checker scheduling/cancellation.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

