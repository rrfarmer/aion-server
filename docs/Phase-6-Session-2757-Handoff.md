# Phase 6 Session 2757 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2757] Wire live legion level up

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2757-Completion.md`
- `docs/Phase-6-Session-2757-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/services/ChallengeTaskService.java`
- `game-server/sql/aion_gs.sql`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 136
- Failed: 0
- Skipped: 0
- Existing warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for this service path.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | `0x0E` is now live; many other legion subactions remain partial or deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionLevelUpAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Level-up happy path and core restrictions are live. Java has a shared in-memory `Legion`; C# persists `legions.level` immediately to keep DB-backed runtime facts consistent. Challenge-task success path remains blocked. |
| `com.aionemu.gameserver.services.LegionService.LegionRestrictions` | `GameServerConnection.HandleLegionLevelUpAsync` | Service Logic | Partial | Unit Tested | Partial Parity | BG, max-level, challenge-task, Kinah, member-count, and contribution restrictions are covered. Exact Java ChallengeTaskService success behavior is not ported. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `GameServerOptions.GameServerLegionOptions` and `Player` legion fields | Model / Config | Partial | Unit Tested | Partial Parity | Level requirement tables are loaded from Java config keys and used by live code. Full Legion aggregate behavior is not ported. |
| `com.aionemu.gameserver.dao.LegionDAO` | `MySqlPlayerEnterWorldRepository.SaveLegionLevelUpMutationAsync` and `CountLegionMembersAsync` | Repository | Partial | Unit Tested | Needs Verification | Uses existing Java schema for `legions.level`, `inventory.item_count`, and `legion_members.legion_id`; no DB integration test was run in this UOW. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `LegionHistoryActions` | Enum Metadata | Partial | Unit Tested | Partial Parity | Added `LEVEL_UP` constant using existing Java id/type mapping. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Unit Tested | Partial Parity | Added reviewed level-up restriction/event helpers. |

## Known Gaps

- `ChallengeTaskService.canRaiseLegionLevel` is not ported; level 5+ raises are conservatively blocked when `gameserver.legion.task.requirement.enable` is true.
- No DB integration coverage for the level-up repository mutation yet.
- No real client validation was performed.
- The C# persistence timing differs from Java's in-memory Legion mutation plus later store behavior because C# lacks a shared Legion aggregate.

## Next Runtime UOW Candidate

Candidate:
- Port a minimal live `ChallengeTaskService.canRaiseLegionLevel` equivalent for the level 5+ legion level-up gate, backed by Java challenge-task data and `challenge_tasks`/`legion_members.challenge_score` state if those structures are already loadable.

Runtime Progress Gate:
- Deferred/live behavior advanced: level 5+ `CM_LEGION 0x0E` requests would stop being unconditionally blocked and would execute Java-equivalent challenge-task completion checks before level-up.
- Java source of truth: `ChallengeTaskService.canRaiseLegionLevel`, `ChallengeTasksDAO.load`, `ChallengeTask.isCompleted`, `ChallengeTaskTemplate.isLegionLevelTask`, `ChallengeTaskTemplate.getMinLevel`, and `LegionRestrictions.canChangeLevel`.
- C# runtime artifact to wire/fix: add or locate runtime challenge-task loading structures, repository reads for legion challenge tasks, and replace the conservative `GuildLevelUpChallengeTask` block in `HandleLegionLevelUpAsync` with the real check.
- Client-visible/state/persistence effect expected: eligible level 5+ legions can level up and receive the existing level-up packets/history; ineligible legions still receive `904452`.
- Why this is not preview-only/test-only/documentation-only: it would unblock an already-live level-up branch using runtime DB/static-data state and change whether live packets/state mutations occur.

Focused validation recipe:
- Specific behavior/contract: level 5+ challenge-task gate returns false when no required completed legion-level tasks exist and true when all Java-required tasks are completed, allowing the live level-up path.
- C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~Challenge" --logger "console;verbosity=minimal" --no-restore`; narrow to the edited challenge-task test class plus `CmLegionTests` after discovery.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live dispatch/state/persistence branch is affected; start focused.

Risks to watch:
- Do not add challenge-task preview/planner scaffolding as a UOW. Only proceed if the static data and DB shape can be used by live `CM_LEGION 0x0E`.
- The Java service also handles task list packets and quest-finish updates; keep the first UOW scoped to `canRaiseLegionLevel` only if that can be done safely.
- If challenge-task runtime data is too large to port safely, choose another live `CM_LEGION` branch rather than doing evidence-only hardening.

Safe alternative runtime candidates:
- Wire another deferred `CM_LEGION` subaction only if it has Java source, a live packet/state/persistence effect, and focused validation.
- Add a narrow live repository integration UOW only if it directly protects the already-live level-up persistence path and includes a runtime DB effect, not just test-only coverage.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
