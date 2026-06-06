# Phase 6 Session 2769 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2769] Broadcast legion announcement changes

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2769-Completion.md`
- `docs/Phase-6-Session-2769-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 82
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.changeAnnouncement`, `SM_LEGION_EDIT`, or `SM_LEGION_INFO`.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x09` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, edit-right rejection, truncation, persistence, active mutation, same-legion online propagation, requester packets, non-empty edit fanout, and clear info fanout are covered. No real-client verification. |
| `com.aionemu.gameserver.services.LegionService.changeAnnouncement` | `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Ports save, requester system messages, non-empty edit broadcast, and clear info broadcast. Java shared `Legion` announcement state is approximated by active and online same-legion `Player` announcement fields. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x05` | `SmLegionEdit.Announcement` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Packet writes edit type, announcement string, and Unix seconds like Java. No Java golden fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `SmLegionInfo.FromPlayer` | Server Packet | Partial | Unit Tested | Partial Parity | Clear branch uses existing C# player-backed legion info packet. Ranking and full Java `Legion` aggregate data remain approximated. |

## Known Gaps

- No Java golden packet fixture was generated for `SM_LEGION_EDIT` announcement or `SM_LEGION_INFO`.
- Full Java `Legion` aggregate announcement state is still approximated by active and online same-legion `Player` announcement fields.
- No real-client validation was performed for the announcement edit or clear broadcasts.
- `CM_LEGION` invite exOpcode `0x01` is currently not handled by the C# live legion switch; Java resolves an online target, runs invite restrictions, stores request state, and sends a live `SM_QUESTION_WINDOW` invite to the target.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire live `CM_LEGION` exOpcode `0x01` legion invite question flow for online targets like Java.

Runtime Progress Gate:
- Deferred/live behavior advanced: C# currently parses invite packets but does not dispatch exOpcode `0x01`; Java `LegionService.invitePlayerToLegion` resolves an online target, validates inviter rights and target eligibility, stores a response request, and sends `SM_QUESTION_WINDOW.STR_GUILD_DO_YOU_ACCEPT_INVITATION` to the target.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x01`, `LegionService.invitePlayerToLegion`, `LegionRestrictions.canInvitePlayer`, `ResponseRequester.putRequest`, and `SM_QUESTION_WINDOW`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionAsync` exOpcode `0x01`, a new live invite handler using `_connectionRegistry.TryGetOnlinePlayerByName`, target/requester `ResponseRequester` state, `SmQuestionWindow`, and Java-equivalent rejection messages where existing `SmSystemMessage` helpers are available.
- Client-visible/state effect expected: valid online invite requests create live pending request state and send the target a real question window; missing/offline targets and invalid inviter/target cases send Java-equivalent system messages where currently modeled.
- Why this is not preview-only/test-only/documentation-only: it wires a currently deferred live client packet path, mutates live request state, and sends real server packets from live code.

Focused validation recipe:
- Add live handler tests for exOpcode `0x01`:
  - valid brigade-general/deputy invite to online non-legion target sends the target invite question and stores request state;
  - missing/offline target sends Java-equivalent no-such-user message to requester;
  - non-authorized inviter sends Java-equivalent no-right message and does not send a target question;
  - already-legion target or same-legion target is rejected without mutating request state.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionService.invitePlayerToLegion` or the question-window payload is discovered.

Risks to watch:
- Java uses `World.getInstance().getPlayer(targetName)` and `ResponseRequester`; C# should reuse the connection registry and existing `ResponseRequester` model rather than adding a planner.
- Full Java add-to-legion accept behavior can be a later UOW; this UOW should stop at the live invite question flow unless the accept path is already safely available.
- Keep request IDs and visible question parameters aligned with Java `SM_QUESTION_WINDOW`.

Safe alternative runtime candidates:
- Port Java invite acceptance/add-to-legion only after the invite question flow is live.
- Port `SM_LEGION_DOMINION_LOC_INFO` only if paired with a live send path or runtime load that client code actually consumes.
- Return to challenge quest-finish progress only after a live quest-finish socket/runtime hook exists; do not use planner placeholders as migration progress.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
