# Phase 6 Session 2760 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2760] Wire live legion dominion selection

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2760-Completion.md`
- `docs/Phase-6-Session-2760-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionDominionService.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionLocation.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDominionDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SaveLegionCurrentDominionAsync_WritesJavaLegionDominionColumn_WhenEnabled|FullyQualifiedName~TryAddLegionDominionParticipantAsync_InsertsOnceLikeJavaLocationJoin_WhenEnabled" --logger "console;verbosity=minimal" --no-restore
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` case `0x10` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Dominion selection branch is live. Legion create/invite/brigade-general transfer/dominion ranking remain outside this UOW. |
| `com.aionemu.gameserver.services.LegionService.joinLegionDominion` | `HandleLegionDominionJoinAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Rank/current-dominion/join/store/fanout path is ported. Java location l10n lookup is not yet backed by C# static dominion data. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionLocation.join` | `TryAddLegionDominionParticipantAsync` | Persistence / Runtime State | Partial | Guarded DB Test Added | Needs Verification | Inserts only when participant is not already present. No in-memory dominion service map is maintained yet. |
| `com.aionemu.gameserver.dao.LegionDAO.storeLegion` | `SaveLegionCurrentDominionAsync` | Repository | Partial | Guarded DB Test Added | Needs Verification | This scoped method persists current dominion only, not every `storeLegion` column. |
| `SM_SYSTEM_MESSAGE.STR_MSG_GUILD_APPLY_DOMINION` | `SmSystemMessage.MsgGuildApplyDominion` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Message id/parameter count match. Parameter currently uses dominion id string instead of Java `LegionDominionLocation.getL10n()`. |
| `SM_LEGION_INFO` | `SmLegionInfo.FromPlayer` | Server Packet | Previously Ported / Reused | Unit Tested | Partial Parity | This UOW reuses and sends the packet after mutating current dominion. |

## Known Gaps

- Dominion message parameter uses the selected dominion id string; Java uses `LegionDominionLocation.getL10n()` from `legion_dominion_template.xml`.
- C# does not yet load Java legion dominion static data into a runtime dominion service map.
- C# does not yet maintain Java's in-memory `LegionDominionService` participant/location state beyond the database-backed selection path.
- DB integration tests were not executed against MySQL in this session because `AION_GAMESERVER_DB_INTEGRATION` was not enabled.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists. Current socket-bound quest finish remains guarded/planner-only.

## Next Runtime UOW Candidate

Candidate:
- Load Java `legion_dominion_template.xml` into C# runtime static data and use it in the live `CM_LEGION 0x10` branch so `STR_MSG_GUILD_APPLY_DOMINION` receives Java's encoded l10n string instead of the fallback dominion id.

Runtime Progress Gate:
- Deferred/live behavior advanced: the live dominion-selection packet path would use Java static data at runtime for its client-visible message parameter.
- Java source of truth: `LegionDominionService.initLocations`, `LegionDominionLocationTemplate.getL10nId`, `L10n.getL10n`, and `LegionService.joinLegionDominion`.
- C# runtime artifact to wire/fix: `StaticData` legion dominion location table plus `GameServerConnection.BroadcastLegionDominionJoinedAsync` lookup.
- Client-visible/state/persistence effect expected: successful live dominion selection sends `SM_SYSTEM_MESSAGE(1402902)` with the same encoded location l10n parameter Java uses.
- Why this is not preview-only/test-only/documentation-only: it loads Java XML/static data into runtime C# structures consumed by a live packet path and changes a real server packet emitted by live code.

Focused validation recipe:
- Add/extend static-data loading tests for `legion_dominion_template.xml`.
- Extend `CmLegionTests.HandleInfrastructurePacketAsync_DominionJoinPersistsStateAndBroadcastsInfoLikeJava` to inject runtime static dominion data and assert the encoded l10n parameter, while keeping a fallback test if no table is present.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~StaticData_LoadsLegionDominion" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `L10n.getL10n` or dominion dataholder behavior is discovered.

Risks to watch:
- Do not turn this into a static-data metadata-only UOW. The loaded table must be used by the live `CM_LEGION 0x10` handler in the same unit.
- Preserve the current persistence/fanout behavior from UOW-2760.
- If the static loader shape becomes too broad, keep only the Java fields needed by live dominion selection: `id` and `name_id`.

Safe alternative runtime candidates:
- Wire `CM_LEGION` exOpcode `0x05` Brigade General transfer only if a small Java-equivalent live state/persistence path can be scoped safely.
- Wire `CM_LEGION_DOMINION_REQUEST_RANKING` only if participant/ranking data can be loaded from existing schema and a real `SM_LEGION_DOMINION_RANK` packet can be sent.
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
