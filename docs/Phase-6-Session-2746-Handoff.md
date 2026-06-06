# Phase 6 Session 2746 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2746] Persist live legion announcement edits

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2746-Completion.md`
- `docs/Phase-6-Session-2746-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/sql/aion_gs.sql`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema|FullyQualifiedName~SaveLegionAnnouncementAsync_ReplacesAndClearsAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 19
- Failed: 0
- Skipped: 0
- Existing warnings only.

Hygiene:

```powershell
git diff --check
```

Result:
- Passed with normal CRLF warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet/DAO path.

DB integration:
- DB-gated tests compiled but did not execute against live MySQL because `AION_GAMESERVER_DB_INTEGRATION` was not set to `1`.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcodes `0x07`, `0x08`, and `0x09` are live; other subactions remain deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionAnnouncementChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Core announcement change/clear behavior live; fanout missing. |
| `com.aionemu.gameserver.dao.LegionDAO` | `IPlayerEnterWorldRepository.SaveLegionAnnouncementAsync` / `MySqlPlayerEnterWorldRepository.SaveLegionAnnouncementAsync` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Delete/optional-insert shape ported; live MySQL execution not run. |
| `com.aionemu.gameserver.model.team.legion.LegionPermissionsMask` | `GameServerConnection.LegionEditPermission` and loaded player permission masks | Permission Logic | Partial | Unit Tested | Partial Parity | EDIT mask `0x200` enforced; broader enum remains only represented where needed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added no-right/success/clear helpers. |

## Known Gaps

- Online legion-member fanout remains missing for announcement set/clear.
- C# still lacks Java's shared `Legion` aggregate; active player snapshot is updated directly.
- DB-gated persistence was not executed against live MySQL this session.
- Overlong announcement warning logging is not ported.
- MySQL timestamp timezone behavior remains Needs Verification.

## Next Runtime UOW Candidate

Candidate:
- Fill more live `SM_LEGION_INFO` fields from existing `legions` columns: contribution points and occupied/last/current dominion ids.

Runtime Progress Gate:
- Deferred/live behavior advanced: existing live `CM_LEGION` exOpcode `0x08` would send more complete Java-equivalent legion info instead of defaulting runtime fields to zero.
- Java source of truth: `SM_LEGION_INFO.writeImpl`, `LegionDAO.loadLegion`, `Legion.getContributionPoints`, `getOccupiedLegionDominion`, `getLastLegionDominion`, and `getCurrentLegionDominion`.
- C# runtime artifact to wire/fix: `Player` legion contribution/dominion fields, `MySqlPlayerEnterWorldRepository.LoadPlayerAsync`, `SmLegionInfo.FromPlayer`, and focused packet/repository tests.
- Client-visible/state/runtime-loading effect expected: live legion info refresh returns contribution points and dominion ids loaded from the existing `legions` schema.
- Why this is not preview-only/test-only/documentation-only: it loads Java schema/static runtime data into live C# player state used by a live server packet path.

Focused validation recipe:
- Specific behavior/contract: player hydration reads `legions.contribution_points`, `occupied_legion_dominion`, `last_legion_dominion`, and `current_legion_dominion`; `SmLegionInfo.FromPlayer` writes those values in Java packet order.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLegionLevelForTradeListFilteringAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore`
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: repository hydration and live server packet population.
- Broad .NET decision: start focused; do not run unfiltered project/solution validation unless focused evidence exposes wider repository or packet risk.

Safe alternative candidates:
- Wire another `CM_LEGION` subaction only when it has Java source, active C# runtime state, and a client-visible packet/state/persistence effect.
- Investigate online legion-member fanout only after confirming the current C# connection registry can safely enumerate active players by legion id without inventing a separate shared aggregate.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
