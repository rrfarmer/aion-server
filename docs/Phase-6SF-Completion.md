# Phase 6SF Completion Handoff - Butler Quest Start Extraction

Date: May 25, 2026
Unit of Work: UOW-988
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-988] Resolve butler quest start extraction`)

## Status

Phase 6 is still in progress. This unit resolves the six previously unresolved `butlerId` Java handler quest-start registrations in the offline extractor/audit path.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No production `StaticData`/`DataManager` integration, Java handler execution, NPC-spawn population, delayed refresh scheduling, nearby-quest candidate filtering, `QuestService.checkStartConditions`, player-controller send, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartJavaHandlerExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`
- `docs/QuestNpcStart-RealData-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SF-Completion.md`

## What Changed

- Reviewed the six Oriel/Pernon `butlerId` handlers.
- Confirmed Java deterministically populates static `Set<Integer> butlers` values, then iterates them and calls `addOnQuestStart`.
- Added extractor support for:
  - `setName.add(123)` integer sets
  - `Iterator<Integer> iter = setName.iterator()`
  - `int localId = iter.next()`
  - `for (int localId : setName)`
- Added focused unit coverage for iterator/enhanced-for static integer-set expansion.
- Updated the real-data audit baseline to zero unresolved handler registrations.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused Java handler extractor suite: passed, 5 tests.
- Focused real-data source-loader audit: passed, 1 test after baseline update.
- Full game-server suite: passed, 1683 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `game-server/data/handlers/quest/oriel/_18806HeartofRock.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | C# now expands the static `butlers` set and `Iterator<Integer>.next()` start-registration pattern. Runtime dialog behavior, house ownership checks, talk events, Java `HashSet` iteration order, and handler execution are not ported here. |
| `game-server/data/handlers/quest/oriel/_18821AlmostForgotMyBlessings.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | Same static Oriel butler set support as above. Quest dialog/reward behavior and active-house butler checks remain outside the extractor. |
| `game-server/data/handlers/quest/oriel/_18828UserFriendly.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | Start registrations are extracted; Java item registration/use behavior for `182213191` and dialog flow remain unported here. |
| `game-server/data/handlers/quest/pernon/_28806WiltingFlowersFallingTears.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | C# now expands the static Pernon `butlers` set and iterator start-registration pattern. Runtime dialog behavior, house ownership checks, talk events, Java `HashSet` iteration order, and handler execution are not ported here. |
| `game-server/data/handlers/quest/pernon/_28821YourButlerGift.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | Same static Pernon butler set support as above. Quest dialog/reward behavior and active-house butler checks remain outside the extractor. |
| `game-server/data/handlers/quest/pernon/_28828TheManyFacetsOfFriendship.java` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extraction | Partial | Regression Tested | Partial Parity | Start registrations are extracted; Java item registration/use behavior for `182213205` and dialog flow remain unported here. |
| Representative `game-server/data/handlers/quest/**` classes extending `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader` | Java Handler Source Loader | Partial | Regression Tested | Needs Verification | Real-data audit now reports zero unresolved handler registrations. This still does not execute Java classloading/reflection or prove full runtime handler parity. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest NPC Registration / DTO | Partial | Regression Tested | Partial Parity | Extracted set-expanded handler sources can feed staged table storage, but this unit does not populate runtime world instances or claim Java `HashSet` iteration order. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartJavaHandlerExtractorTests.ExtractsIteratorAndForEachValuesFromStaticIntegerSets` | Unit | Java Oriel/Pernon butler handlers using `Set<Integer>`, `Iterator<Integer>.next()`, and enhanced `for` | Static integer-set values are expanded into one start source per set value for iterator and enhanced-for loops. | Deterministic C# test from reviewed Java handler source. | Does not claim Java `HashSet` iteration order or execute handlers. |
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_LoadsStagedQuestStartSourcesWithoutProductionWiring` | Regression | Real repository Java/XML quest-start source data | Pins updated staged-loader counts: 5214 total sources, 4400 XML, 814 Java handler, zero unresolved handler registrations, 1668 distinct NPC ids, and 4503 distinct quest ids. | Deterministic C# audit over current repository source files. | Does not run Java reflection/JAXB, execute handlers, populate production runtime state, or prove runtime parity. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged source loader and audit are not integrated into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- Java `HashSet` iteration order is not claimed for expanded set values; the staged table stores sets, but source-list order must not be used as parity evidence.
- The handler extractor still supports only conservative source patterns; future dynamic expressions, collection-derived IDs, constructor parameters, inherited fields, method calls, and nonliteral assignments may need triage if new unresolved rows appear.
- XML extraction remains partial and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 8 in this unit
- Total artifacts ported: 1 extractor pattern enhancement in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8
- Total blocked artifacts: 3 blocked/not-started categories, including production loader integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Add a staged table-population adapter or test fixture that feeds audited loader output into `QuestNpcStartTable`, verifies duplicate-collapsing registration behavior on real data, and reports table/source counts; keep production `StaticData`/`DataManager`, `QuestService.checkStartConditions`, player-controller sends, and ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Real-data table population fixture | focused test or new staging helper only | Medium | Do not wire production startup. |
| B | Candidate filtering Java analysis | read-only Java `PlayerController`/`QuestService` source plus docs | Medium | Safe if no code edits. |
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
