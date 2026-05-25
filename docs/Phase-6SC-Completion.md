# Phase 6SC Completion Handoff - Java Handler Quest Start Extractor

Date: May 25, 2026
Unit of Work: UOW-985
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-985] Extract Java handler quest start registrations`)

## Status

Phase 6 is still in progress. This unit implements only an offline Java handler source extractor for a conservative subset of direct quest-start NPC registrations.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No extractor loader integration, Java handler execution, `StaticData`/`DataManager` integration, NPC-spawn population, delayed refresh scheduling, nearby-quest candidate filtering, `QuestService.checkStartConditions`, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartJavaHandlerExtractorTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SC-Completion.md`

## What Changed

- Added `QuestNpcStartJavaHandlerExtractor`.
- Added extraction result and unresolved-registration DTOs.
- Extracts direct `registerQuestNpc(...).addOnQuestStart(...)` calls when expressions resolve to:
  - integer literals
  - simple `int` assignments
  - `int[]` indexes with literal indexes, such as `npcIds[0]`
  - inherited `questId` through `super(1234)`
- Reports unsupported expressions as unresolved rows instead of guessing.
- Added tests for literal extraction, constants/arrays, unresolved expressions, and staged table population.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java handler source extractor | Representative Java quest handlers and `AbstractQuestHandler` | `QuestNpcStartJavaHandlerExtractor.cs`, tests | Implementation / Tests | No write parallelism in this unit | Medium | Completed by orchestrator; isolated new files but shared docs. |
| B | Staged XML/handler loader integration | XML scripts plus Java handler tree | new loader/test files | Implementation / Tests | Future isolated unit only | Medium | Next likely step; avoid concurrent edits to extractor abstractions. |
| C | Side-effect persistence docs | AP rank equipment/abyss skill persistence paths | docs/read-only first | Analysis | Yes if read-only | Medium | Safe future sidecar candidate. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | tooling/docs | Analysis / Tooling | Only if tooling exists | Medium | Still blocked locally by Java 8/Maven gap. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused Java handler extractor suite: passed, 4 tests.
- Full game-server suite: passed, 1678 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| Representative `game-server/data/handlers/quest/**` classes extending `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extractor | Partial | Unit Tested | Needs Verification | C# extracts direct `registerQuestNpc(...).addOnQuestStart(...)` calls from source text for a limited expression subset. It does not execute handlers, run Java reflection/classloading, handle all Java syntax, or prove coverage across the full handler tree. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Base Handler / Quest Id Source | Partial | Unit Tested | Partial Parity | C# resolves `questId` only from literal `super(id)` constructor calls. Constructor parameters, superclass alternatives, dynamic quest ids, reflection lifecycle, and runtime registration ordering remain unmodeled. |
| `com.aionemu.gameserver.questEngine.QuestEngine.registerQuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest Engine / Handler Registration Source | Partial | Unit Tested | Partial Parity | C# recognizes direct handler registration calls and emits source metadata. Other Java quest registration maps/lists, talk/kill events, range overloads, reload/unload behavior, and threading remain unported. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest NPC Registration / DTO | Partial | Unit Tested | Partial Parity | Extracted handler sources can populate the staged table and duplicate-collapsing start-id storage. Runtime population into `WorldMapInstance` and Java `HashSet` ordering are not claimed. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartJavaHandlerExtractorTests.ExtractsLiteralNpcIdWithInheritedQuestId` | Unit | Representative Java handler `super(questId)` plus direct `registerQuestNpc(literal).addOnQuestStart(questId)` | Literal NPC ids and inherited `questId` extraction. | Deterministic C# test from source-reviewed Java handler shape. | No runtime Java handler loading. |
| `QuestNpcStartJavaHandlerExtractorTests.ExtractsScalarConstantsAndArrayIndexes` | Unit | Java handlers using `START_NPC_ID`, `questStartNpcId`, and `npcIds[0]` | Simple integer assignment and array-index resolution. | Deterministic C# test from source-reviewed handler patterns. | Does not cover loops, computed indexes, or all Java declaration forms. |
| `QuestNpcStartJavaHandlerExtractorTests.ReportsUnsupportedExpressionsInsteadOfGuessing` | Unit | Dynamic Java registration expressions | Unsupported NPC/quest expressions are reported as unresolved rows. | Conservative parser behavior test. | Does not enumerate every unsupported shape in the Java tree. |
| `QuestNpcStartJavaHandlerExtractorTests.ExtractedHandlerSourcesCanPopulateQuestNpcStartTable` | Unit | Java handler start registration feeding `QuestNpc.addOnQuestStart` | Extracted handler sources can populate the staged start table. | C# integration-style unit test for extractor/table boundary. | No runtime world-instance population. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `QuestNpcStartJavaHandlerExtractor` is not integrated into `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- The handler extractor intentionally leaves dynamic expressions unresolved; loops, collection-derived IDs, constructor parameters, inherited fields, method calls, nonliteral assignments, and comments/preprocessor-like edge cases need future triage before loader use.
- XML extraction is still offline and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 partial Java handler source extractor in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 4 blocked/not-started categories, including extractor loader integration, unresolved handler-expression triage, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Implement a staged quest-start registration loader that composes XML and Java handler extractor outputs for representative source directories/files and reports unresolved handler registrations; keep runtime handler execution, `QuestService.checkStartConditions`, player-controller sends, and production ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Staged XML/handler loader implementation | new loader/test files only | Medium | Do not edit `StaticData`, startup wiring, or extractor parsing behavior in parallel. |
| B | Java handler unresolved-case audit | read-only Java source plus docs | Medium | Safe if it only reports patterns and does not edit extractor code. |
| C | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing quest-start table, extractor shared parsing behavior, `StaticData`, `DataManager`, packet files, world runtime files, or Phase 6 docs.
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
