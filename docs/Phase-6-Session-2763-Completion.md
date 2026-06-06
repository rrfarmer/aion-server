# Phase 6 Session 2763 Completion

## Unit of Work

[Phase 6][UOW-2763] Wire legion Brigade General transfer questions

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x05` now drives the Java two-stage Brigade General transfer prompt flow instead of falling through the live C# handler.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl`, `LegionService.startBrigadeGeneralChangeProcess`, `LegionService.appointBrigadeGeneral(Player, Player)`, and `LegionRestrictions.canAppointBrigadeGeneral`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, `Player.ResponseRequester`, `QuestionResponseRegistry`, `PendingLegionBrigadeGeneralTransferRequest`, `SmQuestionWindow`, and `SmSystemMessage`.
- Client-visible/state effect changed: a live Brigade General transfer request now resolves an online target, sends Java-equivalent system/question packets, stores pending requester/target response state, rejects missing/invalid/busy targets with Java message ids, and notifies the requester when the target denies the offer.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path, sends real server packets from live connection code, and mutates live pending player response state.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/legion/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Wired `CM_LEGION` exOpcode `0x05` to `HandleLegionBrigadeGeneralTransferRequestAsync`.
- Added `PendingLegionBrigadeGeneralTransferRequest` to carry requester, target, and legion identity through the two question-response stages.
- Added question response kinds for requester confirmation and target transfer offer.
- Added Java message ids for Brigade General transfer failures, sent-offer notification, and target denial notification.
- Added `SM_QUESTION_WINDOW` code `80011` for the target offer question.
- Implemented Java-equivalent live prompt behavior:
  - missing online target sends `STR_GUILD_CHANGE_MASTER_NO_SUCH_USER`;
  - requester receives question id `904979` with target name;
  - requester acceptance validates Brigade General permission, self-target, and same-legion membership;
  - busy target sends `STR_GUILD_CHANGE_MASTER_SENT_CANT_OFFER_WHEN_HE_IS_QUESTION_ASKED`;
  - available target receives `SM_QUESTION_WINDOW(80011, requesterObjectId, 0, requesterName)`;
  - requester receives `STR_GUILD_CHANGE_MASTER_SENT_OFFER_MSG_TO_HIM(targetName)`;
  - target denial sends `STR_GUILD_CHANGE_MASTER_HE_DECLINE_YOUR_OFFER(targetName)` to the requester.

## Validation Decision

- Changed surface: live client packet dispatch, question-response pending state, direct packet sends, and system-message ids.
- Specific behavior/contract: Java `CM_LEGION 0x05` read shape and the live requester-confirm/target-offer prompt contract through denial.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this question-response service path.
- Broad-validation trigger: a deferred live packet branch and question-response handler path were enabled.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, packet serialization, and pending response state.
- Why this scope is sufficient: no shared packet primitive, crypto, scheduler, schema, persistence repository, or broad world-state primitive changed.

## Validation Result

- Focused C# result: Passed, 70 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_BrigadeGeneralTransferConsumesJavaEmptyIdAndCharacterName` | Unit | `CM_LEGION.readImpl` exOpcode `0x05` | Reads ignored `D` then character name string. | Parser assertion. | None for scoped read shape. |
| `HandleInfrastructurePacketAsync_BrigadeGeneralTransferMissingOnlineTargetSendsNoSuchUserLikeJava` | Unit | `LegionService.startBrigadeGeneralChangeProcess` | Missing online target sends message id `1300270`. | Live handler assertion. | Uses connection registry lookup, not a real client. |
| `HandleInfrastructurePacketAsync_BrigadeGeneralTransferStoresRequesterConfirmAndSendsQuestionLikeJava` | Unit | `LegionService.startBrigadeGeneralChangeProcess` | Stores requester pending request and sends question id `904979` with target name. | Live handler and packet payload assertions. | Does not include target acceptance mutation. |
| `HandleQuestionResponseAsync_BrigadeGeneralTransferConfirmSendsOfferToTargetLikeJava` | Unit | `LegionService.appointBrigadeGeneral(Player, Player)` | Requester acceptance stores target pending request, sends requester offer message, and sends target question id `80011`. | Live handler and packet payload assertions. | Rank mutation remains next UOW. |
| `HandleQuestionResponseAsync_BrigadeGeneralTransferConfirmRejectsBusyTargetLikeJava` | Unit | `ResponseRequester.putRequest` busy branch | Busy target sends message id `1300331` to requester. | Live handler assertion. | Does not cover every invalid restriction branch. |
| `HandleQuestionResponseAsync_BrigadeGeneralTransferOfferDenyNotifiesRequesterLikeJava` | Unit | Target denial branch in `LegionService.appointBrigadeGeneral(Player, Player)` requester callback | Target denial sends message id `1300332` to requester. | Live handler assertion. | Accept-side rank mutation remains unported. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x05` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape and live prompt dispatch are wired. Target acceptance does not yet mutate ranks. |
| `com.aionemu.gameserver.services.LegionService.startBrigadeGeneralChangeProcess` | `HandleLegionBrigadeGeneralTransferRequestAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Online target lookup, requester pending state, and requester question packet are ported. |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(Player, Player)` | `HandleLegionBrigadeGeneralTransferConfirmResponseAsync` / `HandleLegionBrigadeGeneralTransferOfferResponseAsync` | Live Question Path | Partial | Unit Tested | Partial Parity | Restriction checks, busy target, offer packet, and denial notification are ported. Accept mutation is intentionally left as the next runtime slice. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `SmQuestionWindow` | Server Packet | Partial | Unit Tested | Partial Parity | Existing serializer is reused with question id `80011`; no Java golden packet fixture was generated. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` transfer ids | `SmSystemMessage` helpers | Server Packet | Partial | Unit Tested via handler sends | Partial Parity | Message ids for the prompt/deny path are added and asserted through live sends. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 6 runtime/packet/state artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 1 accept-side leadership rank mutation/persistence/broadcast slice
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Target acceptance of question id `80011` currently consumes the pending request but does not yet promote the target, demote the former Brigade General, persist rank changes, add legion history, or broadcast `SM_LEGION_UPDATE_MEMBER`/`SM_LEGION_EDIT(0x08)`.
- No Java golden packet fixture was generated for the question packets.
- Invalid restriction branches beyond missing target and busy target are implemented but not exhaustively tested in this UOW.
- No real-client validation was performed for the leadership-transfer dialogs.
