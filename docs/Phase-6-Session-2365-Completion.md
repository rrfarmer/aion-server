# Phase 6 Session 2365 Completion - Wire Autogroup Cancel Queue Registration

## Scope

Wired the narrow `CM_AUTO_GROUP` window `101` cancellation path into the C# autogroup looking-party queue.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` calls `AutoGroupService.cancelRegistration(player, instanceMaskId)` for window `101`.
- Missing search entries return without side effects.
- If the canceling player is the `LookingForParty` leader, Java removes the whole party from the mask queue and sends `SM_AUTO_GROUP(maskId, 2)` to each online member.
- If the canceling player is a non-leader member, Java unregisters only that member, sends `SM_AUTO_GROUP(maskId, 2)` only to that player, then checks for new matches.
- Java penalty scheduling and queue rematch behavior are not ported in this UOW.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added leader tracking to `AutoGroupLookingPartyRegistration`.
- Added `AutoGroupLookingPartyRegistrationService.CancelRegistrationAsync(...)`.
- Modeled Java leader cancellation by removing the whole queued party and sending cancel-window packets to all queued members that are online through `IGameClientConnectionRegistry`.
- Modeled Java member cancellation by removing only the canceling member and sending the cancel-window packet only to that player.
- Preserved Java missing-entry no-op behavior.
- Wired `GameServerConnection.HandleAutoGroupAsync(...)` window `101` to the cancellation service.
- Added focused cancellation tests for leader, member, and missing-entry branches.

Known limitations:

- Java `AutoGroupUtility.penalisePlayer/penaliseParty` behavior remains missing.
- Java `checkQueueForNewMatches(maskId)` after member cancellation remains missing.
- `CM_AUTO_GROUP` windows `102` through `105` remain deferred.
- `AutoGroupConfig.AUTO_GROUP_ENABLE`, success registration fanout, queue matching, instance creation, and quick-entry refill remain missing.

## Validation Decision

- Changed surface: live connection dispatch plus production runtime queue state and packet fanout.
- Specific behavior/contract: `CM_AUTO_GROUP` window `101` should remove queued autogroup search state according to Java leader/member semantics and send `SM_AUTO_GROUP(maskId, 2)` cancel-window packets to Java-equivalent recipients.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

Result: passed 14, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.cancelRegistration`; Java source review identified the source-of-truth control flow and packet recipients.
- Broad-validation trigger: live connection dispatch was touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered the queue state and cancel packet contract. No shared packet primitive, scheduler, persistence, serialization helper, or data loader changed.
- Why this scope is sufficient: the implemented queue mutation and packet-recipient semantics are directly asserted. Remaining Java penalty/rematch behavior is explicitly unimplemented and documented as a gap.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `101` now dispatches to queue cancellation. Windows `102`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.cancelRegistration(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CancelRegistrationAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Missing search entry no-op, leader whole-party removal, member-only removal, and cancel-window packet recipients are modeled. Java penalties and rematch checks remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.isLeader(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration.LeaderObjectId` | Runtime State | Partial | Unit Tested | Partial Parity | C# stores the registration leader and uses it for cancel semantics. Java object/member lifecycle and online filtering are not fully modeled. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.unregisterMember(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CancelRegistrationAsync(...)` | Runtime State | Partial | Unit Tested | Partial Parity | Non-leader cancellation removes only the player's id and leaves the remaining party queued. Java post-removal rematch behavior remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `2` cancel-registration payload is reused and covered by packet test. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.CancelRegistration_LeaderRemovesWholePartyAndSendsCancelWindowLikeJava` | Unit | Java source review | Leader cancellation removes the whole party, clears queue state, and sends cancel-window packets to all party members. | Focused state and packet-recipient test. | Java penalty scheduling remains missing. |
| `AutoGroupLookingPartyRegistrationServiceTests.CancelRegistration_MemberRemovesOnlyMemberAndSendsCancelWindowLikeJava` | Unit | Java source review | Non-leader cancellation removes only the canceling member and sends the cancel-window packet only to that player. | Focused state and packet-recipient test. | Java rematch check remains missing. |
| `AutoGroupLookingPartyRegistrationServiceTests.CancelRegistration_MissingEntryIsNoOpLikeJavaNullSearchEntry` | Unit | Java source review | Missing search entries do not mutate queue state and do not send packets. | Focused no-op state test. | None for this branch. |
| `GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload` | Unit | Java source review | `SM_AUTO_GROUP` window `2` cancel-registration payload shape. | Focused packet serialization test. | Does not cover every `SM_AUTO_GROUP` window. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` windows `102` through `105` remain deferred.
- Java autogroup penalties and rematch checks remain missing.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config behavior remains missing in C#.
- Java `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` are not ported.
- Java success registration packet fanout, queue matching, instance creation, and quick-entry refill remain missing.
- Java periodic registration cron callbacks and real scheduled close task handles remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2365] Wire autogroup cancel queue registration
```
