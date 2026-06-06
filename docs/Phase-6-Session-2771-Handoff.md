# Phase 6 Session 2771 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2771] Consume legion invite denial responses

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2771-Completion.md`
- `docs/Phase-6-Session-2771-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 83
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.invitePlayerToLegion` invite response handling.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE` guild invite denial | `GameServerConnection.HandleQuestionResponseAsync` / `HandleLegionInviteDenyResponseAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Denial for question id `80001` consumes matching request state and notifies the inviter. Acceptance remains unwired. |
| `com.aionemu.gameserver.services.LegionService.invitePlayerToLegion` deny handler | `HandleLegionInviteDenyResponseAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Denial callback behavior is covered. Accept callback `addToLegion` remains pending. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `QuestionResponseRegistry.Respond` | Runtime State | Partial | Unit Tested | Partial Parity | Matching guild invite denial removes the request before side effects; missing request is side-effect free. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_REJECTED_INVITATION` | `SmSystemMessage.GuildInviteHeRejectedInvitation` | Server Packet | Complete for current helper | Unit Tested | Partial Parity | Message id and parameter are asserted through live response handling. No Java golden packet fixture. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.saveNewLegionMember` | no C# insert-member repository method yet | Repository | Not Started | No Tests | Unknown | Acceptance requires inserting `legion_members(legion_id, player_id, rank)` before live add-to-legion can be wired honestly. |

## Known Gaps

- Guild invite acceptance is not wired; accepting the invite still does not add the responder to the inviter's legion.
- C# has no existing repository method equivalent to Java `LegionMemberDAO.saveNewLegionMember`, which inserts into `legion_members(legion_id, player_id, rank)`.
- Java `LegionService.addToLegion` also displays the current legion announcement and records join history; these must be included when acceptance is wired.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire live guild invite acceptance by adding a real C# repository mutation for Java `LegionMemberDAO.saveNewLegionMember` and applying `LegionService.addToLegion` effects from `CM_QUESTION_RESPONSE` question id `80001`.

Runtime Progress Gate:
- Deferred/live behavior advanced: C# stores and denies guild invite question responses, but nonzero acceptance responses to question id `80001` still do not add the target to the inviter's legion.
- Java source of truth: `LegionService.invitePlayerToLegion` accept handler, `LegionService.addToLegion`, `Legion.addLegionMember`, `LegionMemberDAO.saveNewLegionMember`, `LegionService.addHistory`, and `displayLegionAnnouncement`.
- C# runtime artifact to wire/fix: `IPlayerEnterWorldRepository` / `PlayerEnterWorldRepository` insert-member method, `EmptyPlayerEnterWorldRepository`, `GameServerConnection.HandleQuestionResponseAsync`, `PendingLegionInviteRequest`, target/player legion fields, `SmLegionUpdateMember` or existing legion update packets, `SmSystemMessage.GuildNotice`, and `InsertLegionHistoryAsync`.
- Client-visible/state/persistence effect expected: accepting a pending guild invite consumes the response request, persists the responder as a legion member with Java rank `LEGIONARY`, mutates the responder's live legion fields, records join history, displays current announcement when present, and sends Java-equivalent legion update/title/info packets where existing packet support allows. If member limit support is unavailable, document the gap and preserve Java-equivalent failure behavior where possible.
- Why this is not preview-only/test-only/documentation-only: it will consume live pending request state, persist a new `legion_members` row using the existing DB shape, mutate live player legion state, and send real server packets from live handler code.

Focused validation recipe:
- Add repository tests or extend `CmLegionTests` fakes for:
  - `SaveNewLegionMemberAsync` inserts `(legion_id, player_id, rank)` equivalent to Java `LegionMemberDAO.saveNewLegionMember`;
  - invite acceptance consumes target request state and clears `PendingLegionInviteRequest`;
  - acceptance persists the new member, mutates target legion fields/rank/title state, inserts join history, and sends announcement/update packets to appropriate online players;
  - stale inviter or failed insert does not mutate live legion state.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionMemberDAO.saveNewLegionMember` or `LegionService.addToLegion` is discovered.

Risks to watch:
- Do not implement acceptance as in-memory-only state; it must persist through the existing `legion_members` table shape.
- Java `Legion.addLegionMember` may enforce member limits; if C# lacks full legion aggregate/member-limit state, document the precise missing check and avoid claiming full parity.
- Preserve Java ordering: add/bind member, display announcement, add history.

Safe alternative runtime candidates:
- If full acceptance fanout is too broad, first add the repository insert and live accept state mutation/persistence, then leave richer broadcast fanout as the following UOW.
- Port Java `DeniedStatus.GUILD` only if a live C# invite-denial setting exists or can be wired into actual player settings state.
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
