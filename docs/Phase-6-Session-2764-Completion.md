# Phase 6 Session 2764 Completion

## Unit of Work

[Phase 6][UOW-2764] Apply legion Brigade General transfer acceptance

## Runtime Progress Gate

- Deferred/live behavior advanced: target acceptance of `SM_QUESTION_WINDOW(80011)` no longer only consumes pending state; it now applies the live Brigade General transfer.
- Java source of truth: `LegionService.appointBrigadeGeneral(Player, Player)`, `LegionService.appointBrigadeGeneral(LegionMember)`, `LegionRestrictions.canAppointBrigadeGeneral`, `SM_LEGION_UPDATE_MEMBER`, `SM_LEGION_EDIT(0x08)`, `LegionMemberDAO.storeLegionMember`, and `LegionService.addHistory(... APPOINTED)`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionBrigadeGeneralTransferOfferResponseAsync`, live `Player.LegionRank`, `IPlayerEnterWorldRepository.SaveLegionMemberRankAsync`, `IPlayerEnterWorldRepository.InsertLegionHistoryAsync`, `SmLegionUpdateMember`, `SmLegionEdit.RefreshAnnouncement()`, and online same-legion packet fanout.
- Client-visible/state/persistence effect changed: accepting the offer demotes the former Brigade General to Centurion, promotes the target to Brigade General, saves rank changes, records `APPOINTED` history, and sends Java-equivalent member/edit packets to online same-legion players.
- Why this is not preview-only/test-only/documentation-only: it mutates live player legion rank state, persists runtime rank/history state, and sends real server packets from the live question-response path.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/legion/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionRank.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`

## C# Runtime Changes

- Implemented the accepted target-offer branch for `CM_QUESTION_RESPONSE` question id `80011`.
- Re-checks requester/target eligibility before mutation, matching Java's second `canAppointBrigadeGeneral` guard without sending new client messages.
- Demotes the requester from `BRIGADE_GENERAL` to `CENTURION` and promotes the target to `BRIGADE_GENERAL`.
- Persists both C# online rank rows through the existing `SaveLegionMemberRankAsync` shape. Java only explicitly stores the previous Brigade General when offline; the C# online save lifecycle is not equivalent yet, so this UOW uses the existing direct rank persistence boundary for both live mutations and documents the difference conservatively.
- Inserts legion history action `APPOINTED` with the accepted target name.
- Broadcasts to online same-legion players:
  - `SM_LEGION_UPDATE_MEMBER` for the former Brigade General;
  - `SM_LEGION_UPDATE_MEMBER` for the new Brigade General with message id `1300273`;
  - `SM_LEGION_EDIT(0x08)` via `SmLegionEdit.RefreshAnnouncement()`.
- Keeps stale/invalid accept responses side-effect free when the requester is no longer online or no longer eligible.

## Validation Decision

- Changed surface: live question-response handler, live player legion-rank state, persistence calls, legion history, and same-legion packet broadcasts.
- Specific behavior/contract: Java target acceptance of `STR_GUILD_CHANGE_MASTER_DO_YOU_ACCEPT_OFFER` promotes/demotes ranks, persists/history state, and broadcasts `SM_LEGION_UPDATE_MEMBER` plus `SM_LEGION_EDIT(0x08)`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionUpdateMember|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.appointBrigadeGeneral`.
- Broad-validation trigger: live state, persistence, connection dispatch, and packet broadcast side effects were enabled.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, rank state mutation, persistence/history calls, and packet serialization contracts. No shared packet primitive, crypto, scheduler, schema, or common persistence abstraction changed.
- Why this scope is sufficient: the risk is isolated to the existing legion transfer question path and existing legion member/edit packet serializers.

## Validation Result

- Focused C# result: Passed, 81 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_BrigadeGeneralTransferOfferAcceptMutatesRanksPersistsHistoryAndBroadcastsLikeJava` | Unit | `LegionService.appointBrigadeGeneral(LegionMember)` | Target accept demotes requester, promotes target, saves both ranks, inserts `APPOINTED` history, and broadcasts two member updates plus edit `0x08` to same-legion online players. | Live handler, state, repository-call, and packet payload assertions. | Uses test connection registry, not a real client. C# persists both online ranks, documented as a conservative persistence choice. |
| `HandleQuestionResponseAsync_BrigadeGeneralTransferOfferAcceptWithStaleRequesterDoesNotMutateLikeJava` | Unit | `LegionService.appointBrigadeGeneral(Player, Player)` second validation branch | Stale requester state does not mutate ranks, persist, or broadcast. | Live handler assertion. | Java logs an audit entry for invalid second validation; C# does not yet port that audit logger side effect. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(Player, Player)` | `GameServerConnection.HandleLegionBrigadeGeneralTransferOfferResponseAsync` | Live Question Path | Partial | Unit Tested | Partial Parity | Accept and deny branches are now live. Java audit logging for invalid second validation is not ported. |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(LegionMember)` | `GameServerConnection.AppointLegionBrigadeGeneralAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Rank mutation, history, and online broadcast are ported. C# explicitly saves both online rank rows through existing repository shape. |
| `com.aionemu.gameserver.model.team.legion.LegionRank` | `Aion.GameServer.Model.Legion.LegionRanks` | Model / Constants | Partial | Unit Tested via handler packets | Partial Parity | Uses Java enum names and rank ids already present; this UOW consumes `BRIGADE_GENERAL` and `CENTURION` in live mutation. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `Aion.GameServer.Model.Legion.LegionHistoryActions` | Model / Constants | Partial | Unit Tested via repository call | Partial Parity | Added named `APPOINTED` constant over the existing id/type mapping. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | `IPlayerEnterWorldRepository.SaveLegionMemberRankAsync` | Repository Boundary | Partial | Unit Tested via fake repository | Partial Parity | Existing C# method persists rank only, not nickname/selfintro/challenge score; acceptable for scoped leadership transfer but not full DAO parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `SmLegionUpdateMember` | Server Packet | Partial | Unit Tested | Partial Parity | Existing serializer reused; UOW asserts member update packets emitted from live accept path. No Java golden packet fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x08` | `SmLegionEdit.RefreshAnnouncement()` | Server Packet | Complete for type `0x08` | Unit Tested | Partial Parity | Packet shape writes only the edit type like Java. Helper name follows prior C# naming even though Java comment labels it "Refresh Legion Announcement?". |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported or advanced in this UOW: 7 runtime/model/repository/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 7
- Total blocked artifacts: 2 (Java audit logging for invalid second validation, exact Java/C# online rank-save lifecycle equivalence)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java audit logging for a failed second `canAppointBrigadeGeneral` check is not ported.
- The C# rank persistence lifecycle differs from Java's online/offline member store behavior; this UOW uses explicit rank saves for both online participants.
- No Java golden packet fixture was generated for `SM_LEGION_UPDATE_MEMBER` or `SM_LEGION_EDIT(0x08)` in this transfer path.
- No real-client validation was performed for the completed leadership-transfer flow.
