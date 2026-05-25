# Quest NPC Start Source Real-Data Audit

Date: May 25, 2026
Unit of Work: UOW-987, updated by UOW-988 and UOW-989

## Purpose

This audit records the offline staged loader output for Java/XML quest-start NPC registration sources.

Java remains the source of truth. This document does not enable production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, nearby-quest refresh, or `CM_ITEM_PURIFICATION` dispatch wiring, and it does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestNpc.java`
- `game-server/data/static_data/quest_script_data/**/*.xml`
- `game-server/data/handlers/quest/**/*.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartRegistrationSourceLoader.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`

## Audit Scope

The focused audit test runs `QuestNpcStartRegistrationSourceLoader` over:

- XML: `game-server/data/static_data/quest_script_data`
- Java handlers: `game-server/data/handlers/quest`

The staged loader extracts XML `start_npc_ids`, extracts direct Java handler `registerQuestNpc(...).addOnQuestStart(...)` calls, preserves unresolved Java handler rows, and does not execute Java handlers, run JAXB, populate runtime `QuestEngine`, or wire production C# startup.

## Audit Results

| Metric | Count |
|---|---:|
| Total staged start sources | 5214 |
| XML quest-script sources | 4400 |
| Java handler sources | 814 |
| Unresolved Java handler registrations | 0 |
| Distinct NPC ids across resolved sources | 1668 |
| Distinct quest ids across resolved sources | 4503 |

## Staged Table Population Results

UOW-989 feeds the audited loader output into `QuestNpcStartTable` in a focused regression test. This remains offline and does not wire production startup.

| Metric | Count |
|---|---:|
| Source rows recorded by `QuestNpcStartTable.Sources` | 5214 |
| Registered NPC ids | 1668 |
| Registered NPC/quest start pairs | 5214 |
| Largest quest-start set on one NPC | 50 |

## Staged World-Instance Projection Results

UOW-990 projects the populated `QuestNpcStartTable` into a staged `WorldMapInstanceRuntimeState` quest-id set. This mirrors only Java's `WorldMapInstance.addObject(Npc)` contribution of `QuestNpc.getOnQuestStart()` ids and does not run start-condition filtering, delayed refresh scheduling, or packet sends.

| Metric | Count |
|---|---:|
| Inspected NPC ids from populated table | 1668 |
| NPC ids with staged quest starts | 1668 |
| Projected distinct quest ids | 4503 |
| Newly registered world-instance quest ids | 4503 |
| Final world-instance quest-id count | 4503 |

## Unresolved Java Handler Registrations

None in the current repository-data audit.

UOW-988 resolved the previous six `butlerId` rows by adding support for the deterministic Java pattern where handlers populate a static integer set through `butlers.add(...)`, iterate it with `Iterator<Integer>.next()` or enhanced `for`, and call `registerQuestNpc(butlerId).addOnQuestStart(questId)`.

## Interpretation

This audit gives a deterministic repository-data baseline for the staged source extractor, not runtime parity.

Known limitations:

- Java reflection/classloading behavior is not executed.
- JAXB model loading and schema validation are not executed.
- Java handler dynamic expressions remain unresolved unless supported explicitly, but the current real-data audit has no unresolved rows.
- XML extraction still focuses only on NPC start registrations, not talk/kill/end/distance/zone registrations.
- Candidate calculation, `QuestService.checkStartConditions`, level-difference marker calculation, and `SM_NEARBY_QUESTS` sending remain unimplemented for runtime nearby refresh.

## Next Recommended Unit

Continue with a read-only Java/C# audit for the future player-controller send boundary, or add a staged real-data marker projection only for templates without unsupported dependencies. Keep production startup and nearby-refresh dispatch disabled until predicate parity exists.
