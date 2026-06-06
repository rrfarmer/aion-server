# Phase 6 Session 2769 Completion

## Unit of Work

[Phase 6][UOW-2769] Broadcast legion announcement changes

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_LEGION` exOpcode `0x09` announcement edits now propagate announcement state and broadcast Java-equivalent legion packets instead of only updating the requester.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x09`, `LegionService.changeAnnouncement`, `LegionDAO.saveAnnouncement`, `SM_LEGION_EDIT(announcement)`, and `SM_LEGION_INFO`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAnnouncementChangeAsync`, active and online same-legion `Player.LegionAnnouncement`/`LegionAnnouncementEpochSeconds`, existing `SaveLegionAnnouncementAsync`, same-legion online fanout, `SmLegionEdit.Announcement`, and `SmLegionInfo.FromPlayer`.
- Client-visible/state/persistence effect changed: valid non-empty announcement edits now mutate active and online same-legion announcement state, persist through the existing repository, send requester done plus `SM_LEGION_EDIT(0x05)`, and broadcast the edit packet to same-legion bystanders. Clear edits now persist null, clear active and same-legion announcement state, send requester clear message, and send `SM_LEGION_INFO` to same-legion bystanders while excluding outsiders.
- Why this is not preview-only/test-only/documentation-only: it mutates live legion/player announcement state, persists through the existing repository shape, and sends real server packets from a live client handler.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`

## C# Runtime Changes

- Added Java-style non-empty announcement fanout using `SmLegionEdit.Announcement`.
- Added Java-style clear-announcement fanout using `SmLegionInfo.FromPlayer` for bystanders only.
- Propagated announcement text and epoch seconds to online same-legion `Player` instances.
- Preserved existing announcement persistence and requester system messages.
- Kept no-right rejection side-effect free, with no bystander mutation or broadcast.

## Validation Decision

- Changed surface: live client handler, online player announcement state, persistence call, and same-legion packet fanout.
- Specific behavior/contract: Java `LegionService.changeAnnouncement` checks edit rights, truncates long messages, saves the announcement, sends the requester system message, broadcasts `SM_LEGION_EDIT(0x05)` for non-empty announcements, and broadcasts `SM_LEGION_INFO` to non-active legion members when clearing.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.changeAnnouncement`, `SM_LEGION_EDIT`, or `SM_LEGION_INFO`.
- Broad-validation trigger: live state mutation, persistence, and connection fanout were touched.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, persistence call, announcement state propagation, fanout filtering, and packet serialization contracts. No shared packet primitive, crypto, scheduler, schema, or persistence abstraction changed.
- Why this scope is sufficient: the change is isolated to the existing `CM_LEGION 0x09` handler and existing `SM_LEGION_EDIT`/`SM_LEGION_INFO` serializers.

## Validation Result

- Focused C# result: Passed, 82 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementWithoutEditRightSendsNoRightLikeJava` | Unit | `LegionService.changeAnnouncement` edit-right guard | No-right edit sends the Java no-right message, preserves requester and bystander announcement state, avoids persistence, and emits no bystander packet. | Live handler side-effect assertion. | Uses test registry, not a real client. |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementPersistsStateAndBroadcastsLikeJava` | Unit | `LegionService.changeAnnouncement` non-empty branch | Non-empty edit mutates active and same-legion bystander state, persists, sends requester done then `SM_LEGION_EDIT(0x05)`, broadcasts edit to bystander, and excludes outsider. | Live handler, repository, state, and packet payload assertions. | C# approximates Java's shared `Legion` aggregate with online `Player` fields. |
| `HandleInfrastructurePacketAsync_ChangeAnnouncementTruncatesLongMessageLikeJava` | Unit | Java truncation branch in `LegionService.changeAnnouncement` | Long announcements are truncated to 256 chars and the edit packet uses the truncated value. | Live handler and packet payload assertion. | Does not verify log output. |
| `HandleInfrastructurePacketAsync_ClearAnnouncementPersistsNullAndBroadcastsInfoLikeJava` | Unit | `LegionService.changeAnnouncement` clear branch | Clear edit persists null, clears active and bystander state, sends requester clear message, broadcasts `SM_LEGION_INFO` to bystander, and excludes outsider. | Live handler, repository, state, and packet payload assertions. | Uses C# `SmLegionInfo.FromPlayer` approximation of Java `Legion` aggregate. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x09` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, edit-right rejection, truncation, persistence, active mutation, same-legion online propagation, requester packets, non-empty edit fanout, and clear info fanout are covered. No real-client verification. |
| `com.aionemu.gameserver.services.LegionService.changeAnnouncement` | `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Ports save, requester system messages, non-empty edit broadcast, and clear info broadcast. Java shared `Legion` announcement state is approximated by active and online same-legion `Player` announcement fields. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x05` | `SmLegionEdit.Announcement` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Packet writes edit type, announcement string, and Unix seconds like Java. No Java golden fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `SmLegionInfo.FromPlayer` | Server Packet | Partial | Unit Tested | Partial Parity | Clear branch uses existing C# player-backed legion info packet. Ranking and full Java `Legion` aggregate data remain approximated. |

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported or advanced in this UOW: 4 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked artifacts: 2 (no Java golden packet fixture for `SM_LEGION_EDIT` announcement or `SM_LEGION_INFO`)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- No Java golden packet fixture was generated for `SM_LEGION_EDIT` announcement or `SM_LEGION_INFO`.
- Full Java `Legion` aggregate announcement state is still approximated by active and online same-legion `Player` announcement fields.
- No real-client validation was performed for the announcement edit or clear broadcasts.
