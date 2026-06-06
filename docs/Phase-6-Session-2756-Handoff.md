# Phase 6 Session 2756 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2756] Send legion title clear on removal

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionUpdateTitle.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2756-Completion.md`
- `docs/Phase-6-Session-2756-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_TITLE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 65
- Failed: 0
- Skipped: 0
- Existing warnings only.

Other validation:
- `git diff --check` passed with line-ending warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet/service path.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync`, `HandleLegionLeaveAsync`, and `BroadcastLegionUpdateTitleAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Title-clear packet is live for kick/self-leave removals; icon/conqueror/bonus effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_TITLE` | `SmLegionUpdateTitle` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `114` and title-clear payload covered. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Kick and leave paths now send visible title-clear updates. |

## Known Gaps

- No `SM_ICON_INFO(1, false)` on legion bonus removal yet.
- No `ConquerorAndProtectorService.onLeaveLegion` or `legion.removeBonus()` equivalent.
- Visible-recipient semantics depend on C# `BroadcastToVisiblePlayersAsync`, not a full Java `KnownList` aggregate comparison.
- Repository delete/history remains fake-tested, not DB-gated.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Port and send `SM_ICON_INFO(1, false)` for live legion removal when a reliable legion-bonus state is available, or otherwise select the next adjacent `CM_LEGION` live subaction that passes the Runtime Progress Gate.

Runtime Progress Gate:
- Deferred/live behavior advanced: Java's bonus-icon removal packet would be sent from live kick/leave code for players leaving a bonus legion.
- Java source of truth: `LegionService.removeLegionMember` branch `if (legion.hasBonus()) PacketSendUtility.sendPacket(player, new SM_ICON_INFO(1, false))`, `SM_ICON_INFO.writeImpl`, and its server opcode.
- C# runtime artifact to wire/fix: locate or add `SmIconInfo`, identify reliable C# legion bonus state or document blocker, and wire live kick/leave removal only when the state source exists.
- Client-visible/state/persistence effect expected: removed online players receive the Java-shaped icon-disable packet when their former legion had an active bonus.
- Why this is not preview-only/test-only/documentation-only: it would send a missing Java server packet from live legion removal code.

Focused validation recipe:
- Specific behavior/contract: `SM_ICON_INFO(1, false)` packet payload and conditional send from live removal only when C# can prove former legion bonus state.
- C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`; broaden only if packet tests live outside `CmLegionTests`.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live server-packet output and existing live membership state mutation; start focused.

Risks to watch:
- Do not fabricate a legion-bonus state source. If none exists, choose a different runtime UOW rather than adding planner/readiness scaffolding.
- Preserve packet order relative to leave packet, title update, and reset if implemented.
- If `SM_ICON_INFO` is absent but bonus state is blocked, consider another adjacent live `CM_LEGION` branch instead of a packet-only unreachable change.

Safe alternative runtime candidates:
- Wire another deferred `CM_LEGION` subaction only if it has Java source, a live state/packet effect, and focused validation.
- Continue improving legion removal only through live packet/state effects, not preview or evidence layers.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
