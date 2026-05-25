# Phase 6SD Completion Handoff - Staged Quest Start Source Loader

Date: May 25, 2026
Unit of Work: UOW-986
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-986] Add staged quest start source loader`)

## Status

Phase 6 is still in progress. This unit implements only an offline source loader that composes XML quest-script and Java handler quest-start extractor outputs.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No production `StaticData`/`DataManager` integration, Java handler execution, NPC-spawn population, delayed refresh scheduling, nearby-quest candidate filtering, `QuestService.checkStartConditions`, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartRegistrationSourceLoader.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceLoaderTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SD-Completion.md`

## What Changed

- Added `QuestNpcStartRegistrationSourceLoader`.
- Added `QuestNpcStartRegistrationSourceLoadResult`.
- The loader accepts optional XML quest-script and Java handler directories.
- XML files are fed through `QuestNpcStartXmlExtractor`.
- Java files are fed through `QuestNpcStartJavaHandlerExtractor`.
- Source files are processed recursively in stable ordinal file order.
- Unresolved Java handler rows are preserved beside resolved source registrations.
- Missing optional directories return empty results.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused source loader suite: passed, 3 tests.
- Full game-server suite: passed, 1681 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader` | Offline Loader / Source Aggregator | Partial | Unit Tested | Needs Verification | C# composes XML and Java handler source-extractor outputs over directories in stable order. It does not run Java reflection/JAXB, instantiate handlers, register into `QuestEngine`, model reload/unload behavior, or integrate with production `DataManager`. |
| `com.aionemu.gameserver.questEngine.handlers.models.XMLQuest` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader`; `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | XML Quest Loader Boundary | Partial | Unit Tested | Needs Verification | Loader includes XML extractor output, but remains offline. JAXB validation, XML model construction, template execution, serialization differences, and runtime registration ordering are not verified. |
| Representative `game-server/data/handlers/quest/**` classes extending `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader`; `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Loader | Partial | Unit Tested | Needs Verification | Loader includes resolved handler sources and unresolved rows, but does not execute Java classloading/reflection or guarantee full handler-tree coverage. Dynamic expressions and loops remain unresolved. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest NPC Registration / DTO | Partial | Unit Tested | Partial Parity | Loader outputs can feed staged table storage, but this unit does not populate runtime world instances or claim Java `HashSet` iteration order. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartRegistrationSourceLoaderTests.Load_ComposesXmlAndJavaHandlerExtractorOutputsInStableFileOrder` | Unit | Java `QuestEngine.init` loads XML and handler registrations before runtime use | Loader composes XML and Java handler extractor outputs from directories in stable file order. | Deterministic C# test over temp source files. | Does not run Java classloading/JAXB or production `DataManager`. |
| `QuestNpcStartRegistrationSourceLoaderTests.Load_ReportsJavaHandlerUnresolvedRowsAlongsideResolvedSources` | Unit | Java handler source extraction limitations | Unresolved handler rows are preserved beside resolved registrations. | Conservative loader behavior test. | Does not triage real handler-tree unresolved counts. |
| `QuestNpcStartRegistrationSourceLoaderTests.Load_MissingDirectoriesReturnEmptyResult` | Unit | Staged offline loader safety | Missing optional source directories do not crash the staged loader. | Deterministic C# test. | Production missing-data policy is not selected. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `QuestNpcStartRegistrationSourceLoader` is not integrated into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- No real-data audit has been run yet to summarize resolved/unresolved XML and Java handler registrations across the repository.
- The handler extractor intentionally leaves dynamic expressions unresolved; loops, collection-derived IDs, constructor parameters, inherited fields, method calls, nonliteral assignments, and comments/preprocessor-like edge cases need future triage before loader use.
- XML extraction remains partial and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 partial staged source loader in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 4 blocked/not-started categories, including real-data loader audit, production loader integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Run a real-data staged loader audit over representative `quest_script_data` and `data/handlers/quest` trees, record resolved/unresolved counts in docs, and keep production `StaticData`/`DataManager`, `QuestService.checkStartConditions`, player-controller sends, and ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Real-data staged loader audit | docs plus read-only source trees | Medium | Do not wire production startup. |
| B | Handler unresolved-case triage | read-only Java source plus docs | Medium | Safe if it only reports patterns and does not edit extractor code. |
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
