# Phase 6 Session 2371 Completion - Send Autogroup Duplicate Registration Message

## Scope

Ported the Java duplicate-registration response for `CM_AUTO_GROUP` window `100` start-looking requests.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

Java behavior used:

- `AutoGroupService.startLooking` resolves the `AutoGroupType` before queue mutation.
- Under the mask-specific looking-party lock, if `getSearchEntry(player.getObjectId(), lfps)` returns an existing entry, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED(agt.getTemplate().getInstanceMapId())` to the player and returns.
- `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED(int worldId)` emits message id `1400181` with the instance map id parameter.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `SmSystemMessage.CantInstanceAlreadyRegistered(int worldId)` for Java message id `1400181`.
- Wired `GameServerConnection.HandleAutoGroupAsync(...)` window `100` to send the Java duplicate-registration system message when `AutoGroupLookingPartyRegistrationService.StartLooking(...)` returns `AlreadyRegistered`.
- Extended the autogroup connection fixture to inject a minimal static autogroup table and shared registration service.
- Added a live packet-ingress regression test that sends duplicate window `100` requests and verifies the Java message id/parameter while keeping one queued registration.
- Added adjacent packet-shape coverage for the new `SmSystemMessage` helper.

Known limitations:

- Java `AutoGroupUtility.sendSuccessfulRegistration(...)` fanout after first successful registration remains missing.
- C# queue matching and live auto-instance creation remain missing.
- No Java runtime fixture exists for this branch; source review identified the exact message id and parameter.

## Validation Decision

- Changed surface: live connection dispatch, `SM_SYSTEM_MESSAGE` helper, and focused test fixture support.
- Specific behavior/contract: duplicate `CM_AUTO_GROUP` window `100` start-looking requests should keep one queued registration and send `SM_SYSTEM_MESSAGE` id `1400181` with the autogroup instance map id parameter, matching Java's `STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `AutoGroupService.startLooking`; Java source review identified the duplicate branch and `SM_SYSTEM_MESSAGE` factory output.
- Broad-validation trigger: live connection dispatch and packet helper were touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered live dispatch, service status, and packet helper shape. No shared packet primitive, persistence repository, scheduler primitive, serialization helper, data loader, or common model/state changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` plus `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Service/Packet Dispatch | Partial | Unit Tested | Partial Parity | Duplicate queued registration now sends Java `STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED` from live dispatch. Successful-registration fanout, queue matching, announcements, and full lifecycle remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added `CantInstanceAlreadyRegistered(int)` id `1400181` with invariant world-id parameter. Not a Java-generated golden packet. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player, AionServerPacket)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendPacketAsync(...)` | Packet Utility/Dispatch | Partial | Unit Tested | Partial Parity | Duplicate autogroup branch sends directly to the active connection. Broader utility parity remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupDuplicateStartLookingSendsJavaAlreadyRegisteredMessage` | Unit | Java source review | Duplicate window `100` live ingress sends message id `1400181` with instance map id and leaves one queued registration. | Focused C# ingress test tied to Java duplicate branch. | Uses in-memory static data, not a Java runtime golden. |
| Existing `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_DuplicateMemberRegistrationIsNoOpLikeJavaAlreadyRegisteredBranch` | Unit | Java source review | Service returns `AlreadyRegistered` and does not add a duplicate queue entry. | Focused service test. | Packet sending happens in live dispatch. |
| Existing `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Unit | Java source review | `SmSystemMessage.CantInstanceAlreadyRegistered(int)` emits id `1400181` with world id parameter. | Packet helper test. | Not a Java-generated golden packet. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java `AutoGroupUtility.sendSuccessfulRegistration(...)` fanout remains missing.
- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2371] Send autogroup duplicate registration message
```
