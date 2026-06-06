# Phase 6 Session 2770 Completion

## Unit of Work

[Phase 6][UOW-2770] Wire live legion invite questions

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_LEGION` exOpcode `0x01` invite packets now dispatch instead of being ignored by the C# legion switch.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x01`, `LegionService.invitePlayerToLegion`, `LegionRestrictions.canInvitePlayer`, `ResponseRequester.putRequest`, `SM_QUESTION_WINDOW.STR_GUILD_INVITE_DO_YOU_ACCEPT_INVITATION`, and `SM_SYSTEM_MESSAGE` guild invite failures.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, new `HandleLegionInviteAsync`, `QuestionResponseRegistry`, `PendingLegionInviteRequest`, `SmQuestionWindow.GuildInviteDoYouAcceptInvitation`, and guild invite `SmSystemMessage` helpers.
- Client-visible/state effect changed: valid online invites now store a live pending response request on the target and send the target a real `SM_QUESTION_WINDOW` with legion name, legion level, and inviter name. Missing/offline, no-right, same-legion, other-legion, other-race, and busy-target cases now send Java-equivalent requester system messages without mutating target request state.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates live player response-request state, and sends real server packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Added `CM_LEGION` exOpcode `0x01` dispatch to the live legion handler.
- Added Java-equivalent invite restrictions for modeled state: online target lookup, inviter death, self invite, target legion membership, invite permission bit `0x8`, same-race requirement, and busy question slot.
- Added live target request registration using `ResponseRequester.PutRequest(80001, QuestionResponseRequestKind.LegionInvite, PendingLegionInviteRequest)`.
- Sent Java-equivalent requester messages and target `SM_QUESTION_WINDOW(80001, 0, 0, legionName, legionLevel, inviterName)`.
- Added missing guild invite question and system-message constants used by the live path.

## Validation Decision

- Changed surface: live client handler, response-request state, direct connection-registry packet delivery, and system/question packet constants.
- Specific behavior/contract: Java `LegionService.invitePlayerToLegion` validates invite eligibility, stores a response handler on the target, sends busy/no-user/no-right/member/race rejection messages, and sends the target invite question for valid online targets.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.invitePlayerToLegion` or this question-window path.
- Broad-validation trigger: live client packet dispatch, request-state mutation, and direct packet send were touched.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, request registry mutation, rejection messages, direct send, and question-window payload.
- Why this scope is sufficient: the change is isolated to `CM_LEGION 0x01`, existing response-request infrastructure, and packet constants.

## Validation Result

- Focused C# result: Passed, 81 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_InviteBranchConsumesJavaEmptyIdAndCharacterName` | Unit | `CM_LEGION.readImpl` exOpcode `0x01` | Invite packet consumes Java's empty id and target name fields. | Packet parser assertion. | Parser only. |
| `HandleInfrastructurePacketAsync_LegionInviteMissingOnlineTargetSendsNoUserLikeJava` | Unit | `LegionRestrictions.canInvitePlayer` null target branch | Missing/offline target sends `STR_GUILD_INVITE_NO_USER_TO_INVITE`. | Live handler packet assertion. | Uses test registry. |
| `HandleInfrastructurePacketAsync_LegionInviteWithoutPermissionSendsNoRightLikeJava` | Unit | `LegionPermissionsMask.INVITE` branch | Member without invite right sends no-right and does not register/send a target request. | Live handler state and packet assertions. | Permission source is C# player rank fields. |
| `HandleInfrastructurePacketAsync_LegionInviteSameLegionTargetSendsAlreadyMemberLikeJava` | Unit | same-legion target branch | Same-legion target sends already-member message and no question. | Live handler packet assertion. | Uses online player fixture. |
| `HandleInfrastructurePacketAsync_LegionInviteOtherLegionTargetSendsOtherMemberLikeJava` | Unit | other-legion target branch | Other-legion target sends other-member message and no question. | Live handler packet assertion. | Uses online player fixture. |
| `HandleInfrastructurePacketAsync_LegionInviteOtherRaceTargetSendsRaceDenialLikeJava` | Unit | other-race branch | Other-race invite sends Java denial and does not register target request state. | Live handler state and packet assertions. | C# does not yet expose Java `LEGION_INVITEOTHERFACTION` config override. |
| `HandleInfrastructurePacketAsync_LegionInviteBusyTargetSendsBusyLikeJava` | Unit | `ResponseRequester.putRequest` busy branch | Busy target keeps existing request and requester receives busy message. | Live handler state assertion. | Does not validate existing request's original behavior. |
| `HandleInfrastructurePacketAsync_LegionInviteOnlineTargetStoresRequestAndSendsQuestionLikeJava` | Unit | `LegionService.invitePlayerToLegion` success branch | Valid online invite stores pending target request, sends requester success message, and sends target question with Java parameters. | Live handler, response state, direct packet, and serialized payload assertions. | Accept/deny response remains pending. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x01` | `CmLegion` / `GameServerConnection.HandleLegionInviteAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, dispatch, modeled restrictions, pending request registration, requester messages, and target question send are covered. |
| `com.aionemu.gameserver.services.LegionService.invitePlayerToLegion` | `GameServerConnection.HandleLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Question flow is live. Java target denied-guild-invite setting and accept/deny membership handling remain gaps. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `QuestionResponseRegistry.PutRequest` | Runtime State | Partial | Unit Tested | Partial Parity | Successful invite stores request; duplicate question id is rejected as busy. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` guild invite | `SmQuestionWindow.GuildInviteDoYouAcceptInvitation` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Writes question id 80001 and the three Java parameters. No Java golden fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` guild invite helpers | `SmSystemMessage` guild invite methods | Server Packet | Partial | Unit Tested | Partial Parity | Message ids used by this path are covered. Denied-guild-invite setting helper is not wired because C# target setting is not modeled here. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 5 runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 2 (target denied-guild-invite setting, Java golden question fixture)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- `CM_QUESTION_RESPONSE` for guild invite accept/deny is not wired yet; pending invite request state is stored but response application is a separate runtime UOW.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- No Java golden fixture was generated for the guild invite question packet.
