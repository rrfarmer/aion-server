# Phase 6SE Completion Handoff - Quest Start Real-Data Loader Audit

Date: May 25, 2026
Unit of Work: UOW-987
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-987] Audit real quest start source data`)

## Status

Phase 6 is still in progress. This unit adds an offline real-data regression audit for the staged quest-start source loader.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No production `StaticData`/`DataManager` integration, Java handler execution, NPC-spawn population, delayed refresh scheduling, nearby-quest candidate filtering, `QuestService.checkStartConditions`, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`
- `docs/QuestNpcStart-RealData-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SE-Completion.md`

## What Changed

- Added `QuestNpcStartRegistrationSourceRealDataAuditTests`.
- The audit runs `QuestNpcStartRegistrationSourceLoader` over:
  - `game-server/data/static_data/quest_script_data`
  - `game-server/data/handlers/quest`
- Pinned current staged-loader counts:
  - total resolved staged start sources: 5184
  - XML quest-script sources: 4400
  - Java handler sources: 784
  - unresolved Java handler registrations: 6
  - distinct NPC ids: 1668
  - distinct quest ids: 4497
- Verified the XML `report_to_many start_item_id` suppression still excludes quest `2274` / NPC `203622`.
- Added `docs/QuestNpcStart-RealData-Audit.md` with the same counts and unresolved-handler list.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused real-data source-loader audit: passed, 1 test.
- Full game-server suite: passed, 1682 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader`; `Aion.GameServer.Tests.QuestNpcStartRegistrationSourceRealDataAuditTests` | Offline Loader / Source Aggregator | Partial | Regression Tested | Needs Verification | Real repository-data audit resolves 5184 staged sources and preserves 6 unresolved handler rows. This does not run Java reflection/JAXB, instantiate handlers, register into `QuestEngine`, model reload/unload behavior, or integrate with production `DataManager`. |
| `com.aionemu.gameserver.questEngine.handlers.models.XMLQuest` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader` | XML Quest Loader Boundary | Partial | Regression Tested | Needs Verification | Real-data audit resolves 4400 XML `start_npc_ids` sources. JAXB validation, XML model construction, template execution, serialization differences, and runtime registration ordering are not verified. |
| Representative `game-server/data/handlers/quest/**` classes extending `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader` | Java Handler Source Loader | Partial | Regression Tested | Needs Verification | Real-data audit resolves 784 Java handler sources and identifies 6 unresolved `butlerId` registrations. Java classloading/reflection is not executed and dynamic expressions remain unresolved unless explicitly supported. |
| `game-server/data/handlers/quest/oriel/*` and `game-server/data/handlers/quest/pernon/*` butler start handlers | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor`; `Aion.GameServer.Tests.QuestNpcStartRegistrationSourceRealDataAuditTests` | Java Handler Source Audit | Not Started | Regression Tested | Needs Verification | Six `butlerId` registrations are unresolved. The value is intentionally not guessed; Java housing/butler source must be inspected before adding support. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest NPC Registration / DTO | Partial | Regression Tested | Partial Parity | Audit proves resolved source records are positive IDs and can be staged. It still does not populate runtime world instances or claim Java `HashSet` iteration order. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_LoadsStagedQuestStartSourcesWithoutProductionWiring` | Regression | Real repository Java/XML quest-start source data | Pins current staged-loader counts: 5184 total sources, 4400 XML, 784 Java handler, 6 unresolved handler registrations, 1668 distinct NPC ids, and 4497 distinct quest ids. Also verifies the `report_to_many start_item_id` suppression still excludes quest 2274/NPC 203622. | Deterministic C# audit over current repository source files. | Does not run Java reflection/JAXB, execute handlers, populate production runtime state, or prove runtime parity. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged source loader and audit are not integrated into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- Six `butlerId` handler registrations remain unresolved and need Java housing/butler source inspection.
- The handler extractor intentionally leaves dynamic expressions unresolved; loops, collection-derived IDs, constructor parameters, inherited fields, method calls, nonliteral assignments, and comments/preprocessor-like edge cases need future triage before production loader use.
- XML extraction remains partial and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 0 new production artifacts in this unit; 1 real-data regression audit added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including `butlerId` handler resolution, production loader integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Inspect the Java Oriel/Pernon housing/butler quest pattern and resolve or explicitly classify the six `butlerId` handler registrations, adding a conservative extractor rule only if Java makes the value deterministic; keep production `StaticData`/`DataManager`, `QuestService.checkStartConditions`, player-controller sends, and ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `butlerId` handler source analysis | read-only Java source plus focused extractor/test files if deterministic | Medium | Do not wire production startup. |
| B | Handler unresolved-case broader audit | read-only Java source plus docs | Medium | Safe if it only reports patterns and does not edit extractor code. |
| C | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing quest-start table, extractor/loader parsing behavior, `StaticData`, `DataManager`, packet files, world runtime files, or Phase 6 docs.
- Production `CM_ITEM_PURIFICATION` automatic dispatch with quest callback work.
- Real dynamic quest handler invocation with nearby-refresh work.
- ItemPurification persistence contract changes across repository/service/test files until ownership is narrowed.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
5. Run focused and full tests for any C# code changes.
6. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
7. Create the next handoff and commit the completed unit.
