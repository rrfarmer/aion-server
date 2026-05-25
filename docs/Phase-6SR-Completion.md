# Phase 6SR Completion - Quest Reward-Group Hydration

Date: May 25, 2026
Unit of Work: UOW-1000
Commit message: `[Phase 6][UOW-1000] Hydrate quest reward groups`

## Session Summary

This unit closed a narrow repository-load gap created by the staged XML `finished reward` predicate work. Java `PlayerQuestListDAO.load` reads nullable `player_quests.reward` into `QuestState.rewardGroup`; C# now selects the same column into `PlayerQuestState.RewardGroup`.

Production nearby quest sends, `CM_LEVEL_READY` nearby integration, NPC-spawn delayed refresh, production player-controller refresh, and production ItemPurification dispatch remain disabled.

## Completed Work

- Updated `MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` to select `reward` from `player_quests`.
- Hydrates nullable `reward` into `PlayerQuestState.RewardGroup`.
- Added `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAgainstJavaSchema_WhenEnabled`.
- Updated Phase 6 parity docs and the latest handoff.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests"` passed with 11 tests in the normal non-DB environment.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1707 tests.

Note: the DB integration assertion is gated by `AION_GAMESERVER_DB_INTEGRATION=1`; in the normal local run it compiles and returns early.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SR-Completion.md`

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.load` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` | Repository / Quest State Hydration | Partial | Integration Tested when DB flag enabled | Partial Parity | C# now selects nullable `player_quests.reward` and hydrates `PlayerQuestState.RewardGroup`, matching Java's reward-group load shape. The integration test is gated by `AION_GAMESERVER_DB_INTEGRATION=1`; normal local runs compile the test and return early. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | DTO / Quest State | Partial | Unit Tested; Integration Tested when DB flag enabled | Partial Parity | Reward group is now available to staged XML `finished reward` checks for repository-loaded quest states. Next-repeat time, complete time, persistence/update behavior, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition`; `com.aionemu.gameserver.model.templates.quest.FinishedQuestCond` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `Aion.GameServer.Dataholders.NearbyQuestFinishedCondition` | Predicate Consumer | Partial | Unit Tested | Partial Parity | No predicate logic changed in this unit; the existing UOW-999 reward-gated `finished` check now has a repository-hydrated source field. Live nearby sends remain disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAgainstJavaSchema_WhenEnabled` | Integration | Java `PlayerQuestListDAO.SELECT_QUERY` and `QuestState.rewardGroup` | Validates nullable `player_quests.reward` hydrates to `PlayerQuestState.RewardGroup` against the Java schema when DB integration is enabled. | Gated DB integration assertion over `game-server/sql/aion_gs.sql`. | Skips unless `AION_GAMESERVER_DB_INTEGRATION=1`; next-repeat and complete-time remain unmodeled. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The new DB integration assertion is gated and did not execute against a live DB in the normal local run.
- Next-repeat time and complete time remain unhydrated, so time-based repeat parity is still blocked.
- Quest-state persistence/update for reward groups was not changed or verified.
- Packet sends, `CM_LEVEL_READY`, NPC-spawn delayed refresh, production player-controller refresh, and ItemPurification dispatch remain disabled.
- Inventory item preconditions, combine-skill checks, and NPC faction checks remain unsupported.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 1 repository hydration field in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories: next-repeat hydration, complete-time hydration, reward-group persistence/update verification, and live nearby send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit closes the repository-load gap for XML reward-group checks without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add the next narrow nearby predicate dependency: inventory item preconditions, combine-skill checks, NPC faction checks, repeat timing, or broader refresh-plan audits across representative player archetypes.

Keep packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Add staged inventory item preconditions for nearby start conditions, or source-audit repeat timing before implementing it.
- Why: XML reward-gated prerequisites now have staged and repository-loaded reward group support. The next blocker is another predicate dependency, not send wiring.
- Files: `NearbyQuestTemplateTable`, `NearbyQuestTemplateXmlExtractor`, `NearbyQuestStartConditionService`, focused tests, and docs for inventory; or docs/read-only Java source for repeat timing.
- Guardrail: Do not wire live sends or production ItemPurification dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Inventory precondition staged design | docs/read-only Java plus staged dataholder tests | Medium | Avoid if another task edits central predicate files. |
| B | Repeat timing source audit | Java source and docs only | Low | Read-only candidate. |
| C | Broader refresh-plan archetype audit | audit tests/docs | Medium | Avoid central predicate edits in parallel. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement one selected predicate slice and update shared docs | selected service/dataholder/test files, Phase 6 docs | Production send paths unless this becomes the sole owner |
| Agent A | Read-only repeat-timing or inventory predicate checklist | Java source and docs only | All writes |

If no sub-agent tool is available, do the recommended task sequentially.

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestTemplateXmlExtractor.cs`: central staged extractor; one owner at a time.
- `NearbyQuestTemplateTable.cs`: central staged template shape; one owner at a time.
- `PlayerQuestState.cs`: shared quest-state DTO; one owner at a time.
- `PlayerEnterWorldRepository.cs`: repository load path; one owner at a time.
- `GameServerConnection.cs`: production send path; one owner only.
- `GameClientSocketServer.cs`: connection registry/send infrastructure; one owner only.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6SR-Completion.md`, `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, `docs/QuestStartConditions-Nearby-Audit.md`, and `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- `docs/commit-conventions.md` is still missing; use the commit format in `docs/orchestration-rules.md`.
- Production `CM_ITEM_PURIFICATION` dispatch and real nearby-refresh packet sends must remain disabled.
- The next strongest nearby quest move is another staged predicate dependency, not live sends.
