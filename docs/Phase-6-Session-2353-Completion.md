# Phase 6 Session 2353 Completion - AutoGroup Leave Planner

## Scope

Ported the Java `AutoGroupService.onLeaveInstance` branch contract into a C# planner, including subtype leave cleanup for the currently reviewed Java auto-instance classes.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvPFFAInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoHarmonyInstance.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`

Java behavior used:

- `InstanceService.onLeaveInstance` calls `AutoGroupService.onLeaveInstance(player)` only when `AUTO_GROUP_ENABLE` is true.
- `AutoGroupService.onLeaveInstance` always calls `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)` when the service is invoked.
- If the player belongs to a registered auto instance, Java calls the subtype `autoInstance.onLeaveInstance(player)`, then `destroyOrAddPlayersFromQuickEntries(autoInstance)`.
- `AutoPvpInstance.onLeaveInstance` unregisters the player and removes them from their group or alliance.
- `AutoPvPFFAInstance.onLeaveInstance` unregisters the player only.
- `AutoHarmonyInstance.onLeaveInstance` unregisters the player from harmony group tracking and removes them from their group.
- `destroyIfPossible` destroys only when registered auto-group players are empty and no players inside are online; otherwise quick-entry refill can run only when the template allows quick registration.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupInstanceLeavePlanService`.
- Added `AutoGroupInstanceLeaveFacts`, `AutoGroupInstanceLeavePlan`, `AutoGroupInstanceLeaveStatus`, and `AutoGroupInstanceKind`.
- Added focused tests for disabled config, missing/unregistered auto instance, PvP group/alliance cleanup, FFA/Harmony subtype cleanup, and destroy-versus-quick-entry refill decisions.

Known limitations:

- This UOW is a planner slice, not live autogroup runtime wiring.
- C# still lacks a full Java-equivalent `AutoGroupService` runtime registry for `WorldMapInstance -> AutoInstance`.
- Java quick-entry queue mutation, periodic registration packet sends, and instance destruction are represented as planned side effects, not executed.

## Validation Decision

- Changed surface: one non-live service planner plus focused tests.
- Specific behavior/contract: Java `AutoGroupService.onLeaveInstance` and auto-instance subtype leave side-effect decisions.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupService.onLeaveInstance`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is an isolated non-live planner and tests.
- Broad .NET decision: skipped full project/solution validation because the filtered planner test compiled the affected project/dependencies and directly covered the scoped Java branch contract.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeavePlanService.CreatePlan(...)` | Service Planner | Partial | Unit Tested | Partial Parity | Top-level Java branch flow is modeled. Runtime auto-instance registry, packet sends, quick-entry mutation, and destruction wiring remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.PvpRaceInstance` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Unregister plus group/alliance removal decisions covered; no live team mutation in this UOW. |
| `com.aionemu.gameserver.model.autogroup.AutoPvPFFAInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.FreeForAllArena` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Unregister-only behavior covered; no live auto-instance mutation in this UOW. |
| `com.aionemu.gameserver.model.autogroup.AutoHarmonyInstance.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceKind.HarmonyArena` planner branch | Planner Branch | Partial | Unit Tested | Partial Parity | Harmony group tracking removal plus group removal decisions covered; no live score/group reward mutation in this UOW. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.checkAndSendOpenRegistrations(Player)` | `AutoGroupInstanceLeavePlan.WouldCheckOpenRegistrations` | Planned Side Effect | Partial | Unit Tested | Partial Parity | Planner records the required refresh after service invocation; packet send/runtime opened-registration registry remains unwired. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeavePlanServiceTests.CreatePlan_SkipsAllWorkWhenJavaAutoGroupConfigDisabled` | Unit | Java source review | `AUTO_GROUP_ENABLE` gate blocks service work. | Focused planner test plus Java source review. | Does not execute live config path. |
| `AutoGroupInstanceLeavePlanServiceTests.CreatePlan_RefreshesOpenRegistrationsWhenNoRegisteredAutoInstancePlayerLikeJavaService` | Unit | Java source review | Missing/unregistered auto instance still refreshes open registrations. | Focused planner test plus Java source review. | Does not send `SM_AUTO_GROUP`. |
| `AutoGroupInstanceLeavePlanServiceTests.CreatePlan_PvpRaceLeaveUnregistersAndRemovesCurrentTeamLikeJavaAutoPvpInstance` | Unit | Java source review | PvP leave unregisters and removes group or alliance. | Focused planner test plus Java source review. | Does not mutate live `PlayerGroupRuntime` or alliance runtime. |
| `AutoGroupInstanceLeavePlanServiceTests.CreatePlan_ArenaSubtypeLeaveBranchesMatchJavaUnregisterAndGroupCleanup` | Unit | Java source review | FFA unregister-only and Harmony group cleanup decisions. | Focused planner test plus Java source review. | Does not mutate arena score/reward state. |
| `AutoGroupInstanceLeavePlanServiceTests.CreatePlan_DestroysOnlyWhenRegisteredPlayersAndOnlinePlayersAreEmptyLikeJavaDestroyIfPossible` | Unit | Java source review | Destroy versus quick-entry refill conditions. | Focused planner test plus Java source review. | Does not call `InstanceService.destroyInstance`. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 7
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live C# autogroup runtime registry equivalent to Java `autoInstances`.
- Live `AutoGroupService.onLeaveInstance` adapter wiring from delayed teleport leave.
- Live quick-entry queue mutation and periodic registration packet refresh.
- Live group/alliance removal for autogroup leave.
- Live destruction handoff from autogroup leave to `InstanceDestroyWorkflowService`.

## Commit

Commit message:

```text
[Phase 6][UOW-2353] Plan autogroup instance leave side effects
```
