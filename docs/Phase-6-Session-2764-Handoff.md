# Phase 6 Session 2764 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2764] Apply legion Brigade General transfer acceptance

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2764-Completion.md`
- `docs/Phase-6-Session-2764-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/legion/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionRank.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionUpdateMember|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 81
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.appointBrigadeGeneral`.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(Player, Player)` | `GameServerConnection.HandleLegionBrigadeGeneralTransferOfferResponseAsync` | Live Question Path | Partial | Unit Tested | Partial Parity | Accept and deny branches are now live. Java audit logging for invalid second validation is not ported. |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(LegionMember)` | `GameServerConnection.AppointLegionBrigadeGeneralAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Rank mutation, history, and online broadcast are ported. C# explicitly saves both online rank rows through existing repository shape. |
| `com.aionemu.gameserver.model.team.legion.LegionRank` | `Aion.GameServer.Model.Legion.LegionRanks` | Model / Constants | Partial | Unit Tested via handler packets | Partial Parity | Uses Java enum names and rank ids already present; this UOW consumes `BRIGADE_GENERAL` and `CENTURION` in live mutation. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `Aion.GameServer.Model.Legion.LegionHistoryActions` | Model / Constants | Partial | Unit Tested via repository call | Partial Parity | Added named `APPOINTED` constant over the existing id/type mapping. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | `IPlayerEnterWorldRepository.SaveLegionMemberRankAsync` | Repository Boundary | Partial | Unit Tested via fake repository | Partial Parity | Existing C# method persists rank only, not nickname/selfintro/challenge score; acceptable for scoped leadership transfer but not full DAO parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `SmLegionUpdateMember` | Server Packet | Partial | Unit Tested | Partial Parity | Existing serializer reused; UOW asserts member update packets emitted from live accept path. No Java golden packet fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x08` | `SmLegionEdit.RefreshAnnouncement()` | Server Packet | Complete for type `0x08` | Unit Tested | Partial Parity | Packet shape writes only the edit type like Java. Helper name follows prior C# naming even though Java comment labels it "Refresh Legion Announcement?". |

## Known Gaps

- Java audit logging for a failed second `canAppointBrigadeGeneral` check is not ported.
- The C# rank persistence lifecycle differs from Java's online/offline member store behavior; this UOW uses explicit rank saves for both online participants.
- No Java golden packet fixture was generated for `SM_LEGION_UPDATE_MEMBER` or `SM_LEGION_EDIT(0x08)` in this transfer path.
- `CM_LEGION` rank change exOpcode `0x06` still only sends the update packet to the requester in C#; Java broadcasts rank changes to the whole legion.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Bring live `CM_LEGION` exOpcode `0x06` rank-change broadcast and online target state mutation closer to Java.

Runtime Progress Gate:
- Deferred/live behavior advanced: C# rank changes currently update an offline repository row or send one `SM_LEGION_UPDATE_MEMBER` to the requester; Java `LegionService.appointRank` broadcasts the member update to the whole legion and online target state should reflect the new rank.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x06`, `LegionService.appointRank`, `LegionRestrictions.canAppointRank`, `LegionMember.setRank`, `LegionMemberDAO.storeLegionMember`, and `PacketSendUtility.broadcastToLegion(legion, new SM_LEGION_UPDATE_MEMBER(...))`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionRankChangeAsync`, online-player resolution through `IGameClientConnectionRegistry`, live `Player.LegionRank`, existing offline `SaveLegionMemberRankAsync`, and same-legion packet fanout using `SmLegionUpdateMember`.
- Client-visible/state/persistence effect expected: promoting/demoting a member mutates an online target's live `Player.LegionRank`, persists offline targets through the existing repository shape, and sends the Java rank-change member update packet to online same-legion players instead of only the requester.
- Why this is not preview-only/test-only/documentation-only: it mutates live player legion state and sends real server packets from a live client handler.

Focused validation recipe:
- Add live handler tests for exOpcode `0x06`:
  - online target rank changes in memory and same-legion online players receive `SM_LEGION_UPDATE_MEMBER`;
  - outsider online players do not receive the broadcast;
  - offline target still persists through `SaveLegionMemberRankAsync`;
  - existing rank restriction branches still send Java system messages and do not broadcast.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionUpdateMember" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionService.appointRank` or `SM_LEGION_UPDATE_MEMBER` is discovered.

Risks to watch:
- `ResolveLegionMemberByNameAsync` currently returns repository snapshots except for the active player. For online targets, prefer the live registry player when available so `Player.LegionRank` mutates.
- Java stores only offline members in `appointRank`; keep that behavior unless a C# persistence lifecycle gap requires an intentional, documented difference.
- Avoid broad validation unless focused rank-change tests expose a wider packet or state risk.

Safe alternative runtime candidates:
- Port `SM_LEGION_DOMINION_LOC_INFO` only if paired with a live send path or runtime load that client code actually consumes.
- Add live dominion ranking broadcast after a real instance-finish score update only after the C# instance-finish path exists.
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
