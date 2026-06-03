# Phase 6 Session 2372 Completion - Send Autogroup Successful Registration Fanout

## Scope

Ported the Java successful-registration packet fanout for `CM_AUTO_GROUP` window `100` start-looking requests.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- `AutoGroupService.startLooking` calls `AutoGroupUtility.sendSuccessfulRegistration(lfp, player.getName(), agt, maskId)` immediately after adding a queued `LookingForParty`.
- `sendSuccessfulRegistration` iterates queued member object ids and sends only to online players.
- For periodic instances, Java first sends `new SM_AUTO_GROUP(maskId, 6, true)`.
- Java then sends `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_REGISTER_SUCCESS()` id `1400194`.
- Java then sends `new SM_AUTO_GROUP(maskId, 1, lfp.getEntryRequestType().getId(), leaderName)`.
- `AutoGroupType.isPeriodicInstance()` is true for masks `1`, `2`, `3`, `107`, `108`, `109`, and `111`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupSummary.IsPeriodicInstance` for the Java periodic mask set.
- Added `SmSystemMessage.InstanceRegisterSuccess()` for Java message id `1400194`.
- Wired successful window `100` registration dispatch to send Java fanout packets to queued member object ids:
  - periodic close-icon `SmAutoGroup` window `6` with `close=true`,
  - success `SmSystemMessage`,
  - waiting-window `SmAutoGroup` window `1` with request type and leader name.
- Added focused packet-shape coverage for `SmAutoGroup` window `1` and the success system message.
- Added a live packet-ingress test for solo successful registration fanout.

Known limitations:

- Multi-member fanout uses the connection registry path but was not covered by a separate multi-member registry test in this UOW.
- Java queue matching, instance creation, battleground announcement, and quick-entry refill remain missing.

## Validation Decision

- Changed surface: live connection dispatch, autogroup static metadata, `SM_AUTO_GROUP` waiting-window packet shape, and `SM_SYSTEM_MESSAGE` helper.
- Specific behavior/contract: a successful `CM_AUTO_GROUP` window `100` registration should send Java's success fanout packet sequence: optional periodic close icon window `6`, success message id `1400194`, then waiting window `1` with request type and leader name.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmAutoGroup|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 23, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupUtility.sendSuccessfulRegistration`; Java source review identified the packet order and message ids.
- Broad-validation trigger: live connection dispatch and packet helpers were touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered live dispatch plus adjacent packet shapes. No shared packet primitive, persistence repository, scheduler primitive, serialization helper, data loader, or common model/state changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendSuccessfulRegistration(...)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendAutoGroupSuccessfulRegistrationAsync(...)` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Packet order is modeled for successful registration. Multi-member registry fanout is implemented but not separately tested; online/offline behavior remains partial. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType.isPeriodicInstance()` | `Aion.GameServer.Dataholders.AutoGroupSummary.IsPeriodicInstance` | Model/Metadata | Partial | Unit Tested indirectly | Partial Parity | Java periodic mask set `{1,2,3,107,108,109,111}` is modeled for fanout. Other `AutoGroupType` methods remain partial/missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `6` support is reused; window `1` waiting payload with request type and leader name is now covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added `InstanceRegisterSuccess()` id `1400194`. Not a Java-generated golden packet. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupSuccessfulRegistrationSendsJavaFanoutPackets` | Unit | Java source review | Solo successful window `100` live ingress sends periodic close-icon, success message, and waiting-window packets in Java order. | Focused C# ingress test. | Does not cover multi-member registry fanout. |
| `GamePacketTests.SmAutoGroup_WritesJavaWaitingWindowPayload` | Unit | Java source review | `SM_AUTO_GROUP` window `1` writes zero progression fields, request type id, trailing zero, and leader name. | Focused packet test. | Not a Java-generated golden packet. |
| Existing `GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload` | Unit | Java source review | Periodic window `6` close flag maps `close=true` to Java flag `0`. | Focused packet test. | Not a Java-generated golden packet. |
| Existing `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Unit | Java source review | Success message helper emits id `1400194`. | Focused packet helper test. | Not a Java-generated golden packet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Multi-member autogroup success fanout should get a dedicated registry-path test.
- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` remain partial/missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2372] Send autogroup successful registration fanout
```
