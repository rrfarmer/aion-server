# Phase 6UT Completion - UOW-1054 Guide HTML Level-Change Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6US-Completion.md`.

## Last Completed Unit

- UOW-1054: `[Phase 6][UOW-1054] Stage guide HTML level-change plan`
- Recent commits before this unit:
  - `54ceef72f [Phase 6][UOW-1053] Align nearby quest empty packet intent`
  - `4cd70dd3c [Phase 6][UOW-1052] Stage QuestEngine level-change callbacks`
  - `a381c58bf [Phase 6][UOW-1051] Stage NPC faction level-up plan`
- UOW-1054 status: complete after full test/commit; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1054 adds a non-live C# guide HTML level-change planner for Java `PlayerController.onLevelChange -> HTMLService.sendGuideHtml`. The planner records Java config/spawn gates, inclusive level iteration, guide template lookup order, inactive-template skips, and future questionnaire-send plus guide-persistence intent. It does not allocate ids, render HTML, send `SM_QUESTIONNAIRE`, or write `guides` rows.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Guide HTML level-change plan | `PlayerController.onLevelChange`, `HTMLService.sendGuideHtml`, `GuideHtmlData.getTemplatesFor` | New guide planner, tests, docs | Service Plan | Selected sequential | Medium | Smallest next Java-order side effect after nearby refresh; shared docs remain orchestrator-owned. |
| B | Skill auto-learn audit/plan | `SkillLearnService.learnNewSkills`, `SkillTreeData` | Skill planner/tests/docs | Service Plan / Analysis | Sequential recommended | Medium-High | Broader: reverse level loop, starting-class backfill, gathering upgrade, packet/effect/recipe side effects. |
| C | Compose existing sub-plans into XP metadata | `PlayerController.onLevelChange` | `QuestXpExecutionPlanService.cs`, XP tests | Plan Composition | No | Medium | Shared XP execution order should remain orchestrator-owned. |
| D | Guide questionnaire packet prerequisite | `SM_QUESTIONNAIRE`, `HTMLService.sendData` | Server packet writer/tests | Packet | Possible isolated later | Medium | Useful follow-up but depends on encoding/chunking decisions and should not be mixed with planner semantics. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the selected work touched shared migration docs and a new planner/test pair with tight Java-order semantics.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/GuideHtmlLevelChangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GuideHtmlLevelChangePlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UT-Completion.md`

## What Changed

- Added `GuideHtmlLevelChangePlanService.CreatePlan`.
- Added `GuideHtmlTemplateSummary` and `GuideHtmlSurveySummary` planning DTOs.
- Added plan statuses for disabled guides, unspawned players, empty level ranges, missing templates, no matching templates, inactive-only templates, and missing player.
- Added descriptor statuses for planned send/persist and inactive-template skip.
- Preserved Java `GuideHtmlData.getTemplatesFor` template order.
- Recorded future `IDFactory.nextId`, `HTMLService.sendData` / `SM_QUESTIONNAIRE`, and `GuideDAO.saveGuide` side-effect intent without executing live behavior.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GuideHtmlLevelChangePlanServiceTests" --nologo` | Passed: 2 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1841 |

