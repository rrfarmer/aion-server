# Phase 6 Session 2348 Handoff - Portal Empty Checker Runtime Wiring

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2348-Completion.md`
- `docs/Phase-6-Session-2348-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2348, wired portal instance allocation to the real empty-instance checker scheduler.

Relevant completed instance destroy/checker slices:

- `InstanceDestroyWorkflowService` composes concrete instance destroy cleanup.
- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask` and schedules a cancellable 60-second fixed-rate task.
- `WorldMapInstanceRuntimeState` stores/cancels the checker task and records last player leave time.
- `InstanceRuntimeService.GetNextAvailableInstance(...)` and portal creation helpers expose Java `autoDestroy` scheduler callback semantics.
- `GameServerOptions.Instance` now loads Java normal/solo destroy-delay config keys.
- `GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` now passes the real scheduler callback for portal-allocated instances.

Still not proven or not implemented:

- Runtime registered-team disbanded lookup for checker decisions.
- Other runtime instance creation call sites need real scheduler callback review.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.

## Commits Made

- `[Phase 6][UOW-2348] Wire portal empty checker scheduling`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `docs/Phase-6-Session-2348-Completion.md`
- `docs/Phase-6-Session-2348-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~InstanceServiceFormulaServiceTests|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests.Schedule_StoresCancellableFixedRateTaskLikeJavaGetNextAvailableInstance" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceService.getNextAvailableInstance(...)`, `InstanceService.getDestroyDelaySeconds(...)`, or `InstanceConfig`; Java source/config review was used as source-of-truth evidence.

Broad-validation trigger: live runtime caller and scheduler wiring, but the blast radius was isolated by the focused portal allocation, config, formula, and checker tests.

Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the changed caller/config contracts.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.configs.main.InstanceConfig.INSTANCE_DESTROY_DELAY_SECONDS` | `Aion.GameServer.Configuration.GameServerInstanceOptions.DestroyDelaySeconds` | Config | Complete | Unit Tested | Verified Parity | Default `600` and override behavior covered by focused config tests. |
| `com.aionemu.gameserver.configs.main.InstanceConfig.SOLO_INSTANCE_DESTROY_DELAY_SECONDS` | `Aion.GameServer.Configuration.GameServerInstanceOptions.SoloDestroyDelaySeconds` | Config | Complete | Unit Tested | Verified Parity | Default `600` and override behavior covered by focused config tests. |
| `com.aionemu.gameserver.services.instance.InstanceService.getDestroyDelaySeconds(WorldMapInstance)` | `Aion.GameServer.Services.InstanceServiceFormulaService.CreateDestroyDelayPlan(...)` | Service Formula | Complete | Unit Tested | Verified Parity | Existing formula tests cover solo vs normal delay selection; portal scheduler callback now uses this formula. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Network.Aion.GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` | Runtime Caller | Partial | Unit Tested | Partial Parity | Portal allocation now passes the real checker scheduler and stores the fixed-rate task. Other runtime creation paths and registered-team disbanded lookup remain. |

## Next Sequential UOW

Recommended next production scope: add runtime registered-team disbanded lookup for empty-instance checker decisions, or wire the next concrete runtime instance creation call site to the checker callback if team lookup requires unavailable runtime state.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- Java team/alliance registration classes used by `WorldMapInstance.getRegisteredTeam()`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/InstanceEmptyInstanceCheckerService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- C# player group/alliance/registered instance runtime tables discovered during Work Discovery
- `dotnetConversion/tests/Aion.GameServer.Tests/InstanceEmptyInstanceCheckerServiceTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests" --no-restore
```

Add only the nearest runtime table/adapter test if the next unit wires a concrete registered-team lookup.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the next unit changes shared group/alliance runtime state or live dispatch wiring beyond the checker lookup adapter.

## Safe Candidates

- Add runtime registered-team disbanded lookup for checker decisions.
- Review and wire the next runtime instance creation call site to `InstanceEmptyInstanceCheckerService.Schedule(...)`.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.
- Wire an existing C# instance handler/auto-group destroy call site to `InstanceDestroyWorkflowService`.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
