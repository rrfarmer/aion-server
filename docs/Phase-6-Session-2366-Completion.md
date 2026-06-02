# Phase 6 Session 2366 Completion - Wire Autogroup Press-Enter Runtime Slice

## Scope

Wired a conservative `CM_AUTO_GROUP` window `102` press-enter slice into the existing C# autogroup runtime owner.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstanceHandler.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` calls `AutoGroupService.pressEnter(player, instanceMaskId)` for window `102`.
- `pressEnter` searches existing auto instances by registered player id and autogroup mask id; missing matches return without side effects.
- A matching player is removed from group/alliance membership before entering.
- `AutoInstance.onPressEnter` calculates and stores the instance entrance cooldown for the auto instance map id.
- Java sends `SM_AUTO_GROUP(instanceMaskId, 5)` after the press-enter side effects.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `InstanceMaskId` to C# `AutoGroupInstanceRuntimeRegistration` and snapshots so the runtime can model Java `getAutoInstance(player, maskId)`.
- Added `AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` to find a registered player by mask id, remove group/alliance membership, and return the Java-backed press-enter intent.
- Wired `GameServerConnection.HandleAutoGroupAsync(...)` window `102` to:
  - no-op when no matching registered auto instance exists,
  - apply the existing instance entrance cooldown path for the matched instance map,
  - send `SmAutoGroup(autoGroup, windowId: 5)` when static autogroup data is available.
- Added focused tests for matching and missing press-enter runtime behavior.

Known limitations:

- C# still does not create/match live `AutoInstance` entries from queue matching, so this path only works when runtime registrations already exist.
- Java `AutoInstance.onPressEnter` subclass-specific behavior is not modeled beyond the base cooldown behavior.
- The live connection dispatch is compiled by the focused test command, but no direct packet-ingress test was added for `CM_AUTO_GROUP` window `102`.
- `CM_AUTO_GROUP` windows `103` through `105` remain deferred.

## Validation Decision

- Changed surface: live connection dispatch plus autogroup instance runtime state and cooldown/packet intent.
- Specific behavior/contract: `CM_AUTO_GROUP` window `102` should find the Java-equivalent registered auto instance by player and mask id, remove team membership, apply base press-enter cooldown behavior, and send window `5`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Result: passed 10, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.pressEnter`; Java source review identified the matching/no-op, team removal, cooldown, and packet behavior.
- Broad-validation trigger: live connection dispatch and cooldown application were touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered the runtime matching/team-removal behavior plus existing `SmAutoGroup` packet serialization. No shared packet primitive, scheduler, persistence repository, serialization helper, or data loader changed.
- Why this scope is sufficient: the implemented runtime branch is directly asserted, and the dispatch/cooldown callsites are compile-covered. Remaining live auto-instance creation and subclass behavior are explicitly not implemented.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `102` now dispatches to press-enter runtime behavior. Windows `103`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.pressEnter(...)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` plus `GameServerConnection.HandleAutoGroupAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Registered-player/mask lookup, missing no-op, group/alliance removal, base cooldown dispatch, and window `5` packet dispatch are modeled. Live auto-instance creation/matching is still missing. |
| `com.aionemu.gameserver.services.AutoGroupService.getAutoInstance(...)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.PressEnter(...)` | Runtime Lookup | Partial | Unit Tested | Partial Parity | C# now tracks `InstanceMaskId` and registered player ids to model Java lookup. C# runtime registration is not yet created by queue matching. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance.onPressEnter(...)` | `Aion.GameServer.Services.InstanceEntranceCooldownService.ApplyEntranceCooldown(...)` | Service | Partial | Unit Tested indirectly | Partial Parity | Base Java cooldown behavior is invoked through the existing connection helper. Subclass-specific press-enter behavior, if any, remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `5` serialization support is reused. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupInstanceLeaveRuntimeServiceTests.PressEnter_RemovesGroupAndKeepsRegisteredPlayerLikeJavaAutoGroupService` | Unit | Java source review | Matching player/mask removes group membership and keeps registered autogroup state for entry. | Focused runtime state test. | Does not execute live packet ingress or teleport entry. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.PressEnter_MissingMaskOrUnregisteredPlayerIsNoOpLikeJavaGetAutoInstanceNull` | Unit | Java source review | Missing mask or unregistered player returns no-op without team or registry mutation. | Focused no-op state test. | None for this branch. |
| Existing `GamePacketTests.SmAutoGroup*` selected by focused filter | Unit | Java source review | `SM_AUTO_GROUP` window serialization remains compile/test covered for current packet implementation. | Focused packet tests. | Does not prove every window against golden Java output. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` windows `103` through `105` remain deferred.
- Live queue matching and auto-instance creation remain missing.
- Java autogroup penalties, cancel-enter removal scheduling, rematch checks, and quick-entry refill remain missing.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config behavior remains missing in C#.
- Java success registration packet fanout and full instance entry/teleport behavior remain missing.
- Java periodic registration cron callbacks and real scheduled close task handles remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2366] Wire autogroup press enter runtime
```
