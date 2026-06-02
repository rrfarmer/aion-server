# Phase 6 Session 2354 Completion - AutoGroup Leave Runtime State

## Scope

Added the smallest C# runtime state needed to apply the Java `AutoGroupService.onLeaveInstance` planner to registered auto-instance players.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvPFFAInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoHarmonyInstance.java`

Java behavior used:

- Java tracks active autogroup instances in `Map<WorldMapInstance, AutoInstance>`.
- Registered autogroup players are removed by subtype `onLeaveInstance` implementations.
- `AutoPvpInstance.onLeaveInstance` removes a leaving player from group or alliance.
- `AutoPvPFFAInstance.onLeaveInstance` unregisters the player only.
- `AutoHarmonyInstance.onLeaveInstance` unregisters harmony tracking and removes group membership.
- `AutoGroupService.destroyIfPossible` removes the autogroup instance when no registered autogroup players remain and no players inside are online.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupInstanceLeaveRuntimeService`.
- Added minimal runtime registration, key, snapshot, and result records for autogroup leave state.
- Added runtime tests for PvP group removal, PvP alliance removal, destroy-if-empty registry removal, and missing/unregistered autogroup instance behavior.

Known limitations:

- This UOW does not yet wire the runtime service into `GameServerConnection`.
- Actual Java quick-entry queue refill and periodic registration packet refresh remain planned side effects.
- Actual instance destruction through `InstanceDestroyWorkflowService` is not invoked yet; this runtime removes only its autogroup registry entry when Java would destroy.
- Harmony score/reward state is not modeled.

## Validation Decision

- Changed surface: one runtime service plus focused tests; no live connection dispatch wiring.
- Specific behavior/contract: Java autogroup leave unregisters tracked players, removes group/alliance membership for PvP subtype cleanup, and removes autogroup registry state only when Java `destroyIfPossible` would destroy.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. The first attempt exposed a result-type naming typo, which was fixed before the passing run. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for autogroup leave runtime behavior; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. This unit adds isolated runtime state and does not alter live connection dispatch.
- Broad .NET decision: skipped full project/solution validation because the filtered runtime/planner tests compiled the affected project/dependencies and directly covered the scoped Java behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.autoInstances` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Registry | Partial | Unit Tested | Partial Parity | Minimal world/instance keyed registry exists for leave handling. Full queue, creation, quick-entry, and packet refresh runtime remains incomplete. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Planner is consumed and registered player/team cleanup is applied. Connection dispatch, periodic registration packets, quick-entry queue, and instance destruction remain unwired. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance.onLeaveInstance(Player)` | `AutoGroupInstanceLeaveRuntimeService` PvP branch | Runtime Branch | Partial | Unit Tested | Partial Parity | Registered player removal plus group/alliance removal covered against C# runtimes. Packet fanout from group/alliance leave workflows is not sent here. |
| `com.aionemu.gameserver.model.autogroup.AutoPvPFFAInstance.onLeaveInstance(Player)` | `AutoGroupInstanceLeaveRuntimeService` FFA branch | Runtime Branch | Partial | Unit Tested | Partial Parity | Registered player removal and destroy-if-empty registry removal covered. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_UnregistersAndRemovesGroupForJavaAutoPvpInstance` | Unit | Java source review | PvP autogroup leave unregisters the player and removes group membership. | Focused runtime test plus Java source review. | Does not send group leave packets. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_UnregistersAndRemovesAllianceForJavaAutoPvpInstance` | Unit | Java source review | PvP autogroup leave removes alliance membership when the player is not grouped. | Focused runtime test plus Java source review. | Does not send alliance leave packets. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_RemovesRegistryWhenJavaDestroyIfPossibleWouldDestroy` | Unit | Java source review | Registry entry is removed only when Java `destroyIfPossible` would destroy. | Focused runtime test plus Java source review. | Does not invoke instance destruction workflow. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_MissingOrUnregisteredInstanceOnlyPlansOpenRegistrationRefresh` | Unit | Java source review | Missing/unregistered autogroup instance leaves registry untouched and plans registration refresh. | Focused runtime test plus Java source review. | Does not send `SM_AUTO_GROUP` refresh packets. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Live wiring from delayed teleport leave into `AutoGroupInstanceLeaveRuntimeService`.
- Java periodic registration refresh packet sends.
- Java quick-entry queue refill.
- Actual instance destruction through `InstanceDestroyWorkflowService` when autogroup destroy is possible.
- Harmony arena score/reward state.

## Commit

Commit message:

```text
[Phase 6][UOW-2354] Add autogroup leave runtime state
```
