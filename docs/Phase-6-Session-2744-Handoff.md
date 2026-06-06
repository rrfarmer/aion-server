# Phase 6 Session 2744 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2744] Send live legion info refresh

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionInfo.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2744-Completion.md`
- `docs/Phase-6-Session-2744-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmLegion`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo`
- `Aion.GameServer.Tests.CmLegionTests`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal"
```

Result:
- Passed: 7
- Failed: 0
- Skipped: 0
- Existing warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet send path.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x08` is live; all other service/mutation subactions remain deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Payload order and opcode are ported. Live runtime defaults ranking/contribution/dominion/announcement fields until the shared legion aggregate is ported. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmLegionInfo.PacketOpCode` | Opcode Metadata | Complete | Unit Tested indirectly | Partial Parity | Opcode `110` reviewed and used. |

## Known Gaps

- No shared Java-equivalent `Legion` aggregate exists in C#, so `SM_LEGION_INFO` cannot yet use `AbyssRankingCache`, contribution points, dominion fields, or announcement history from runtime legion state.
- `CM_LEGION` mutation/service exOpcodes remain deferred.
- No real client validation was performed for the legion info window.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` subopcode `0x07` to show the active legion notice.

Runtime Progress Gate:
- Deferred/live behavior advanced: a live client `/gnotice` or legion notice request would receive the Java-equivalent system message response instead of staying unimplemented.
- Java source of truth: `CM_LEGION.runImpl` case `0x07` sends `STR_MSG_NOSET_GUILD_NOTICE()` when `legion.getAnnouncement()` is null, otherwise sends `STR_GUILD_NOTICE(message, unixTime)`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionAsync`, `SmSystemMessage` helpers for the two Java system messages, and whichever C# runtime/player legion announcement source is available.
- Client-visible/runtime effect: the client receives a real system message for the current legion notice request.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client/server packet path and sends a real server packet from live code.

Focused validation recipe:
- Specific behavior/contract: `CM_LEGION` exOpcode `0x07` sends the Java no-notice system message for a legion member when no announcement runtime state is loaded; if announcement state exists, also test the notice message/time payload.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"`
- Java/Maven: not expected unless a narrow Java system-message fixture is added or discovered.
- Broad-validation trigger: live connection dispatch changed.
- Broad .NET decision: start focused; do not run an unfiltered project/solution test unless focused evidence exposes wider packet-system risk.

Safe alternative candidates:
- Load additional Java legion fields into runtime C# structures used by live `SmLegionInfo`, such as contribution points or announcement, if the existing database schema and C# repository can do so without inventing a new aggregate.
- Inspect and wire a different `CM_LEGION` subopcode only when it has a concrete Java source path, active C# runtime state, and a client-visible packet/state effect.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
