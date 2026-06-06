# Phase 6 Session 2746 Completion

## Unit of Work

[Phase 6][UOW-2746] Persist live legion announcement edits

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x09` now changes or clears the active legion announcement instead of remaining deferred.
- Java source of truth: `CM_LEGION.runImpl` case `0x09`, `LegionService.changeAnnouncement`, `LegionDAO.saveAnnouncement`, `LegionPermissionsMask.EDIT`, and the announcement-edit `SM_SYSTEM_MESSAGE` helpers.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, player announcement runtime state, `IPlayerEnterWorldRepository.SaveLegionAnnouncementAsync`, `MySqlPlayerEnterWorldRepository`, `EmptyPlayerEnterWorldRepository`, and `SmSystemMessage`.
- Client-visible/state/persistence effect changed: authorized players can set or clear announcement state, the existing `legion_announcement_list` schema is updated through delete/optional-insert persistence, and no-right/success/clear system packets are sent from live code.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client packet path, mutates runtime player/legion announcement state, persists via the existing database shape, and sends real server packets.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added `SaveLegionAnnouncementAsync` to `IPlayerEnterWorldRepository`.
- Implemented Java-style delete all existing rows plus optional insert in `MySqlPlayerEnterWorldRepository`.
- Added `SmSystemMessage` helpers for:
  - `STR_GUILD_WRITE_NOTICE_DONT_HAVE_RIGHT` (`1300276`)
  - `STR_GUILD_WRITE_NOTICE_DONE` (`1300277`)
  - `STR_MSG_CLEAR_GUILD_NOTICE` (`1390128`)
- Wired `CM_LEGION` exOpcode `0x09` to:
  - enforce Java EDIT permission mask `0x200`
  - truncate non-empty announcements to 256 chars
  - update player runtime announcement state
  - persist set/clear through `legion_announcement_list`
  - send Java-equivalent no-right/success/clear system messages

## Validation Decision

- Changed surface: live connection dispatch, runtime player announcement state, repository persistence mutation, and system-message helper surface.
- Specific behavior/contract: Java `LegionService.changeAnnouncement` checks EDIT permission, truncates messages longer than 256 chars, stores a timestamped announcement when non-empty, clears when empty, and sends no-right/success/clear system messages. Java `LegionDAO.saveAnnouncement` deletes current rows and inserts one row when the announcement is non-null.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema|FullyQualifiedName~SaveLegionAnnouncementAsync_ReplacesAndClearsAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit test exists for this packet/DAO path in the repo.
- Broad-validation trigger: live connection dispatch and repository persistence mutation.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project, directly exercise the changed live packet branch and repository contract, and no shared packet primitive or broad persistence helper was changed.
- Why this scope is sufficient: the focused tests cover parser, permission, runtime mutation, truncation, clear, system messages, repository call capture, and DB-gated delete/insert compilation; residual risk is online fanout and live MySQL execution.

## Validation Result

- Passed: 19
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings remain outside this UOW.
- DB-gated tests compiled but did not execute against live MySQL because `AION_GAMESERVER_DB_INTEGRATION=1` was not set.
- `git diff --check` passed with normal CRLF warnings only.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_ChangeAnnouncementReadsJavaMessage` | Unit | `CM_LEGION.readImpl` case `0x09` | Parser reads empty `D` and announcement text. | Java source review plus parser assertion. | Does not execute socket parser. |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementWithoutEditRightSendsNoRightLikeJava` | Unit | `LegionService.changeAnnouncement` | Missing EDIT permission sends id `1300276` and does not persist/mutate. | Java source review plus live packet observer. | Permission model is based on loaded player rank/permission snapshot. |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementPersistsRuntimeStateAndSuccessLikeJava` | Unit | `LegionService.changeAnnouncement` | Authorized set updates runtime state, calls repository, and sends id `1300277`. | Java source review plus live packet observer and repository capture. | Does not fan out to online legion members. |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementTruncatesLongMessageLikeJava` | Unit | `LegionService.changeAnnouncement` | Long announcement truncates to 256 chars before persistence. | Java source review plus runtime/repository assertions. | Does not assert warning log. |
| `HandleInfrastructurePacketAsync_ClearAnnouncementPersistsNullAndSendsClearLikeJava` | Unit | `LegionService.changeAnnouncement` | Empty announcement clears runtime state, persists null, and sends id `1390128`. | Java source review plus live packet observer and repository capture. | Does not fan out `SM_LEGION_INFO`. |
| `SaveLegionAnnouncementAsync_ReplacesAndClearsAgainstJavaSchema_WhenEnabled` | DB-gated Integration | `LegionDAO.saveAnnouncement` | Delete-and-insert, then delete-only clear behavior against `legion_announcement_list`. | Java source review plus DB-gated test compiled. | Not run against live MySQL in this session. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcodes `0x07`, `0x08`, and `0x09` are live; other mutation/service subactions remain deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Change/clear announcement core behavior is live. Online legion member broadcast/fanout is not implemented. |
| `com.aionemu.gameserver.dao.LegionDAO` | `IPlayerEnterWorldRepository.SaveLegionAnnouncementAsync` / `MySqlPlayerEnterWorldRepository.SaveLegionAnnouncementAsync` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Delete/optional-insert shape ported; live MySQL execution not run this session. |
| `com.aionemu.gameserver.model.team.legion.LegionPermissionsMask` | `GameServerConnection.LegionEditPermission` and `HasLegionWarehouseRight` | Enum/Permission Logic | Partial | Unit Tested | Partial Parity | EDIT mask `0x200` enforced through existing player rank permission snapshot. Broader enum is not fully modeled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added no-right/success/clear helpers; broader system-message surface is not claimed verified. |

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported in this UOW: 5 partial runtime artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still lacks Java's shared `Legion` aggregate; announcement state is updated on the active loaded player snapshot.
- Java broadcasts `SM_LEGION_EDIT(announcement)` or `SM_LEGION_INFO` to other online legion members; C# fanout remains missing.
- DB-gated persistence test was not executed against live MySQL in this session.
- Java logs a warning when truncating overlong announcements; C# truncates but does not yet log that warning.
- Timestamp timezone behavior remains Needs Verification.
- No real client validation was performed.
