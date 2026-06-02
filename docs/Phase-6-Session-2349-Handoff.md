# Phase 6 Session 2349 Handoff - Registered Team Disband Checker Lookup

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2349-Completion.md`
- `docs/Phase-6-Session-2349-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2349, added registered-team disband lookup for the portal empty-instance checker callback.

Relevant completed instance destroy/checker slices:

- `InstanceDestroyWorkflowService` composes concrete instance destroy cleanup.
- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask` and schedules a cancellable 60-second fixed-rate task.
- `WorldMapInstanceRuntimeState` stores/cancels the checker task, records last player leave time, and stores a registered team id.
- `GameServerOptions.Instance` loads Java normal/solo destroy-delay config keys.
- `GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` passes the real checker scheduler callback for portal-allocated instances.
- `InstanceRegisteredTeamDisbandService` maps retained registered team ids to C# group/alliance runtime membership state.

Still not proven or not implemented:

- Other runtime instance creation call sites need real scheduler callback review.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.
- Full team lifecycle parity is broader than the registered-team checker lookup.

## Commits Made

- `[Phase 6][UOW-2349] Add registered team disband checker lookup`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/InstanceRegisteredTeamDisbandService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InstanceRegisteredTeamDisbandServiceTests.cs`
- `docs/Phase-6-Session-2349-Completion.md`
- `docs/Phase-6-Session-2349-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceRegisteredTeamDisbandServiceTests|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the resolver/checker/portal callback contracts.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()` | `Aion.GameServer.Services.InstanceRegisteredTeamDisbandService.IsRegisteredTeamDisbanded(...)` | Service Helper | Complete | Unit Tested | Partial Parity | Active group/alliance and retained-id-after-removal cases are covered. C# uses removed runtime rows rather than retained empty Java team objects. |
| `com.aionemu.gameserver.model.team.GeneralTeam.isDisbanded()` | `Aion.GameServer.Services.PlayerGroupRuntime.GetMemberObjectIds(...)` / `PlayerAllianceRuntime.GetMemberObjectIds(...)` | Runtime State | Partial | Unit Tested | Partial Parity | Used by the registered-team resolver; complete team lifecycle parity remains separate. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Network.Aion.GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` | Runtime Caller | Partial | Unit Tested | Partial Parity | Portal scheduler callback now includes registered-team disband lookup. Other creation call sites remain. |

## Next Sequential UOW

Recommended next production scope: review and wire the next concrete runtime instance creation call site to `InstanceEmptyInstanceCheckerService.Schedule(...)`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- Auto-group or handler call sites discovered by searching Java `getNextAvailableInstance` usages.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- adjacent portal/auto-group tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests" --no-restore
```

Narrow this further to the edited call-site test plus `InstanceEmptyInstanceCheckerServiceTests` if the full class is slow.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the next unit changes shared group/alliance runtime state, live dispatch, or scheduler primitives.

## Safe Candidates

- Wire another runtime instance creation call site to the checker callback.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.
- Wire an existing C# instance handler/auto-group destroy call site to `InstanceDestroyWorkflowService`.
- Review Java `InstanceService.onLeaveInstance` system-message behavior for a small production parity slice.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
