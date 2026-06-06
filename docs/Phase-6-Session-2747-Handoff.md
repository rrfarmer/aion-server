# Phase 6 Session 2747 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2747] Hydrate legion info contribution fields

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionInfo.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2747-Completion.md`
- `docs/Phase-6-Session-2747-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/sql/aion_gs.sql`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLegionLevelForTradeListFilteringAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 18
- Failed: 0
- Skipped: 0
- Existing warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this DAO/server-packet path.

DB integration:
- DB-gated tests compiled but did not execute against live MySQL because `AION_GAMESERVER_DB_INTEGRATION` was not set to `1`.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x08` sends loaded contribution/dominion fields; exOpcodes `0x07` and `0x09` remain live from prior UOWs; others deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Loaded contribution and dominions now write from active runtime state; ranking still defaults to zero. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Load path maps selected Java `legions` columns used by `SM_LEGION_INFO`; broader LegionDAO behavior remains unported. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player` legion snapshot fields | Runtime State | Partial | Unit Tested | Partial Parity | C# stores a loaded player snapshot, not Java's shared `Legion` aggregate. |

## Known Gaps

- `SM_LEGION_INFO` ranking position remains defaulted because the legion Abyss ranking cache path is not ported.
- C# lacks Java's shared `Legion` aggregate and online legion-member broadcast helper.
- DB-gated tests were not run against live MySQL this session.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x0D` edit-permissions behavior for the active legion.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x0D` would stop being parser-only and mutate live legion permission masks from the client packet.
- Java source of truth: `CM_LEGION.readImpl` case `0x0D`, `CM_LEGION.runImpl` case `0x0D`, `LegionService.changePermissions`, `Legion.setLegionPermissions`, and `SM_LEGION_EDIT.writeImpl` type `0x02`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionAsync`, player legion permission snapshot fields, a new or extended legion repository persistence method if persistence is chosen, `SmLegionEdit` type `0x02`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect expected: a brigade general can change deputy/centurion/legionary/volunteer permission masks from live code and receive/send the Java-shaped permission edit packet; unauthorized players receive the Java no-right system message.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates live legion permission state, and sends a real server packet from live code.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x0D` parses four signed shorts, requires brigade general rank, updates the active player's loaded legion permission fields, and writes `SM_LEGION_EDIT` type `0x02` with those four masks in Java order.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore`
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch and runtime state mutation.
- Broad .NET decision: start focused; do not run unfiltered project/solution validation unless focused evidence exposes wider packet or repository risk.

Risks to watch:
- Java `changePermissions` broadcasts to all online legion members; C# may need an interim active-player-only send until safe fanout exists, documented as Partial Parity.
- Java mutates the shared `Legion` aggregate but does not persist directly in `changePermissions`; confirm whether another Java store path persists the modified masks before adding C# persistence.
- Add `SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT` id `1300283`; do not assume Java sends `STR_GUILD_CHANGE_RIGHT_DONE` for this path without source evidence.

Safe alternative candidates:
- Continue live `SM_LEGION_INFO` parity by investigating Abyss ranking cache only if a narrow runtime ranking source exists.
- Investigate online legion-member fanout only after confirming the C# connection registry can enumerate active players by legion id safely.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
