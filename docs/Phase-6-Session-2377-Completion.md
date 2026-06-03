# Phase 6 Session 2377 Completion - Port Autogroup Battleground Registration Announcements

## Scope

Ported the battleground registration announcement branch from `AutoGroupService.startLooking`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/AutoGroupConfig.java`
- `game-server/src/com/aionemu/gameserver/model/Race.java`
- `game-server/src/com/aionemu/gameserver/model/templates/L10n.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/model/ChatType.java`

Java behavior used:

- After `AutoGroupUtility.sendSuccessfulRegistration(...)`, Java optionally broadcasts a manual `SM_MESSAGE`.
- The branch requires `AutoGroupConfig.ANNOUNCE_BATTLEGROUND_REGISTRATIONS`, a periodic autogroup, `EntryRequestType.GROUP_ENTRY`, and exactly one queued party for the registering player's race after the new entry is added.
- The message is `race.getL10n() + " have registered for " + agt.getL10n() + "."`.
- The packet uses `ChatType.BRIGHT_YELLOW_CENTER` id `36`.
- Recipients are world players whose race differs from the registering race and whose level is inside the autogroup range.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `GameServerAutoGroupOptions.AnnounceBattlegroundRegistrations`, defaulting to Java's `false`.
- Added race tracking to queued `AutoGroupLookingPartyRegistration` records.
- Added `AutoGroupBattlegroundRegistrationAnnouncement` as a service-level broadcast plan.
- Added Java-shaped message construction with `ChatUtil.L10n(900240/900241)` and the autogroup name id token.
- Updated live `CM_AUTO_GROUP` window `100` handling to broadcast the planned `SmMessage` after successful-registration fanout.
- Added focused service tests for first same-race periodic group-entry announcement planning and same-race suppression.
- Added a live ingress test for message text, chat type `36`, post-success ordering, and Java recipient filtering.

Known limitations:

- This UOW ports only the announcement branch after successful registration. Queue matching, open quick-entry checks, and live auto-instance creation remain missing.
- Broadcast delivery requires an available C# connection registry. With no registry, the registration success fanout still behaves as before, but the world broadcast cannot be sent.

## Validation Decision

- Changed surface: autogroup start-looking service result planning, autogroup options, live `CM_AUTO_GROUP` window `100` dispatch, and autogroup connection test registry.
- Specific behavior/contract: first same-race periodic group registration with announcements enabled should broadcast one `SmMessage` with chat type `36` to opposite-race in-range players after successful-registration fanout.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 34, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. Java source review identified the exact branch condition, message construction, chat type, recipient predicate, and ordering.
- Broad-validation trigger: live connection dispatch changed for `CM_AUTO_GROUP` window `100`.
- Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered service behavior plus live ingress dispatch.
- Why this scope is sufficient: edited behavior is local to successful autogroup registration announcement planning and broadcast dispatch; focused tests assert queue gate, message token construction, chat type, recipient filter, and direct fanout ordering.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Service/Dispatch | Partial | Unit Tested | Partial Parity | Battleground registration announcement branch is covered. Queue matching and auto-instance creation remain missing. |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` | `Aion.GameServer.Configuration.GameServerAutoGroupOptions` | Config | Partial | Unit Tested | Partial Parity | Added `AnnounceBattlegroundRegistrations` with Java default `false`. Schedule/period/start-time settings remain partial. |
| `com.aionemu.gameserver.model.Race` | `AutoGroupLookingPartyRegistrationService.GetRaceL10n(...)` | Metadata Helper | Partial | Unit Tested | Partial Parity | ELYOS and ASMODIANS client-string ids are used for announcement text. Full Race enum is not ported here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Packet | Partial | Unit Tested | Partial Parity | Existing manual constructor supports sender id `0`, null sender name, and chat type `36`; focused live test serializes and reads it. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld(...)` | `IGameClientConnectionRegistry.BroadcastToWorldAsync(...)` | Dispatch | Partial | Unit Tested | Partial Parity | Live test validates opposite-race and level-range filtering. Registry-less delivery remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_GroupEntryPlansBattlegroundAnnouncementForFirstSameRacePeriodicRegistrationLikeJava` | Unit | Java source review | Enabled announcement config, periodic mask, group entry, first same-race queue entry, message text, and recipient predicate. | Focused C# service test. | Does not exercise live socket dispatch. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_SuppressesBattlegroundAnnouncementAfterFirstSameRaceRegistrationLikeJava` | Unit | Java source review | Second same-race queued party does not produce an announcement. | Focused C# service test. | Opposite-race first registration is covered through predicate/message test only. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupGroupEntryBroadcastsBattlegroundAnnouncementAfterSuccessLikeJava` | Unit | Java source review | Live `CM_AUTO_GROUP` window `100` sends success fanout first, then broadcasts chat type `36` only to opposite-race in-range recipients. | Focused live ingress test with registry-backed online players. | No full world/socket integration test. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup registration announcement parity advanced, but lifecycle parity remains partial.

## Remaining Gaps

- C# queue matching and live auto-instance creation remain missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` and `checkQueueForNewMatches(maskId)` branches remain missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond enable/announce remain partial or missing.
- Broader PvP arena time availability and solo/FFA/glory item checks remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2377] Port autogroup battleground announcements
```