## Migration Parity Table - UOW-1054

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `Aion.GameServer.Services.QuestXpExecutionPlanService`; `GuideHtmlLevelChangePlanService` | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | UOW-1048 staged the level-change order and UOW-1054 adds a dedicated guide HTML sub-plan. The sub-plan is not composed into XP execution and live XP level-change execution remains disabled. |
| `com.aionemu.gameserver.services.HTMLService.sendGuideHtml` | `Aion.GameServer.Services.GuideHtmlLevelChangePlanService.CreatePlan`; `GuideHtmlLevelChangePlan` | Service Side-Effect Plan | Partial | Unit Tested | Partial Parity | Non-live plan records Java guide config/spawn caller gates, inclusive level loop, inactive-template skips, generated guide-id requirement, questionnaire-send intent, and guide persistence intent. It does not render HTML, allocate ids, send `SM_QUESTIONNAIRE`, call `GuideDAO.saveGuide`, or persist rows. |
| `com.aionemu.gameserver.dataholders.GuideHtmlData.getTemplatesFor` | `GuideHtmlLevelChangePlanService.GetTemplatesFor`; `GuideHtmlTemplateSummary` | Static Data / Template Lookup | Partial | Unit Tested | Partial Parity | C# planner preserves Java lookup order for class+race, class+`PC_ALL`, all-class+race, and all-class+`PC_ALL`. It uses caller-supplied summaries because C# static guide XML loading is not ported. Serialization/XML default behavior and all real guide data remain unverified. |
| `com.aionemu.gameserver.model.templates.Guides.GuideTemplate` | `Aion.GameServer.Services.GuideHtmlTemplateSummary` | DTO / Template Summary | Partial | Unit Tested | Needs Verification | C# summary carries title, level, class, race, activation, reward count, surveys, message, select, and reward info for planning. It is not a full XML/JAXB equivalent and does not render Java `guideTemplate.xhtml` placeholders. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTIONNAIRE` | Guide descriptor metadata only | Packet Dependency | Not Started | Unit Tested as metadata | Needs Verification | Planner records packet intent only. No C# `SmQuestionnaire` server packet writer, chunking regression, UTF-16 string length check, or Java golden-byte comparison exists. |
| `com.aionemu.gameserver.dao.GuideDAO.saveGuide` | Guide descriptor metadata only | Repository Dependency | Not Started | Unit Tested as metadata | Needs Verification | Planner records persistence intent only. No C# guide repository, SQL, generated-id ownership, transaction/failure behavior, or login replay path exists. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GuideHtmlLevelChangePlanServiceTests.CreatePlan_StagesJavaGuideTemplateOrderAndPersistenceIntent` | Unit | `PlayerController.onLevelChange`; `HTMLService.sendGuideHtml`; `GuideHtmlData.getTemplatesFor`; `GuideDAO.saveGuide`; `SM_QUESTIONNAIRE` | Guide config/spawn success path, Java template lookup order, inactive-template skip, planned generated-id/send/persist metadata, and non-live descriptors. | Source-reviewed Java loop and deterministic C# plan assertions. | No live HTML rendering, packet send, guide DAO write, IDFactory allocation, or Java runtime comparison. |
| `GuideHtmlLevelChangePlanServiceTests.CreatePlan_RecordsJavaConfigSpawnedMissingRangeAndInactiveBranches` | Unit | `PlayerController.onLevelChange`; `HTMLConfig.ENABLE_GUIDES`; `player.isSpawned`; `GuideTemplate.isActivated` | Disabled guide config, unspawned player, empty level range, missing static templates, no matching templates, inactive-only templates, and missing-player guards. | Source-reviewed Java gates plus conservative C# missing-context statuses. | Missing template input and missing player are C# planner guards rather than Java runtime branches. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The guide HTML plan is not composed into `QuestXpExecutionPlan` and is not connected to quest-finish execution.
- C# static guide XML loading and `GuideHtmlData` equivalent are not ported; templates are caller-supplied summaries.
- Java `HTMLService.getHTMLTemplate` rendering, `HTMLCache` file loading, placeholder replacement, string encoding, and `SM_QUESTIONNAIRE` chunking remain unported.
- `IDFactory.nextId`, `GuideDAO.saveGuide`, guide login replay, guide reward claim, guide deletion, and id release remain unported.
- Java level-change side effects outside guide HTML remain descriptors only: skill auto-learn, custom rewards, and starter kits.
- Threading, serialization, file/path/encoding, persistence, and packet ordering parity remain unverified for this side effect; date/time, precision/rounding, and reflection behavior were not touched in this unit.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 non-live guide HTML level-change side-effect sub-plan plus 1 template-summary DTO surface
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, live XP composition, static guide XML loading, `SM_QUESTIONNAIRE` server packet/chunking, and guide DAO persistence
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds one more level-change side-effect prerequisite without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Audit/stage `SkillLearnService.learnNewSkills`.
- Why: guide HTML now has a non-live planning surface; skill auto-learn is the next Java-order level-change dependency before custom rewards and starter kit.
- Suggested first slice: non-live planner for Java reverse level loop, starting-class backfill for switched classes below level 10, autolearn filtering, human gathering to daeva essence upgrade, and skill add/remove packet/effect/recipe metadata.
- Likely files: `SkillLearnService.cs`, `SkillTreeTable.cs` or a new focused planner, tests, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, progress/handoff docs. Keep live XP execution disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Skill auto-learn Java audit | Read-only Java/C# inspection | Medium | Can be done read-only, but implementation should remain orchestrator-owned if it touches shared skill services. |
| B | Guide questionnaire packet prerequisite | New server packet writer/tests | Medium | Isolated if no live guide planner changes are included; verify Java string length/chunking carefully. |
| C | Guide DAO schema audit | Read-only SQL/DAO inspection | Low-Medium | Useful before live guide persistence; no writes needed. |
| D | Compose guide sub-plan into XP metadata | `QuestXpExecutionPlanService.cs`, XP tests | Medium | Sequential only; shared XP execution order. |

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `SkillLearnService.cs` / `SkillTreeTable.cs` implementation changes: shared skill behavior.
- `docs/PHASE-6-PROGRESS.md`, audit docs, and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1054, `GuideHtmlLevelChangePlanService.cs`, `GuideHtmlLevelChangePlanServiceTests.cs`, `QuestXpExecutionPlanService.cs`, Java `SkillLearnService.learnNewSkills`, Java `SkillTreeData`, and C# `SkillTreeTable`.
