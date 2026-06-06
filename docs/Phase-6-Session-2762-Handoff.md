# Phase 6 Session 2762 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2762] Send legion dominion ranking packet

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionDominionRank.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmLegionDominionRankTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2762-Completion.md`
- `docs/Phase-6-Session-2762-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_DOMINION_REQUEST_RANKING.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_DOMINION_RANK.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionLocation.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionParticipantInfo.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDominionDAO.java`
- `game-server/sql/aion_gs.sql`

## Tests Run

Focused C# packet/handler command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionDominion|FullyQualifiedName~SmLegionDominionRank|FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 67
- Failed: 0
- Skipped: 0

Guarded repository command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LoadLegionDominionParticipantsAsync_LoadsJavaRowsWithLegionNameFallback" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 1
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION_DOMINION_REQUEST_RANKING` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegionDominionRequestRanking` / `GameServerConnection` | Client Packet / Live Handler | Complete for request/read/dispatch | Unit Tested | Partial Parity | Valid id guard and live send are ported. Java service-backed in-memory location model is approximated through persisted repository rows for ranking until the full dominion service is ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_DOMINION_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionDominionRank` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Field order, rank, sort, and top-25 replacement are tested. No Java golden packet fixture or real-client verification yet. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionLocation.getLegionRanking` | `SmLegionDominionRank` ranking projection | Runtime Projection | Partial | Unit Tested | Partial Parity | Ports `removeNonEligibleLegions=false` order only. Territory winner eligibility threshold path remains unported. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionParticipantInfo` | `LegionDominionParticipantRow` / `LegionDominionRankEntry` | DTO / Packet Row | Partial | Unit Tested | Partial Parity | Includes legion id/name, points, time, and epoch seconds. Java in-memory legion-name lookup is represented by a repository join plus `"NOT AVAILABLE"` fallback. |
| `com.aionemu.gameserver.dao.LegionDominionDAO.loadParticipants` | `MySqlPlayerEnterWorldRepository.LoadLegionDominionParticipantsAsync` | Repository | Partial | Guarded Integration | Needs Verification | Filters by `legion_dominion_id` and maps points/time/date/name. Exact JDBC/MySQL timestamp timezone parity remains unverified. |

## Known Gaps

- Full `LegionDominionService` remains unported: in-memory locations, weekly calculation, reward mail, occupation updates, rift opening, and ranking broadcasts after instance finish are still missing.
- `SM_LEGION_DOMINION_LOC_INFO` is not ported.
- Exact MySQL/JDBC timestamp timezone equivalence for `participated_date` is not runtime-compared.
- No Java golden packet fixture was generated for `SM_LEGION_DOMINION_RANK`.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire the live `CM_LEGION` exOpcode `0x05` Brigade General transfer request path through Java-equivalent pending question state and packets.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION 0x05` currently has no C# live transfer request path; the next UOW would send the Java question/system packets and create pending response state for a real leadership-transfer interaction.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x05`, `LegionService.startBrigadeGeneralChangeProcess`, `LegionService.appointBrigadeGeneral(Player, Player)`, and relevant `LegionRestrictions.canAppointBrigadeGeneral` checks.
- C# runtime artifact to wire/fix: `CmLegion` parser coverage for exOpcode `0x05` if missing, `GameServerConnection.HandleLegionAsync` branch, existing `Player.ResponseRequester`/question-response model, and `SM_QUESTION_WINDOW`/`SM_SYSTEM_MESSAGE` send path.
- Client-visible/state/persistence effect expected: a Brigade General targeting an online same-legion member receives/sends the Java leadership-transfer prompt packets; invalid/missing/busy targets get the Java system-message response. If accept handling is safely scoped, target/current ranks should mutate and persist with `SM_LEGION_UPDATE_MEMBER` broadcasts.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live legion command, sends real server packets, and mutates live pending response state, with an optional follow-up rank persistence mutation if the accept path is included.

Focused validation recipe:
- Add parser test for `CM_LEGION 0x05` reading empty `D` plus character name.
- Add live handler tests for missing/offline target system message, busy target response, and happy-path question packets/pending request state.
- If accept-side rank mutation is included, add tests for former Brigade General demotion, new Brigade General promotion, `SaveLegionMemberRankAsync` for offline demotion if applicable, `SM_LEGION_UPDATE_MEMBER` broadcasts, `SM_LEGION_EDIT(0x08)`, and legion history `APPOINTED`.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for the question packet or leadership-transfer response path is discovered.

Risks to watch:
- Java's transfer request is two-stage: leader starts a request, target accepts, then ranks mutate. Do not claim full leadership-transfer parity if only the prompt path is wired.
- Java uses `World.getInstance().getPlayer(memberName)` for online target resolution. C# should use the connection registry's online-player lookup and keep name normalization consistent with other legion paths.
- Ensure existing question-response handlers can carry the requester/target payload before adding accept-side mutation.

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
