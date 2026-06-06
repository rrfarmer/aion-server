# Phase 6 Session 2761 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2761] Use dominion l10n in live legion selection

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/LegionDominionTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/Phase-6-Session-2761-Completion.md`
- `docs/Phase-6-Session-2761-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionDominionService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/LegionDominionLocationTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/L10n.java`
- `game-server/src/com/aionemu/gameserver/utils/ChatUtil.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/data/static_data/legion_dominion_template.xml`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~StaticData_LoadsLegionDominion" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 60
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionDominionService.initLocations` | `Aion.GameServer.Dataholders.StaticData` / `LegionDominionTable` | Static Data / Runtime Loading | Partial | Unit Tested | Partial Parity | Loads location id and name_id needed by live selection. It does not yet build the full in-memory location/participant service map, rewards, rifts, or occupation state. |
| `com.aionemu.gameserver.model.templates.LegionDominionLocationTemplate` | `LegionDominionLocationSummary` | DTO / Static Data | Partial | Unit Tested | Partial Parity | Only `id` and `name_id` are represented because those are used by the live packet path in this UOW. World, race, zone, rewards, and invasion rift remain unported. |
| `com.aionemu.gameserver.model.templates.L10n` | `LegionDominionLocationSummary.L10n` | Utility Contract | Partial | Unit Tested | Partial Parity | Uses existing C# `ChatUtil.L10n` to match Java encoded client string. Null id behavior remains in `ChatUtil` and was not broadened in this UOW. |
| `com.aionemu.gameserver.services.LegionService.joinLegionDominion` | `GameServerConnection.BroadcastLegionDominionJoinedAsync` | Live Handler Side Effect | Partial | Unit Tested | Partial Parity | Success branch now uses Java static-data l10n for the system message. Rank/persistence/fanout behavior came from UOW-2760. |
| `com.aionemu.gameserver.utils.ChatUtil.l10n` | `Aion.GameServer.Utils.ChatUtil.L10n` | Utility | Previously Ported / Reused | Unit Tested Through Caller | Partial Parity | This UOW reused the helper for dominion names; did not audit every ChatUtil method. |

## Known Gaps

- Full Java legion dominion static model is not ported: world id, race, zone, rewards, invasion rifts, occupation dates, and participant ranking state remain missing.
- `CM_LEGION_DOMINION_REQUEST_RANKING` remains deferred and has no `SM_LEGION_DOMINION_RANK` C# packet yet.
- C# does not yet maintain Java's in-memory `LegionDominionService` participant/location state beyond database-backed selection and static l10n lookup.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION_DOMINION_REQUEST_RANKING` for valid Stonespear ids `1..6` and send a real `SM_LEGION_DOMINION_RANK` packet from live code using the existing database shape and the static dominion table.

Runtime Progress Gate:
- Deferred/live behavior advanced: the currently deferred `CM_LEGION_DOMINION_REQUEST_RANKING` client packet would send a real server ranking packet.
- Java source of truth: `CM_LEGION_DOMINION_REQUEST_RANKING.runImpl`, `SM_LEGION_DOMINION_RANK.writeImpl`, `LegionDominionLocation.getLegionRanking(false)`, and `LegionDominionParticipantInfo`.
- C# runtime artifact to wire/fix: `GameServerConnection` branch for `CmLegionDominionRequestRanking`, new `SmLegionDominionRank`, repository load for `legion_dominion_participants` joined to legion names, and a small rank/top-25 projection.
- Client-visible/state/persistence effect expected: requesting a valid dominion id sends location id, requesting legion rank, top participant count, and participant point/time/date/name rows to the client.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred client packet path and sends a real server packet from live code using runtime database/static data.

Focused validation recipe:
- Add packet serialization tests for `SmLegionDominionRank` matching Java field order.
- Add live handler tests for invalid id no-op and valid id sending the packet.
- Add guarded DB integration coverage for participant/name row loading only if the repository method is added.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionDominion|FullyQualifiedName~SmLegionDominionRank|FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `SM_LEGION_DOMINION_RANK` is discovered or generated.

Risks to watch:
- Java `SM_LEGION_DOMINION_RANK` replaces the last top-25 row with the requesting legion when it is ranked outside the top 25. Preserve that behavior.
- Java date writes `writeQ(participant.getDate())`; inspect packet timestamp expectations before choosing C# `DateTimeOffset`/epoch shape.
- Do not implement final weekly reward distribution or Stonespear instance scoring in the ranking UOW; ranking packet send is a safe smaller slice.

Safe alternative runtime candidates:
- Wire `CM_LEGION` exOpcode `0x05` Brigade General transfer only if a small Java-equivalent live state/persistence path can be scoped safely.
- Extend `LegionDominionTable` with world/race/zone only if those fields are used by a live handler in the same UOW.
- Return to challenge quest-finish progress only after a live quest-finish socket/runtime hook is implemented; do not use planner placeholders as migration progress.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
