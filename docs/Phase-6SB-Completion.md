# Phase 6SB Completion Handoff - XML Quest Start Registration Extractor

Date: May 25, 2026
Unit of Work: UOW-984
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-984] Extract XML quest start registrations`)

## Status

Phase 6 is still in progress. This unit implements only an offline XML quest-script extractor that emits staged `QuestNpcStartRegistrationSource` rows from `start_npc_ids` attributes.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No Java handler source extractor, `StaticData`/`DataManager` loader integration, NPC-spawn population, delayed refresh scheduling, nearby-quest candidate filtering, `QuestService.checkStartConditions`, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartXmlExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartXmlExtractorTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SB-Completion.md`

## What Changed

- Added `QuestNpcStartXmlExtractor`.
- Extracts XML elements with `id` and `start_npc_ids` into `QuestNpcStartRegistrationSource` records.
- Uses Java/JAXB-style whitespace-separated integer parsing, including multiline XML attributes.
- Sets `SourceKind` to `XmlQuest` and preserves the supplied source path.
- Mirrors Java `ReportToMany.register` by skipping NPC start registration for `report_to_many` when `start_item_id` is nonzero.
- Added tests proving extraction, suppression, table population, and stream-input behavior.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | XML quest-script extractor | XML quest models and template `register()` methods | `QuestNpcStartXmlExtractor.cs`, `QuestNpcStartXmlExtractorTests.cs` | Implementation / Tests | No write parallelism in this unit | Medium | Completed by orchestrator; isolated new files but shared docs. |
| B | Java handler-source extractor | Java quest handlers using `addOnQuestStart` | new extractor/test files | Implementation / Tests | Yes in a future isolated unit | Medium | Still needed; avoid concurrent edits to shared extractor abstractions. |
| C | Side-effect persistence docs | AP rank equipment/abyss skill persistence paths | docs/read-only first | Analysis | Yes if read-only | Medium | Safe future sidecar candidate. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | tooling/docs | Analysis / Tooling | Only if tooling exists | Medium | Still blocked locally by Java 8/Maven gap. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartXmlExtractorTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused XML extractor suite: passed, 4 tests.
- Full game-server suite: passed, 1674 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.models.XMLQuest` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | XML Quest Loader Boundary | Partial | Unit Tested | Needs Verification | C# source-parses quest-script XML attributes into staged registration sources. It does not instantiate Java template handlers, run JAXB, process all model fields, execute `register(QuestEngine)`, or integrate with `QuestEngine.init`. |
| `com.aionemu.gameserver.questEngine.handlers.models.ReportToManyData` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | XML DTO / Template Input | Partial | Unit Tested | Partial Parity | C# reads `id`, `start_npc_ids`, and `start_item_id` only. Other fields such as `npcInfos`, `startDialogId`, mission flags, work items, and JAXB validation behavior remain unmodeled. |
| `com.aionemu.gameserver.questEngine.handlers.template.ReportToMany` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | Template / Quest Registration | Partial | Unit Tested | Partial Parity | C# mirrors only the `startItemId != 0` branch that suppresses `addOnQuestStart`. Java item-start registration, talk events, item-use handling, dialogs, reward flow, threading, and runtime handler side effects are not ported here. |
| `com.aionemu.gameserver.questEngine.handlers.template.ReportTo` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | Template / Quest Registration | Partial | Unit Tested | Partial Parity | C# emits sources for `start_npc_ids`, matching the reviewed `register()` start loop shape. End NPC talk events, dialog behavior, work-item checks, and Java runtime handler execution remain unported. |
| `com.aionemu.gameserver.questEngine.handlers.template.WorkOrders` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | Template / Quest Registration | Partial | Unit Tested | Partial Parity | C# emits sources for whitespace-separated `start_npc_ids`, matching the reviewed start loop shape. Recipe validation, component grants, reward deletion, and talk events remain unported. |
| `com.aionemu.gameserver.questEngine.handlers.template.MonsterHunt` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | Template / Quest Registration | Partial | Unit Tested | Partial Parity | C# emits sources for `start_npc_ids` only. `aggro_start_npc_ids`, kill/talk/end NPC registration, enter-world/zone/distance starts, faction special cases, and runtime handler behavior remain unported. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest NPC Registration / DTO | Partial | Unit Tested | Partial Parity | Extracted XML sources can populate the staged table and duplicate-collapsing start-id storage. Runtime population into `WorldMapInstance` and Java `HashSet` ordering are not claimed. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartXmlExtractorTests.ExtractsStartNpcIdsFromXmlQuestTemplates` | Unit | Java XML template `register` methods including `ReportTo`, `WorkOrders`, and `MonsterHunt` | XML `start_npc_ids` values produce one source per NPC id, including multiline whitespace-separated lists. | Deterministic test from source-reviewed Java template registration loops and JAXB list shape. | No Java runtime/JAXB comparison; not loader-wired. |
| `QuestNpcStartXmlExtractorTests.ReportToManyWithStartItemIdSkipsNpcStartRegistrationLikeJava` | Unit | Java `ReportToMany.register` | Nonzero `start_item_id` suppresses NPC start registration, while `0` or absent item id emits NPC starts. | Deterministic test from source-reviewed Java branch. | Does not model quest item registration. |
| `QuestNpcStartXmlExtractorTests.ExtractedSourcesCanPopulateQuestNpcStartTable` | Unit | Java XML start registrations feeding `QuestNpc.addOnQuestStart` | Extracted XML sources can populate the staged start table. | C# integration-style unit test for extractor/table boundary. | No runtime world-instance population. |
| `QuestNpcStartXmlExtractorTests.ExtractFromStreamUsesSameXmlAttributeRules` | Unit | Java XML quest-script file loading | Stream input uses the same attribute parsing behavior. | Deterministic C# test for loader-friendly input. | No directory scan or `DataManager` integration. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `QuestNpcStartXmlExtractor` is not integrated into `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- No Java handler source extractor exists yet; constants, arrays, inherited `questId`, `_questId`, loop-derived ids, and unresolved expressions need conservative handling.
- XML extraction does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 partial XML quest-start extractor in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7
- Total blocked artifacts: 4 blocked/not-started categories, including Java handler extraction, XML loader integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Implement a conservative Java handler source extractor for direct `registerQuestNpc(...).addOnQuestStart(...)` patterns, emitting `QuestNpcStartRegistrationSource` rows plus explicit unresolved cases; keep runtime handler execution, `QuestService.checkStartConditions`, player-controller sends, and production ItemPurification dispatch disabled.

Alternative safe task:
- Integrate the XML extractor into a staged directory loader/test that reads representative quest-script XML files without wiring production `StaticData`/`DataManager`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java handler extractor analysis/implementation | new extractor/test files only | Medium | Do not edit `StaticData`, startup wiring, or XML extractor abstractions in parallel. |
| B | XML extractor staged loader integration | new loader/test files only | Medium | Safe if kept separate from Java source parser and production `DataManager`. |
| C | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing quest-start table, XML extractor shared parsing behavior, `StaticData`, `DataManager`, packet files, world runtime files, or Phase 6 docs.
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
