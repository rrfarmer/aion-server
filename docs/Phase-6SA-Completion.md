# Phase 6SA Completion Handoff - Nearby Quest Start Registration Table

Date: May 25, 2026
Unit of Work: UOW-983
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-983] Add nearby quest start registration table`)

## Status

Phase 6 is still in progress. This unit implements only the staged in-memory `QuestNpc.onQuestStart` registration table prerequisite for the nearby-quest refresh path.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No Java handler execution, Java/XML extractor, `StaticData` integration, NPC-spawn population, delayed refresh scheduling, candidate filtering, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartTableTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SA-Completion.md`

## What Changed

- Added `QuestNpcStartTable`.
- Added `QuestNpcStartRegistration`.
- Added `QuestNpcStartRegistrationSource` and `QuestNpcStartRegistrationSourceKind` for future Java handler/XML/manual source metadata.
- Matched narrow Java behavior:
  - `QuestEngine.registerQuestNpc` creates or reuses a registration.
  - `QuestEngine.getQuestNpc` returns an empty non-stored registration when missing.
  - `QuestNpc.addOnQuestStart` stores each quest id once.
  - Re-registering an existing NPC preserves the first registered quest range.
- Added focused tests for the staged table and source boundary.
- Integrated read-only explorer findings about Java handler and XML quest registration patterns.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Staged quest-start registration table | `QuestEngine.registerQuestNpc`, `QuestEngine.getQuestNpc`, `QuestNpc.addOnQuestStart` | `QuestNpcStartTable.cs`, `QuestNpcStartTableTests.cs` | Implementation / Tests | No write parallelism | Low | Completed by orchestrator; isolated new files. |
| B | Handler-source analyzer feasibility | Java quest handlers, XML quest templates | read-only | Java Analysis | Yes | Medium | Completed by explorer; no writes. |
| C | Side-effect persistence docs | AP rank equipment/abyss skill persistence paths | docs/read-only first | Analysis | Yes if read-only | Medium | Safe future sidecar candidate. |
| D | Real nearby-refresh dispatcher | `PlayerController.updateNearbyQuests`, `QuestService.checkStartConditions` | service/connection/world files | Implementation | No | High | Still blocked by registration extraction and start-condition evaluator. |

## Sub-Agent Output Integrated

Explorer `019e5f4f-b6a9-7411-9614-84f60a407185` completed read-only analysis and was closed.

Key findings:

- Direct Java handler registrations commonly use `qe.registerQuestNpc(...).addOnQuestStart(questId)`.
- Many handlers use constants, fields, arrays, inherited `questId`, or local `_questId`.
- Some registrations are loop-derived from sets or arrays.
- XML/template registrations are a separate important source: `QuestEngine.init()` loads Java scripts, then registers XML quests; `start_npc_ids` can generate start registrations, while `start_item_id` can suppress NPC start registration.
- Regex-only source parsing can provide a useful first tranche but is incomplete without XML quest script parsing and unresolved-case reporting.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartTableTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused quest-start table suite: passed, 4 tests.
- Full game-server suite: passed, 1670 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.registerQuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterQuestNpc` | Quest Engine / Registration Table | Partial | Unit Tested | Partial Parity | C# mirrors create-or-reuse behavior and first registered range preservation for start-registration storage only. Other Java quest maps/lists, dynamic handler loading, reload/unload behavior, threading, and reflection remain unported. |
| `com.aionemu.gameserver.questEngine.QuestEngine.getQuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartTable.GetQuestNpc` | Quest Engine / Registration Lookup | Partial | Unit Tested | Partial Parity | C# returns an empty non-stored registration when missing, matching Java source for this narrow path. Other event lists are not modeled. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartRegistration.AddOnQuestStart` | Quest NPC Registration | Partial | Unit Tested | Partial Parity | C# stores each start quest id once and reports duplicate status. Java uses `HashSet`; C# does not claim ordering parity. Talk/kill/attack/distance events and `registerCanAct` side effects are not modeled. |
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Loader Boundary / DTO | Partial | Unit Tested | Needs Verification | C# has a source metadata shape for future Java-handler and XML quest-script extraction. No extractor/loader is implemented, XML `start_item_id` suppression is not modeled, and no dynamic handler execution occurs. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartTableTests.RegisterQuestNpc_ReusesRegistrationAndTracksStartQuestIdsLikeJava` | Unit | Java `QuestEngine.registerQuestNpc` and `QuestNpc.addOnQuestStart` source review | Registration reuse, default range, duplicate-collapsing start quest ids, and source-shaped lookup. | Deterministic C# test from source-reviewed Java map/set behavior. | Does not execute Java handlers or parse source/XML. |
| `QuestNpcStartTableTests.GetQuestNpc_ReturnsUnregisteredEmptyRegistrationLikeJava` | Unit | Java `QuestEngine.getQuestNpc` source review | Missing lookup returns empty non-stored registration. | Deterministic C# test from reviewed Java method. | Does not model other `QuestNpc` event lists. |
| `QuestNpcStartTableTests.RegisterOnQuestStart_RecordsSourceBoundaryForFutureHandlerAndXmlExtractors` | Unit | Java `QuestEngine.init`, script handler loading, XML quest registration, and explorer analysis | Java-handler/XML/manual source metadata feeds the same start table and preserves custom range. | C# boundary test informed by Java load paths and read-only explorer analysis. | No Java/XML parser yet; no runtime comparison. |
| `QuestNpcStartTableTests.RegisterQuestNpc_PreservesFirstRegisteredRangeLikeJava` | Unit | Java `QuestEngine.registerQuestNpc(int, int)` source review | Re-registering an existing NPC with a different range preserves the first range. | Deterministic C# test from Java `containsKey` branch. | Does not verify range consumers. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `QuestNpcStartTable` is not integrated into `StaticData`, `DataManager`, NPC spawn, world instance population, or production dispatch.
- No Java handler source extractor exists yet; constants, arrays, inherited `questId`, `_questId`, loop-derived ids, and unresolved expressions need conservative handling.
- XML quest script registrations may be large and must be extracted separately; `start_item_id` can suppress NPC start registration.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 1 partial staged quest-start registration table in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 3 blocked/not-started categories, including Java handler extraction, XML quest-script extraction, and quest start-condition evaluation
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Implement a conservative extractor for either Java handler `addOnQuestStart` source patterns or XML quest-script `start_npc_ids` patterns, emitting `QuestNpcStartRegistrationSource` rows with explicit unresolved cases; keep runtime handler execution, `QuestService.checkStartConditions`, player-controller sends, and production ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java handler extractor analysis/implementation | new extractor/test files only | Medium | Do not edit `StaticData` or startup wiring in parallel. |
| B | XML quest-script extractor analysis/implementation | new extractor/test files only | Medium | Separate from Java source parser if write files do not overlap. |
| C | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing quest-start table, extractor shared abstractions, `StaticData`, `DataManager`, packet files, world runtime files, or Phase 6 docs.
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
