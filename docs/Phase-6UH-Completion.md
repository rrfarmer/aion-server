# Phase 6UH Completion - UOW-1042 Offline GP DAO Boundary

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UG-Completion.md`.

## Last Completed Unit

- UOW-1042: `[Phase 6][UOW-1042] Add offline GP DAO boundary`
- Status: ready to commit after validation and staging.

## Summary

UOW-1042 added the C# repository boundary for Java `AbyssRankDAO.addGp`. It captures the exact SQL text and parameter order used by Java for positive/stat-modifying GP and current-GP-only/clamped GP changes. Gameplay is not wired to this repository yet.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/AbyssRankRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AbyssRankRepositoryTests.cs`
- `docs/QuestGpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UH-Completion.md`

## What Changed

- Added `IAbyssRankRepository.AddGpAsync`.
- Added `EmptyAbyssRankRepository`.
- Added `MySqlAbyssRankRepository.AddGpAsync`.
- Added `AbyssRankGpUpdatePlan`.
- Positive/stat-modifying GP plan uses:
  - `UPDATE abyss_rank SET gp = gp + ?, daily_gp = daily_gp + ?, weekly_gp = weekly_gp + ? WHERE player_id = ?`
- Current-GP-only plan uses:
  - `UPDATE abyss_rank SET gp = GREATEST(gp + ?, 0) WHERE player_id = ?`
- `MySqlAbyssRankRepository` returns true when SQL execution completes, including zero affected rows, matching Java's source-reviewed lack of affected-row checks.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "AbyssRankRepositoryTests" --nologo` | Passed: 4 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1821 |

## Migration Parity Table - UOW-1042

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.AbyssRankDAO.addGp` | `Aion.GameServer.Data.AbyssRankGpUpdatePlan` | DAO SQL Plan | Partial | Unit Tested | Partial Parity | C# captures Java's two SQL shapes and parameter order: positive/stat-modifying GP updates `gp`, `daily_gp`, and `weekly_gp`; current-only changes use `GREATEST(gp + ?, 0)`. No DB integration, transaction/runtime comparison, missing-row execution, or gameplay caller wiring yet. |
| `com.aionemu.gameserver.dao.AbyssRankDAO.addGp` | `Aion.GameServer.Data.MySqlAbyssRankRepository.AddGpAsync` | Repository | Partial | No Integration Tests | Needs Verification | Repository executes the planned SQL and returns true when execution completes, including zero affected rows, matching Java's lack of row-count check by source review. Not wired into `GloryPointsService`; no MySQL integration test or Java runtime comparison. |
| `com.aionemu.gameserver.services.abyss.GloryPointsService.addGp` | `GloryPointsAddPlan.OfflineDaoUpdateRequired`; `IAbyssRankRepository.AddGpAsync` | Service/DAO Boundary | Partial | Unit Tested | Needs Verification | The offline branch now has a repository home, but `GloryPointsService` still only returns metadata for offline players. Live world lookup, offline execution, threading, logging side effects, and failure policy remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank.addGp` | `PlayerAbyssRank.AddGp`; `AbyssRankGpUpdatePlan` | Model/DAO Dependency | Partial | Unit Tested | Needs Verification | Online and offline GP changes now have separate C# model/SQL surfaces. Daily/weekly rollover, persistent-state flags, SQL overflow behavior, and consistency between online memory mutation and offline SQL remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AbyssRankRepositoryTests.AbyssRankGpUpdatePlan_UsesJavaPositiveStatsSqlAndParameterOrder` | Unit | `AbyssRankDAO.addGp`; `INCREASE_GP_QUERY_WITH_STATS` | Positive/stat-modifying GP SQL and parameter order. | Source-reviewed Java constants and binding order. | No DB execution or Java runtime capture. |
| `AbyssRankRepositoryTests.AbyssRankGpUpdatePlan_UsesJavaCurrentGpClampSqlWhenStatsAreNotModified` | Unit | `AbyssRankDAO.addGp`; `INCREASE_GP_QUERY` | Current-only GP SQL with `GREATEST(gp + ?, 0)` and parameter order for negative and zero values. | Source-reviewed Java constants and binding order. | Java service zero guard means zero normally should not reach DAO; test documents the repository boundary only. |
| `AbyssRankRepositoryTests.EmptyAbyssRankRepository_ReportsUnavailableMutationBoundary` | Unit | C# repository boundary | Empty repository reports unavailable mutation instead of pretending persistence happened. | C# safety boundary. | No Java equivalent; not a parity claim. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No MySQL integration test verifies affected-row behavior, SQL overflow/sign behavior, or `GREATEST` behavior against the actual schema.
- `GloryPointsService` is not wired to `IAbyssRankRepository`; offline GP execution remains disabled.
- Missing-row behavior is source-reviewed as no exception/no explicit check, but not runtime-verified.
- Java daily/weekly GP rollover and `AbyssRank` persistent-state flags remain unported.
- Threading and online/offline lookup behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 repository contract plus 1 deterministic DAO SQL plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 4 blocked/partial categories: Java runtime comparison, MySQL integration, offline GP service wiring, and daily/weekly rollover/persistence-state semantics
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit creates the offline GP DAO boundary without enabling live offline GP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `IAbyssRankRepository` behind an explicitly gated offline GP execution path.
- Why: `GloryPointsService` still reports `OfflineDaoUpdateRequired`; the repository exists but is not used by gameplay.
- Guardrails: keep quest finish reward mutation disabled; keep online GP helper behavior unchanged; document failure policy before live gameplay wires to the repository.

### Safe Alternative

- Task: Start quest XP helper audit/scaffold.
- Why: XP has broad side effects: rate precision, level-up, stats, skills, nearby quest refresh, and packets.
- Files: likely a dedicated `docs/QuestXpReward-Audit.md` plus small helper scaffolding only if the audit narrows risk.

## Do Not Parallelize

- `GloryPointsService.cs`: GP service boundary.
- `AbyssRankRepository.cs`: offline GP repository boundary.
- `QuestRewardService.cs`: AP/DP/kinah/GP helper surface.
- `QuestFinishOperationPlanService.cs`: shared quest-finish ordering.
- `docs/PHASE-6-PROGRESS.md` and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestGpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1042, `AbyssRankRepositoryTests`, `GloryPointsServiceTests`, and `QuestFinishOperationPlanServiceTests`.
