# Quest NPC Start Source Real-Data Audit

Date: May 25, 2026
Unit of Work: UOW-987

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
| Total staged start sources | 5184 |
| XML quest-script sources | 4400 |
| Java handler sources | 784 |
| Unresolved Java handler registrations | 6 |
| Distinct NPC ids across resolved sources | 1668 |
| Distinct quest ids across resolved sources | 4497 |

## Unresolved Java Handler Registrations

All unresolved rows are the current `butlerId` pattern. The extractor does not guess these because `butlerId` is not resolved by the current literal/simple-assignment/array-index subset.

| Java Handler | Line | NPC Expression | Quest Expression | Reason |
|---|---:|---|---|---|
| `game-server/data/handlers/quest/oriel/_18806HeartofRock.java` | 41 | `butlerId` | `questId` | Unsupported expression |
| `game-server/data/handlers/quest/oriel/_18821AlmostForgotMyBlessings.java` | 41 | `butlerId` | `questId` | Unsupported expression |
| `game-server/data/handlers/quest/oriel/_18828UserFriendly.java` | 43 | `butlerId` | `questId` | Unsupported expression |
| `game-server/data/handlers/quest/pernon/_28806WiltingFlowersFallingTears.java` | 41 | `butlerId` | `questId` | Unsupported expression |
| `game-server/data/handlers/quest/pernon/_28821YourButlerGift.java` | 41 | `butlerId` | `questId` | Unsupported expression |
| `game-server/data/handlers/quest/pernon/_28828TheManyFacetsOfFriendship.java` | 43 | `butlerId` | `questId` | Unsupported expression |

## Interpretation

This audit gives a deterministic repository-data baseline for the staged source extractor, not runtime parity.

Known limitations:

- Java reflection/classloading behavior is not executed.
- JAXB model loading and schema validation are not executed.
- Java handler dynamic expressions remain unresolved unless supported explicitly.
- XML extraction still focuses only on NPC start registrations, not talk/kill/end/distance/zone registrations.
- Candidate calculation, `QuestService.checkStartConditions`, level-difference marker calculation, and `SM_NEARBY_QUESTS` sending remain unimplemented for runtime nearby refresh.

## Next Recommended Unit

Resolve the six `butlerId` handler registrations by inspecting the Java housing/butler quest pattern and adding a conservative extraction rule only if the Java source makes the value deterministic. Keep production startup and nearby-refresh dispatch disabled.
