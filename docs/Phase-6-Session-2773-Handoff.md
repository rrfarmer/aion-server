# Phase 6 Session 2773 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2773] Send legion add-member packet on invite acceptance

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionAddMember.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2773-Completion.md`
- `docs/Phase-6-Session-2773-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_ADD_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionAddMember" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 86
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `SM_LEGION_ADD_MEMBER` serialization.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_ADD_MEMBER` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionAddMember` | Server Packet | Complete for modeled fields | Unit Tested | Partial Parity | Field order and invite-acceptance constructor values are tested. No Java golden packet fixture. |
| `com.aionemu.gameserver.services.LegionService.addLegionMember` add-member broadcast | `GameServerConnection.AcceptLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Live invite acceptance now sends `SmLegionAddMember`; member-list update, emblem fanout, aggregate member cache, member-limit behavior, and bonus behavior remain gaps. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` opcode `111` | `SmLegionAddMember.PacketOpCode` | Packet Opcode | Complete for this packet | Unit Tested through serialization | Partial Parity | Opcode is wired through the packet class; no generated opcode registry check. |

## Known Gaps

- C# still does not send Java's invite-acceptance `SM_LEGION_UPDATE_EMBLEM` fanout.
- C# still lacks Java's `updateLegionMemberList(player, false, player.getObjectId())` behavior for invite acceptance.
- C# still lacks a full Java-like `Legion` aggregate/member-list update and member-limit check for acceptance.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire Java `SM_LEGION_UPDATE_EMBLEM` fanout into live invite acceptance.

Runtime Progress Gate:
- Deferred/live behavior advanced: invite acceptance now persists, mutates state, and sends `SM_LEGION_ADD_MEMBER`, but it still does not broadcast Java's emblem update packet after the new member is added.
- Java source of truth: `LegionService.addLegionMember` line that calls `PacketSendUtility.broadcastPacket(player, new SM_LEGION_UPDATE_EMBLEM(legion.getLegionId(), legionEmblem), true)` and `SM_LEGION_UPDATE_EMBLEM.writeImpl`.
- C# runtime artifact to wire/fix: existing `SmLegionUpdateEmblem`, `GameServerConnection.AcceptLegionInviteAsync`, responder/copied legion emblem fields, and focused live-handler tests.
- Client-visible/state/persistence effect expected: clients visible to the accepted player, including the accepted player, receive the Java-shaped emblem update packet after the add-member fanout.
- Why this is not preview-only/test-only/documentation-only: it sends a real server packet from live invite-acceptance code and changes visible client state for the accepted player's legion emblem.

Focused validation recipe:
- Update invite acceptance tests to assert `SmLegionUpdateEmblem` visible broadcast with source responder and `includeSourcePlayer: true`.
- Assert the packet writes legion id, emblem id, emblem type, and ARGB bytes copied from the inviter to the responder.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~CmLegionSendEmblemInfoTests" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `SM_LEGION_UPDATE_EMBLEM` serialization is discovered.

Broad-validation trigger:
- none beyond focused live packet fanout; do not run unfiltered project tests unless focused validation exposes wider risk.

Risks to watch:
- `PacketSendUtility.broadcastPacket(player, ..., true)` is visible-player fanout, not online-legion fanout.
- Use responder legion emblem fields after `BindLegionInviteAcceptedPlayer` copies them from the inviter.
- Keep the add-member packet and edit/title order aligned with Java where existing C# support allows.

Safe alternative runtime candidates:
- Port the missing `SM_LEGION_MEMBERLIST` acceptance exclusion behavior if current C# has enough member-list packet support.
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
