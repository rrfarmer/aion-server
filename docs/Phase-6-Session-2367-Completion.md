# Phase 6 Session 2367 Completion - Wire Autogroup Cancel-Enter Runtime Slice

## Scope

Wired a conservative `CM_AUTO_GROUP` window `103` cancel-enter slice into the existing C# autogroup runtime owner.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvPFFAInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoHarmonyInstance.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` calls `AutoGroupService.cancelEnter(player, instanceMaskId)` for window `103`.
- `cancelEnter` searches existing auto instances by registered player id and autogroup mask id; missing matches return without side effects.
- A matching player is removed from `AutoInstance.registeredAGPlayers`.
- Java then schedules player penalty/removal, runs `destroyOrAddPlayersFromQuickEntries(autoInstance)`, and sends `SM_AUTO_GROUP(instanceMaskId, 2)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)` to model Java registered-player removal by player/mask lookup.
- Wired `GameServerConnection.HandleAutoGroupAsync(...)` window `103` to:
  - no-op when no matching registered auto instance exists,
  - unregister the player from the autogroup runtime state,
  - send `SmAutoGroup(autoGroup, windowId: 2)` when static autogroup data is available.
- Added focused tests for cancel-enter unregister and missing/no-op behavior.

Known limitations:

- Java `penalisePlayerAndScheduleRemoval(objectId)` remains missing.
- Java `destroyOrAddPlayersFromQuickEntries(autoInstance)` remains missing; C# does not yet know enough about players currently inside the auto instance or quick-entry queue refill to safely destroy/refill here.
- The live connection dispatch is compiled by the focused test command, but no direct packet-ingress test was added for `CM_AUTO_GROUP` window `103`.
- `CM_AUTO_GROUP` windows `104` and `105` remain deferred.

## Validation Decision

- Changed surface: live connection dispatch plus autogroup instance runtime registration state and cancel-window packet intent.
- Specific behavior/contract: `CM_AUTO_GROUP` window `103` should find the Java-equivalent registered auto instance by player and mask id, unregister the player, and send window `2`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.cancelEnter`; Java source review identified matching/no-op, unregister, penalty/refill, and window `2` packet behavior.
- Broad-validation trigger: live connection dispatch was touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered runtime unregister behavior plus existing `SmAutoGroup` packet serialization. No shared packet primitive, scheduler, persistence repository, serialization helper, or data loader changed.
- Why this scope is sufficient: the implemented unregister/no-op runtime branch is directly asserted, and unimplemented Java penalty/refill/destroy behavior is explicitly documented as a remaining gap.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `103` now dispatches to cancel-enter runtime behavior. Windows `104`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.cancelEnter(...)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)` plus `GameServerConnection.HandleAutoGroupAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Registered-player/mask lookup, missing no-op, unregister, and window `2` packet dispatch are modeled. Penalty scheduling and quick-entry/destroy handling remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance.unregister(...)` | `Aion.GameServer.Services.AutoGroupInstanceRuntimeState.Unregister(...)` | Runtime State | Partial | Unit Tested | Partial Parity | C# removes the player object id from registered runtime state. Java subclass and lifecycle side effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `2` serialization support is reused. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeaveRuntimeServiceTests.CancelEnter_UnregistersPlayerLikeJavaAutoGroupService` | Unit | Java source review | Matching player/mask removes the player from registered autogroup runtime state. | Focused runtime state test. | Does not execute penalty scheduling, quick-entry refill, destroy check, or live packet ingress. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.CancelEnter_MissingMaskOrUnregisteredPlayerIsNoOpLikeJavaGetAutoInstanceNull` | Unit | Java source review | Missing mask or unregistered player returns no-op without team or registry mutation. | Focused no-op state test. | None for this branch. |
| Existing `GamePacketTests.SmAutoGroup*` selected by focused filter | Unit | Java source review | `SM_AUTO_GROUP` window serialization remains compile/test covered for current packet implementation. | Focused packet tests. | Does not prove every window against golden Java output. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` windows `104` and `105` remain deferred.
- Java penalty scheduling, delayed removal, quick-entry refill, and cancel-enter destroy checks remain missing.
- Live queue matching and auto-instance creation remain missing.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config behavior remains missing in C#.
- Java success registration packet fanout and full instance entry/teleport behavior remain missing.
- Java periodic registration cron callbacks and real scheduled close task handles remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2367] Wire autogroup cancel enter runtime
```
