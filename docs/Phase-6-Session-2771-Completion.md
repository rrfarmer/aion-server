# Phase 6 Session 2771 Completion

## Unit of Work

[Phase 6][UOW-2771] Consume legion invite denial responses

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_QUESTION_RESPONSE` packets for guild invite question id `80001` with denial response now execute instead of leaving the pending invite request untouched.
- Java source of truth: `CM_QUESTION_RESPONSE.runImpl`, `ResponseRequester.respond`, and `LegionService.invitePlayerToLegion` anonymous `RequestResponseHandler.denyRequest`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleQuestionResponseAsync`, new `HandleLegionInviteDenyResponseAsync`, target `ResponseRequester.Respond`, `PendingLegionInviteRequest`, and `SmSystemMessage.GuildInviteHeRejectedInvitation`.
- Client-visible/state effect changed: denying a live legion invite now consumes the target's pending response request, clears the target pending invite payload, and sends the online inviter Java message `STR_GUILD_INVITE_HE_REJECTED_INVITATION(responder.getName())`.
- Why this is not preview-only/test-only/documentation-only: it consumes live pending request state from a real client question response and sends a real server packet to the inviter from live handler code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Routed guild invite question id `80001` denial responses into a dedicated live handler.
- Consumed `QuestionResponseRequestKind.LegionInvite` through `ResponseRequester.Respond`, preserving Java's remove-before-callback semantics.
- Cleared `Player.PendingLegionInviteRequest` after a matching denial dispatch.
- Added the Java system-message helper for `STR_GUILD_INVITE_HE_REJECTED_INVITATION`.
- Sent the inviter the denial notification through the live connection registry.

## Validation Decision

- Changed surface: live question-response dispatch, response-request state consumption, pending player state, direct registry packet send, and system-message helper.
- Specific behavior/contract: Java denial invokes `RequestResponseHandler.denyRequest`, which sends `STR_GUILD_INVITE_HE_REJECTED_INVITATION(responder.getName())` to the requester after `ResponseRequester.respond` consumes the request.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.invitePlayerToLegion` invite response handling.
- Broad-validation trigger: live question-response dispatch and request-state mutation were touched.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, request-state consumption, stale-request branch, and notification packet.
- Why this scope is sufficient: the change is isolated to guild invite denial response handling and the existing legion invite tests exercise both request setup and response consumption.

## Validation Result

- Focused C# result: Passed, 83 total, 0 failed, 0 skipped.
- First focused run failed because the test registry omitted the inviter; the fixture was corrected and the same focused command passed.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteDenyConsumesRequestAndNotifiesInviterLikeJava` | Unit | `LegionService.invitePlayerToLegion` deny handler | Denial consumes target request state, clears pending invite payload, sends no local responder packet, and notifies the online inviter with message id `1300259` and responder name. | Live handler, request state, and direct packet assertions. | Uses test registry, not a real client. |
| `HandleQuestionResponseAsync_LegionInviteDenyWithoutPendingRequestIsSideEffectFreeLikeJava` | Unit | `ResponseRequester.respond` missing request behavior | Missing response-request dispatch leaves unrelated pending payload and sends no packets. | Live handler side-effect assertion. | Does not model Java logging because none is client-visible here. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE` guild invite denial | `GameServerConnection.HandleQuestionResponseAsync` / `HandleLegionInviteDenyResponseAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Denial for question id `80001` consumes matching request state and notifies the inviter. Acceptance remains unwired. |
| `com.aionemu.gameserver.services.LegionService.invitePlayerToLegion` deny handler | `HandleLegionInviteDenyResponseAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Denial callback behavior is covered. Accept callback `addToLegion` remains pending. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond` | Runtime State | Partial | Unit Tested | Partial Parity | Matching guild invite denial removes the request before side effects; missing request is side-effect free. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_REJECTED_INVITATION` | `SmSystemMessage.GuildInviteHeRejectedInvitation` | Server Packet | Complete for current helper | Unit Tested | Partial Parity | Message id and parameter are asserted through live response handling. No Java golden packet fixture. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.saveNewLegionMember` | no C# insert-member repository method yet | Repository | Not Started | No Tests | Unknown | Acceptance requires inserting `legion_members(legion_id, player_id, rank)` before live add-to-legion can be wired honestly. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 4 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked/not-started artifacts: 1 (`LegionMemberDAO.saveNewLegionMember` equivalent)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Guild invite acceptance is not wired; accepting the invite still does not add the responder to the inviter's legion.
- C# has no existing repository method equivalent to Java `LegionMemberDAO.saveNewLegionMember`, which inserts into `legion_members(legion_id, player_id, rank)`.
- Java `LegionService.addToLegion` also displays the current legion announcement and records join history; these must be included when acceptance is wired.
- No Java golden fixture was generated for the rejected-invitation system message packet.
