# Phase 6 Session 2340 Handoff - Resolve Instance Exit Fallback

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2340-Completion.md`
- `docs/Phase-6-Session-2340-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2340, modeled Java instance-exit destination resolution and bind-location fallback for `TeleportService.moveToInstanceExit(...)`.

Relevant completed portal/instance/spawn/destroy slices:

- Fresh solo/group allocation paths spawn instance objects and notify `onInstanceCreate` after spawning.
- Portal difficulty is preserved on top-level allowed plans and team plans.
- Instance spawning filters by nonzero difficulty id and uses the allocated runtime instance id.
- Static doors and static handler objects are materialized for instance spawning.
- Temporary spawn tracking is per `(spawn, instanceId)`.
- Modeled destroy order includes map removal, optional temporary cleanup, optional non-player object cleanup, and destroy handler notification.
- Player forced-exit planning selects players in the destroyed instance and plans `STR_MSG_LEAVE_INSTANCE_FORCE(0)` plus `moveToInstanceExit`.
- Instance-exit static data now loads from cache rows, preserves Java defaults, and resolves destination-vs-bind-fallback plans.

Still not proven or not implemented:

- Composition of forced-exit player plans with instance-exit resolution.
- Live forced-exit packet send and teleport dispatch.
- Bind-location coordinate resolution.
- Live game-server destroy path wiring with all cleanup callbacks.
- Empty-instance checker scheduling and cancellation.
- Walker formation cleanup.
- Dynamic Java AI/instance handler discovery and per-map/per-AI handler class selection.

## Commits Made

- `[Phase 6][UOW-2340] Resolve instance exit fallback`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/InstanceExitTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/Phase-6-Session-2340-Completion.md`
- `docs/Phase-6-Session-2340-Handoff.md`

Unrelated dirty file to preserve:

- `docs/parity-verification.md` may already be dirty and should remain unstaged unless the user explicitly asks to edit it.

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.StaticData_LoadsInstanceExitSummariesWithJavaRaceFallbacks|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Repository hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceExitData.getInstanceExit(...)` or `TeleportService.moveToInstanceExit(...)`. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because focused tests compiled the affected project and directly covered the changed parser/resolver behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dataholders.InstanceExitData` | `Aion.GameServer.Dataholders.InstanceExitTable` | Dataholder | Partial | Unit Tested | Partial Parity | C# groups exits by instance world id and preserves source order. `getInstanceExit` behavior for `PC_ALL`, exact race, and missing rows is tested from reviewed Java logic. JAXB lifecycle and Java enum parsing are not runtime-compared. |
| `com.aionemu.gameserver.model.templates.portal.InstanceExit` | `Aion.GameServer.Dataholders.InstanceExitSummary` | DTO | Partial | Unit Tested | Partial Parity | Scalar attributes and Java defaults for `race=PC_ALL` and `h=0` are covered by static-data loading test. Setter/getter API parity is not needed for current C# immutable summary shape. |
| `com.aionemu.gameserver.services.teleport.TeleportService.moveToInstanceExit` | `Aion.GameServer.Services.InstanceRuntimeService.ResolveInstanceExit` | Service Plan | Partial | Unit Tested | Partial Parity | C# resolves destination vs bind fallback using Java's `InstanceService.instanceExists(exitWorld, 1)` condition. Live teleport mutation and bind-location resolution remain pending. |

## Next Sequential UOW

Recommended next production scope: compose destroyed-player forced-exit plans with instance-exit resolution so each affected player plan includes Java's forced message and the modeled teleport destination or bind fallback.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/InstanceExitData.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/InstanceExitTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_CreatePlayerForcedExitPlansSendsForceMessageAndMoveToInstanceExitLikeJava|FullyQualifiedName~WorldMapRuntimeStateTests.InstanceRuntimeService_ResolveInstanceExitUsesRaceExitAndFallsBackToBindLikeJavaTeleportService" --no-restore
```

Behavior to prove: destroyed-instance players receive Java's force message and their planned exit action can be resolved using Java `moveToInstanceExit` destination/fallback rules. Narrow to a new exact test name after Work Discovery if the next unit adds a composition method.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless live teleport dispatch, packet fanout, shared static-data cache semantics, or broad runtime state mutation is enabled.

## Safe Candidates

- Compose forced-exit player plans with instance-exit resolution.
- Live forced-exit send/teleport dispatch if all required boundaries are already modeled.
- Live destroy wiring with temporary cleanup, player forced-exit planning, non-player cleanup, and handler notification.
- Walker formation cleanup on instance destroy.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

