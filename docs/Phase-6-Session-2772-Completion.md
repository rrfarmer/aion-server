# Phase 6 Session 2772 Completion

## Unit of Work

[Phase 6][UOW-2772] Apply legion invite acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_QUESTION_RESPONSE` packets for guild invite question id `80001` with nonzero accept responses now execute the legion join path instead of stopping after invite setup/denial support.
- Java source of truth: `LegionService.invitePlayerToLegion` accept handler, `LegionService.addToLegion`, `LegionService.addLegionMember`, `LegionMemberDAO.saveNewLegionMember`, `LegionService.displayLegionAnnouncement`, and `LegionService.addHistory`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleQuestionResponseAsync`, `AcceptLegionInviteAsync`, `PendingLegionInviteRequest`, `IPlayerEnterWorldRepository.SaveNewLegionMemberAsync`, live responder legion fields, `SmLegionInfo`, `SmSystemMessage.GuildNotice`, `SmLegionUpdateMember`, `SmLegionEdit`, `SmLegionUpdateTitle`, and legion history insertion.
- Client-visible/state/persistence effect changed: accepting a pending guild invite now consumes the target's response request, persists the responder in `legion_members`, mutates the responder's live legion state/rank/announcement/emblem data, records `JOIN` history, sends legion info and announcement packets to the responder, broadcasts existing legion update/edit packets to online legion members, and broadcasts the responder's visible legion title update.
- Why this is not preview-only/test-only/documentation-only: it persists a new runtime membership row, mutates live player/legion state, records runtime history, and sends real server packets from the live question-response handler.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/model/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Routed all guild invite question id `80001` responses through a single live handler, preserving denial behavior and adding acceptance behavior.
- Added `SaveNewLegionMemberAsync` to the player-enter-world repository abstraction and MySQL implementation using the Java DAO table shape: `legion_members(legion_id, player_id, rank)`.
- Applied the Java default invite rank `VOLUNTEER`; this corrects the previous handoff's `LEGIONARY` guess.
- Bound accepted responders to the inviter's live legion metadata after successful persistence only.
- Sent Java-aligned immediate response effects: `SM_LEGION_INFO`, optional guild notice, `JOIN` history, legion member update fanout using existing C# packet support, announcement refresh, and visible title update.
- Added `LegionHistoryActions.Join` so C# history insertion uses the Java action name consistently.

## Validation Decision

- Changed surface: live question-response dispatch, repository persistence, player legion state mutation, legion history insertion, direct legion packet fanout, and visible title broadcast.
- Specific behavior/contract: Java acceptance removes the response request, persists a new legion member, binds the invited player into the legion with rank `VOLUNTEER`, displays the current announcement, records `JOIN` history, and sends legion update/title/info effects.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.addToLegion` or `LegionMemberDAO.saveNewLegionMember`.
- Broad-validation trigger: live packet dispatch, runtime state mutation, repository shape, and packet fanout were touched.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised invite setup, denial retention, acceptance success, failed persistence guard, packet serialization helpers, and repository fake contract.
- Why this scope is sufficient: the change is isolated to guild invite question-response handling and the new insert-member repository seam; the tests assert persistence, state, history, and packet contracts through the live handler.

## Validation Result

- Focused C# result: Passed, 122 total, 0 failed, 0 skipped.
- First focused run failed because the `SM_LEGION_INFO` assertion helper did not consume the Java/C# empty announcement terminator after a non-empty announcement; the helper was corrected and the same focused command passed.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.invitePlayerToLegion` accept handler and `LegionService.addToLegion` | Acceptance consumes request state, inserts member rank `VOLUNTEER`, mutates target legion fields, inserts `JOIN` history, sends `SM_LEGION_INFO` plus notice, fans out existing member/edit packets, and broadcasts visible title. | Live handler, fake repository, packet serialization, direct registry packets, and visible broadcast assertions. | Uses existing C# `SM_LEGION_UPDATE_MEMBER` support because `SM_LEGION_ADD_MEMBER` is not ported yet. |
| `HandleQuestionResponseAsync_LegionInviteAcceptFailedInsertDoesNotMutateLikeJavaPersistenceGuard` | Unit | `LegionMemberDAO.saveNewLegionMember` failure guard in `Legion.addLegionMember` path | Failed insert consumes the answered request but does not mutate the responder, add history, or send packets. | Live handler and repository failure assertion. | Does not model full Java member-limit failure messaging because C# lacks the aggregate legion member limit path. |
| `AssertLegionInfoAnnouncementPacket` | Test helper | `SM_LEGION_INFO.writeImpl` announcement serialization | Non-empty announcement assertions consume the trailing empty string terminator. | Packet reader reaches end of packet. | Helper-only adjustment. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `CM_QUESTION_RESPONSE` guild invite acceptance | `GameServerConnection.HandleQuestionResponseAsync` / `HandleLegionInviteResponseAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Question id `80001` accept now consumes request state and enters live add-to-legion effects. |
| `LegionService.addToLegion` | `AcceptLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Persists, binds responder state, displays announcement, records history, and sends available packets. Missing full aggregate member-limit behavior and exact add-member fanout. |
| `LegionService.addLegionMember(legion, player)` default rank | `BindLegionInviteAcceptedPlayer` | Runtime State | Partial | Unit Tested | Partial Parity | Uses Java default rank `VOLUNTEER`. No full legion member list object is updated because C# currently stores legion data on player state. |
| `LegionMemberDAO.saveNewLegionMember` | `IPlayerEnterWorldRepository.SaveNewLegionMemberAsync` / `MySqlPlayerEnterWorldRepository.SaveNewLegionMemberAsync` | Repository | Partial | Unit Tested through fake/live handler | Partial Parity | Inserts `legion_id`, `player_id`, and `rank`; no DB integration test was run. |
| `LegionHistoryAction.JOIN` | `LegionHistoryActions.Join` | Runtime Persistence Metadata | Complete for action mapping | Unit Tested through acceptance | Partial Parity | Action name and id mapping are wired for join history insertion. |
| `SM_LEGION_INFO` plus guild notice | `SmLegionInfo.FromPlayer` / `SmSystemMessage.GuildNotice` | Server Packet | Partial | Unit Tested | Partial Parity | Responder receives info and announcement notice after live acceptance. No Java golden fixture. |
| `SM_LEGION_ADD_MEMBER` Java fanout | Existing `SmLegionUpdateMember` fanout | Server Packet | Not Ported | No Direct Test | Known Gap | C# uses existing update-member packet as a temporary live fanout; exact Java add-member packet remains a runtime packet UOW. |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported or advanced in this UOW: 6 runtime/repository/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 6
- Total blocked/not-started artifacts: 1 exact packet fanout (`SM_LEGION_ADD_MEMBER`)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still lacks Java `SM_LEGION_ADD_MEMBER`; accepted members currently trigger the existing `SmLegionUpdateMember` fanout.
- C# still lacks a full Java-like `Legion` aggregate/member-list update and member-limit check for the acceptance path.
- C# does not yet send Java's emblem update fanout during invite acceptance.
- No DB integration test verified the MySQL insert against a real schema.
- No Java golden packet fixture was generated for the acceptance packet sequence.
