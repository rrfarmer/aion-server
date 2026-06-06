# Phase 6 Session 2772 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2772] Apply legion invite acceptance

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2772-Completion.md`
- `docs/Phase-6-Session-2772-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/model/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 122
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.addToLegion` or `LegionMemberDAO.saveNewLegionMember`.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `CM_QUESTION_RESPONSE` guild invite acceptance | `GameServerConnection.HandleQuestionResponseAsync` / `HandleLegionInviteResponseAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Question id `80001` accept now consumes request state and enters live add-to-legion effects. |
| `LegionService.addToLegion` | `AcceptLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Persists, binds responder state, displays announcement, records history, and sends available packets. Missing full aggregate member-limit behavior and exact add-member fanout. |
| `LegionService.addLegionMember(legion, player)` default rank | `BindLegionInviteAcceptedPlayer` | Runtime State | Partial | Unit Tested | Partial Parity | Uses Java default rank `VOLUNTEER`; previous handoff's `LEGIONARY` note was a source-inspection error. |
| `LegionMemberDAO.saveNewLegionMember` | `IPlayerEnterWorldRepository.SaveNewLegionMemberAsync` / `MySqlPlayerEnterWorldRepository.SaveNewLegionMemberAsync` | Repository | Partial | Unit Tested through fake/live handler | Partial Parity | Inserts `legion_id`, `player_id`, and `rank`; no DB integration test was run. |
| `LegionHistoryAction.JOIN` | `LegionHistoryActions.Join` | Runtime Persistence Metadata | Complete for action mapping | Unit Tested through acceptance | Partial Parity | Action name and id mapping are wired for join history insertion. |
| `SM_LEGION_INFO` plus guild notice | `SmLegionInfo.FromPlayer` / `SmSystemMessage.GuildNotice` | Server Packet | Partial | Unit Tested | Partial Parity | Responder receives info and announcement notice after live acceptance. No Java golden fixture. |
| `SM_LEGION_ADD_MEMBER` Java fanout | Existing `SmLegionUpdateMember` fanout | Server Packet | Not Ported | No Direct Test | Known Gap | C# uses existing update-member packet as a temporary live fanout; exact Java add-member packet remains a runtime packet UOW. |

## Known Gaps

- C# still lacks Java `SM_LEGION_ADD_MEMBER`; accepted members currently trigger the existing `SmLegionUpdateMember` fanout.
- C# still lacks a full Java-like `Legion` aggregate/member-list update and member-limit check for the acceptance path.
- C# does not yet send Java's emblem update fanout during invite acceptance.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Port and wire Java `SM_LEGION_ADD_MEMBER` for the live invite-acceptance fanout.

Runtime Progress Gate:
- Deferred/live behavior advanced: invite acceptance now persists and mutates state, but online legion members receive only C#'s existing `SmLegionUpdateMember` approximation instead of Java's exact `SM_LEGION_ADD_MEMBER` packet.
- Java source of truth: `LegionService.addLegionMember` broadcast path and `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_ADD_MEMBER.java`.
- C# runtime artifact to wire/fix: new `SmLegionAddMember` packet, `AcceptLegionInviteAsync` online-legion fanout, and focused packet/live-handler tests.
- Client-visible/state/persistence effect expected: clients on the live accept path receive the Java-shaped add-member packet for the joining player rather than only the update-member approximation.
- Why this is not preview-only/test-only/documentation-only: it sends a real server packet from live invite acceptance code and changes the client-visible packet contract for online legion members.

Focused validation recipe:
- Add packet serialization assertions for `SmLegionAddMember`.
- Update the invite acceptance test to assert `SmLegionAddMember` fanout to the inviter, joined player, and online same-legion bystander.
- Keep `SmLegionUpdateMember` only if Java also sends a matching update in that exact path; otherwise replace the approximation.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionAddMember" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `SM_LEGION_ADD_MEMBER` serialization is discovered.

Risks to watch:
- Java packet field order must be checked directly from `SM_LEGION_ADD_MEMBER.writeImpl`.
- Do not claim full accept-path parity until member-limit behavior, member-list aggregate update, and emblem fanout are also handled or objectively proven unnecessary for this C# runtime.
- Preserve the Java invite rank `VOLUNTEER`.

Safe alternative runtime candidates:
- Wire invite-acceptance emblem fanout using Java `SM_LEGION_UPDATE_EMBLEM` if existing C# packet support is complete.
- Add the Java-equivalent aggregate/member-limit guard only if current C# runtime data can load enough live legion member state to enforce it honestly.
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
