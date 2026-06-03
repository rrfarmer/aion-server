# Phase 6 Session 2384 Completion - Allocate Autogroup Ready Match Instance Ids

## Scope

Replaced the live ready-match synthetic autogroup runtime id with a narrow C# world-map allocation bridge when runtime world state is available.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`

Java behavior used:

- `AutoGroupService.createNewInstance(...)` calls `InstanceService.getNextAvailableInstance(...)` before ready-window delivery.
- Java `InstanceService.getNextAvailableInstance(...)` creates the next map-local `WorldMapInstance`, sets max players, invokes instance-create handler behavior, and returns the allocated instance.
- `AutoInstance.onInstanceCreate(instance)` stores the instance reference and records `startInstanceTime`.
- For periodic PvP matches, pre-instance max players are the sum of the two faction capacities; in a ready match this equals the matched player count.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- `ApplyReadyMatchPlanAsync(...)` now accepts a materializer callback so live callers can replace the non-live runtime-registration intent before registration/window delivery.
- `GameServerConnection` materializes ready-match runtime registrations through `InstanceRuntimeService.GetNextAvailableInstance(...)` using `GameServerRuntimeContext.WorldMapStates`.
- The live bridge now:
  - allocates a map-local instance id;
  - uses matched player count as `maxPlayers`;
  - disables auto-destroy for this partial auto-instance bridge, matching Java `createNewInstance(..., autoDestroy: false)`;
  - calls `NotifyInstanceCreated()`;
  - stores `StartInstanceTime` on the autogroup runtime registration/state/snapshot.
- Existing non-live service paths still produce registration intents without requiring world state.
- Autogroup connection test fixtures now include modeled instance world-map summaries for their auto-group maps.

Known limitations:

- This is still partial Java `createNewInstance(...)` parity.
- The C# bridge allocates and notifies a modeled world instance, but it does not spawn instance contents, run instance-specific handler factories, or model event spawns.
- Difficulty id remains `0`; C# `AutoGroupSummary` does not yet carry Java `AutoGroupType.getDifficultId()`.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- Ready-enter expiry, quick-entry refill, penalties, and full lifecycle remain missing.

## Validation Decision

- Changed surface: live connection dispatch, ready-match apply materialization, autogroup runtime state, and shared world instance runtime state.
- Specific behavior/contract: a live ready match allocates a modeled world-map instance id before window `4`, registers that id in autogroup runtime state, notifies instance creation, and preserves prior press-enter behavior.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~WorldMapRuntimeStateTests" --no-restore
```

Result: passed 79, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this production `createNewInstance(...)` bridge; Java source review supplied the allocation, `onInstanceCreate`, and ready-window ordering.
- Broad-validation trigger: live connection dispatch and shared world/instance runtime state changed.
- Broad .NET decision: skipped full project/solution validation. The focused command compiled the affected project and directly covered ready-match service materialization, runtime snapshots, connection window `100`/`102` flow, and world-map allocation/lifecycle behavior.
- Why this scope is sufficient: the UOW changed only autogroup ready-match materialization and existing world runtime APIs; packet primitives, persistence, scheduler, spawn engine internals, and static-data loaders were not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.createNewInstance(...)` | `AutoGroupLookingPartyRegistrationService.ApplyReadyMatchPlanAsync(...)` / `GameServerConnection.MaterializeAutoGroupReadyMatchRuntimeInstance(...)` | Service/Connection | Partial | Unit Tested | Partial Parity | Live ready matches now allocate a modeled world instance id before runtime registration/window `4`; Java spawn content, difficulty id, and full auto-instance object state remain partial. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(...)` | `InstanceRuntimeService.GetNextAvailableInstance(...)` / `WorldMapRuntimeStateTable` | Service/Runtime | Partial | Unit Tested | Partial Parity | Autogroup live path now consumes the existing C# allocator and notifies instance creation. Java `SpawnEngine.spawnInstance`, handler factory selection, and event spawns remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `AutoGroupInstanceRuntimeRegistration` / `AutoGroupInstanceRuntimeState` | Runtime Model | Partial | Unit Tested | Partial Parity | Runtime state now stores allocated instance id plus `StartInstanceTime`; Java instance reference and many handler callbacks remain partial. |
| `com.aionemu.gameserver.model.autogroup.AutoPvpInstance` | `AutoGroupInstanceKind.PvpRaceInstance` / ready-match runtime registration | Runtime Model | Partial | Unit Tested | Partial Parity | Matched player count is used as the allocated max-player count for ready periodic PvP matches. Race team formation and port-to-start-position remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP` | `GameServerConnection.HandleAutoGroupAsync(...)` | Packet Handler | Partial | Unit Tested | Partial Parity | Window `100` ready matches now materialize world instance state before window `4`; window `102` still reaches press-enter window `5`. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupReadyMatchSendsWindowFourAndRemovesQueuesLikeJava` | Unit | Java source review | Live ready match allocates world-map instance id `2`, stores max players from matched roster, notifies creation, sends window `4`, and still allows window `102` press-enter. | Focused C# connection/world runtime test. | Does not validate spawned instance content or teleport destination. |
| `AutoGroupLookingPartyRegistrationServiceTests.ApplyReadyMatchPlan_RemovesMatchedQueuesAndSendsCleanupBeforeReadyWindowLikeJava` | Unit | Java source review | Ready-match apply materializer can replace the synthetic intent with an allocated id and `StartInstanceTime` before runtime registration. | Focused C# service test. | Materializer itself is covered by connection test, not this pure service test. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.PressEnter_RemovesGroupAndKeepsRegisteredPlayerLikeJavaAutoGroupService` | Unit | Java source review | Runtime snapshots preserve ready-enter and start-instance timestamps through press-enter. | Focused C# runtime test. | Does not port `AutoPvpInstance.portToStartPosition`. |
| `WorldMapRuntimeStateTests` selected tests | Unit | Prior Java source review | Existing world-map allocation and lifecycle notification behavior remains covered after the autogroup bridge consumes it. | Adjacent focused world runtime tests. | Java spawn engine side effects remain outside this runtime table slice. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; ready-match allocation id parity improved, but full auto-instance lifecycle remains partial.

## Remaining Gaps

- Java `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` is not wired into this autogroup allocation bridge.
- Java `AutoGroupType.getDifficultId()` is not represented on `AutoGroupSummary`.
- Java `AutoPvpInstance.onPressEnter(...)` port-to-start-position remains missing.
- The 120-second `LookingForParty.isOnStartEnterTask()` expiry is not enforced yet.
- Penalty scheduling, delayed open-registration refresh, member-cleanup queue recheck, quick-entry refill, and full destroy lifecycle remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2384] Allocate autogroup ready match instance ids
```
