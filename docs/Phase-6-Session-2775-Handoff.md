# Phase 6 Session 2775 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2775] Send legion member list on invite acceptance

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionMemberList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2775-Completion.md`
- `docs/Phase-6-Session-2775-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionMemberList" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 87
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `SM_LEGION_MEMBERLIST` serialization.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionMemberList` | Server Packet | Partial | Unit Tested | Partial Parity | Modeled row fields and first/last count behavior are tested. Housing ids default to zero and no Java golden fixture exists. |
| `com.aionemu.gameserver.services.LegionService.updateLegionMemberList` invite path | `GameServerConnection.SendLegionInviteMemberListAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Sends one current-online roster chunk to the accepted player and excludes the accepted player. Full Java roster cache, offline members, and 80-row split behavior are not ported. |
| `com.aionemu.gameserver.model.team.legion.Legion.canAddMember` | no C# invite-acceptance member-limit guard yet | Runtime State Guard | Not Started | No Tests | Unknown | Java checks max members by legion level before adding; C# still persists first without this guard. |

## Known Gaps

- Member list currently includes only online same-legion players visible through the C# connection registry; Java uses the full legion member cache including offline members.
- Java splits member lists into chunks of 80; this C# invite path currently sends a single chunk.
- Member-list house address and door-state ids are currently zero because the C# invite path does not query active houses for roster rows.
- C# still lacks Java's `Legion.canAddMember` max-member guard before invite-acceptance persistence.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Add the Java `Legion.canAddMember` max-member guard to live invite acceptance before persisting a new legion member.

Runtime Progress Gate:
- Deferred/live behavior advanced: invite acceptance now persists and sends the main Java packet sequence, but it still does not reject full legions before insert like Java `Legion.addLegionMember` does.
- Java source of truth: `LegionService.addToLegion`, `Legion.addLegionMember`, `Legion.canAddMember`, `LegionConfig.LEGION_LEVEL*_MAX_MEMBERS`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE`.
- C# runtime artifact to wire/fix: `GameServerConnection.AcceptLegionInviteAsync`, `IPlayerEnterWorldRepository.CountLegionMembersAsync`, new legion max-member config values under existing legion options, and focused live-handler tests.
- Client-visible/state/persistence effect expected: accepting an invite for a full legion sends the inviter the Java full-member system message and does not persist or mutate the responder.
- Why this is not preview-only/test-only/documentation-only: it changes live acceptance control flow, prevents an invalid database insert/state mutation, and sends a real server packet on the failure branch.

Focused validation recipe:
- Add tests where `CountLegionMembersAsync` returns the max for the inviter's legion level.
- Assert no `SaveNewLegionMemberAsync`, no responder mutation, no member-list/add-member/emblem/edit/title packets, and a direct full-member system message to the inviter.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `Legion.canAddMember` is discovered.

Broad-validation trigger:
- none beyond focused live acceptance guard; do not run unfiltered project tests unless focused validation exposes wider risk.

Risks to watch:
- C# currently has level-up required-member config but not max-member config; add Java property mappings conservatively without changing existing keys.
- Java uses in-memory legion member ids. C# can use `CountLegionMembersAsync` against `legion_members` as the existing persistence-backed substitute, but document that aggregate cache parity remains partial.
- Preserve Java ordering: guard before `SaveNewLegionMemberAsync`.

Safe alternative runtime candidates:
- Load offline legion members into `SmLegionMemberList` only if a narrow repository method can be added and used by the live invite path in the same UOW.
- Wire Java `legion.addBonus()` only after identifying the live C# bonus/icon state it should mutate or send.
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
