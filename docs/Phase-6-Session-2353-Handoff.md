# Phase 6 Session 2353 Handoff - AutoGroup Leave Planner

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2353-Completion.md`
- `docs/Phase-6-Session-2353-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2353, added a C# planner for Java `AutoGroupService.onLeaveInstance` side-effect decisions.

Relevant completed instance destroy/checker/leave slices:

- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask`.
- Portal instance allocation passes the real checker scheduler callback.
- `InstanceRegisteredTeamDisbandService` maps registered team ids to group/alliance runtime membership state.
- `InstanceLeaveMessageService` models Java reset-warning message selection for instance leave.
- `SmSystemMessage` has helpers for `STR_MSG_LEAVE_INSTANCE`, `STR_MSG_LEAVE_INSTANCE_PARTY`, and `STR_MSG_LEAVE_INSTANCE_FORCE`.
- Delayed teleport completion sends the leave-instance reset-warning packet before C# position mutation and spawn packets when world or instance changes.
- Delayed teleport leave handling invokes `IInstanceLifecycleHandler.OnLeaveInstance(Player)` before reset-warning message planning.
- `AutoGroupInstanceLeavePlanService` models Java autogroup leave decisions for disabled config, missing/unregistered auto instance, PvP race instances, FFA arenas, Harmony arenas, destroy-if-empty, quick-entry refill, and open-registration refresh.

Still not proven or not implemented:

- Live C# autogroup runtime registry equivalent to Java `Map<WorldMapInstance, AutoInstance>`.
- Live `AutoGroupService.onLeaveInstance` adapter wiring from delayed teleport leave.
- Live quick-entry queue mutation, periodic registration packet refresh, and autogroup instance destruction.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.

## Commits Made

- `[Phase 6][UOW-2353] Plan autogroup instance leave side effects`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeavePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeavePlanServiceTests.cs`
- `docs/Phase-6-Session-2353-Completion.md`
- `docs/Phase-6-Session-2353-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `AutoGroupService.onLeaveInstance`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because the filtered planner test compiled the affected project/dependencies and directly covered the scoped Java branch contract.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeavePlanService.CreatePlan(...)` | Service Planner | Partial | Unit Tested | Partial Parity | Top-level Java branch flow is modeled. Runtime auto-instance registry, packet sends, quick-entry mutation, and destruction wiring remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.PvpRaceInstance` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Unregister plus group/alliance removal decisions covered; no live team mutation in this UOW. |
| `com.aionemu.gameserver.model.autogroup.AutoPvPFFAInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.FreeForAllArena` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Unregister-only behavior covered; no live auto-instance mutation in this UOW. |
| `com.aionemu.gameserver.model.autogroup.AutoHarmonyInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.HarmonyArena` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Harmony group tracking removal plus group removal decisions covered; no live score/group reward mutation in this UOW. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.checkAndSendOpenRegistrations(Player)` | `AutoGroupInstanceLeavePlan.WouldCheckOpenRegistrations` | Planned Side Effect | Partial | Unit Tested | Partial Parity | Planner records the required refresh after service invocation; packet send/runtime opened-registration registry remains unwired. |

## Next Sequential UOW

Recommended next production scope: inspect whether a narrow live adapter can consume `AutoGroupInstanceLeavePlanService` during delayed teleport leave without inventing the full Java autogroup queue. If not, port the smallest missing runtime state needed for a real `WorldMapInstance -> AutoInstance` registry.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeavePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceDestroyWorkflowService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeavePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.HandleTeleportAnimationDoneAsync_SendsLeaveInstanceResetWarningBeforeSpawnLikeJavaSpawnTask" --no-restore
```

Before running, narrow the filter to the exact edited test name and closest adjacent test after discovery. If only documentation changes are made, use `git diff --check` instead.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: none unless the next UOW enables live side effects through connection dispatch, group/alliance mutation, scheduler/destruction, or packet sends. If such a trigger applies, write it into the active completion/handoff notes before any broad .NET run.

## Safe Candidates

- Add a narrow autogroup leave runtime adapter using the planner if C# can represent registered auto players without broad queue work.
- Add a minimal C# auto-instance registry keyed by world/instance if that unblocks live leave wiring.
- Port `PeriodicInstanceManager.checkAndSendOpenRegistrations` as a non-live planner for opened-registration icon packets.
- Port or explicitly model `ConquerorAndProtectorService.onLeaveMap` if supporting C# state exists.
- Review pet position update and same-map spawn behavior in delayed teleport completion.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
