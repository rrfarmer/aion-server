# Phase 6KW Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KV and covers Session 797.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "StaticDataNpcSkillTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 38 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1388 tests.

## Recent Work Completed

### Session 797 - Represented NPC Skill Static-Data Loader

- Re-inspected Java `NpcSkillData`, `NpcSkillTemplates`, `NpcSkillTemplate`, and `NpcSkillSpawn`.
- Added represented C# `NpcSkillTable`, `NpcSkillListSummary`, `NpcSkillTemplateSummary`, and `NpcSkillSpawnSummary` dataholder types.
- Extended `StaticData.LoadFromCacheAsync` to project `npc_skill_templates` / `npc_skills` / `npc_skill` / `spawn_npc` XML into the represented table.
- Modeled Java JAXB `npc_ids` whitespace-list parsing, first-list-wins duplicate NPC-id indexing, `NpcSkillTemplate` scalar defaults, and `NpcSkillSpawn` scalar defaults.
- Kept live Java JAXB unmarshalling, duplicate warning logging, `DataManager.SKILL_DATA` pruning/lookup, condition templates, full NPC AI skill selection, scheduler/date-time execution, random behavior, effects, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.NpcSkillData` | `Aion.GameServer.Dataholders.NpcSkillTable` plus `StaticData.NpcSkills` | Dataholder / Repository | Partial | Regression Tested | Needs Verification | Indexes represented NPC skill lists by NPC id and preserves Java first-list-wins duplicate behavior. Does not run JAXB, duplicate warning logging, `setNpcSkillTemplates`, `DataManager.SKILL_DATA` pruning, or live Java data-manager integration. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates` | `Aion.GameServer.Dataholders.NpcSkillListSummary` | DTO | Partial | Regression Tested | Needs Verification | Carries `npc_ids` and skill summaries. XML whitespace parsing is tested, including tabs. Shared object identity is represented for indexed duplicate checks, but Java collection mutation/JAXB lifecycle behavior is not ported. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate` | `Aion.GameServer.Dataholders.NpcSkillTemplateSummary` | DTO | Partial | Regression Tested | Needs Verification | Projects represented scalar fields/defaults used by current NPC skill readiness work. Condition templates, `getSkillTemplate()` live lookup, full target enum semantics, XML enum validation, and runtime AI behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.npcskill.NpcSkillSpawn` | `Aion.GameServer.Dataholders.NpcSkillSpawnSummary` | DTO | Complete for represented scalar fields | Regression Tested | Needs Verification | Projects `npc_id`, `delay`, `min_distance`, `max_distance`, `min_count`, and `max_count`, including Java defaults. Runtime Java JAXB comparison, serialization behavior, and spawn-engine consumption remain unverified. |
| `javax.xml.bind.annotation.XmlList` / Java JAXB whitespace handling | `StaticData.ReadXmlIntListAttribute` | XML Loading Utility | Partial | Regression Tested | Needs Verification | Adds a dedicated whitespace-list parser for `npc_ids`. It is not a general JAXB replacement and does not validate schema or enum conversion behavior. |

## Tests Added Or Updated

- `StaticDataNpcSkillTests.LoadFromCacheAsync_ProjectsNpcSkillSpawnXmlDefaultsAndNpcIdIndex`
  - Validates represented `npc_skill_templates` loading.
  - Validates XML whitespace-list `npc_ids`, including a tab separator.
  - Validates first-list-wins duplicate NPC indexing.
  - Validates Java `NpcSkillTemplate` defaults and explicit scalar overrides.
  - Validates Java `NpcSkillSpawn` defaults and explicit scalar overrides.
- These tests are source-derived from Java. They do not compare against Java runtime JAXB execution, duplicate warning logs, schema validation, XML enum conversion, `DataManager.SKILL_DATA` lookup/pruning, live AI selection, scheduler/date-time behavior, reflection behavior, threading behavior, serialization behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill static-data loader/table slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live JAXB runtime comparison, schema validation, duplicate warning logging, mutable `setNpcSkillTemplates`, condition templates, target enum validation, `DataManager.SKILL_DATA` lookup/pruning, skill-template live lookup, AI selection, Java RNG runtime comparison, scheduler/date-time execution, spawn-engine execution, live AI mutation, controller/effect execution, packets, threading/serialization, reflection behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Static NPC-skill loading is represented data only; it is not yet wired into live NPC AI skill execution.
- Live Java JAXB behavior, schema validation, duplicate warning logs, mutable `setNpcSkillTemplates`, condition templates, target enum validation, `DataManager.SKILL_DATA.getSkillTemplate`, skill pruning, probability/random selection, spawn scheduling, spawn-engine execution, AI mutation, controller/effect execution, packets, persistence, threading, serialization, date/time behavior, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill readiness parity by adding an adapter from represented `NpcSkillTable` / `NpcSkillTemplateSummary` into `PlayerSummonSkillExecutionService` candidate projection, including `spawn_npc` metadata and `is_post_spawn` filtering. Keep condition-template evaluation, live `DataManager.SKILL_DATA` pruning, Java `Rnd.chance`/`Rnd.get`, random target selection/spawns, effects, packets, spawn-engine execution, scheduler/date-time behavior, threading, serialization, and controller execution explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KV-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillData`, `NpcSkillTemplates`, `NpcSkillTemplate`, `NpcSkillSpawn`, and the NPC AI call sites that consume `getNpcSkillList`.
4. Inspect C# `NpcSkillTable`, `StaticData.NpcSkills`, `PlayerSummonSkillExecutionService.ProjectMercenaryNpcSkillTemplate`, and the represented candidate tests.
5. Implement one narrow adapter from represented static NPC skills into summon NPC skill candidate projection, preserving Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
