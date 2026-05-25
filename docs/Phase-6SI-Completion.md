# Phase 6SI Completion - Nearby Start Conditions Audit

Date: May 25, 2026
Unit of Work: UOW-991

## Session Summary

This unit source-audited the Java nearby quest start-condition predicate and level-diff marker calculation that sit between staged world quest ids and `SM_NEARBY_QUESTS`.

Java remains the source of truth. This unit does not implement a C# predicate, does not enable packet sends, and does not wire production ItemPurification dispatch.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/XMLStartCondition.java`
- `game-server/src/com/aionemu/gameserver/dataholders/QuestsData.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/QuestStateList.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestState.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestStatus.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestCandidateProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`
- Future C# quest-template/start-condition dataholders and nearby predicate service

## Completed Work

- Added `docs/QuestStartConditions-Nearby-Audit.md`.
- Documented the exact Java nearby UI call shape:
  - `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)`
  - `QuestService.getLevelRequirementDiff(questId, playerLevel)`
- Documented Java predicate gate order:
  - existing quest state and repeatability
  - quest-template lookup
  - race, min/max level, class, gender, abyss rank
  - XML start conditions
  - inventory item preconditions
  - combine skill requirements
  - NPC faction state
- Documented `XMLStartCondition` subchecks and the nearby-specific `warn = false` equipped-item behavior.
- Documented the level-diff marker rule used by `SM_NEARBY_QUESTS`.
- Updated nearby-refresh/readiness docs with the new C# dependency gaps.

## Validation

- Documentation-only unit; no code tests were added or run after this audit.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Future C# nearby quest start-condition service | Service / Quest Predicate | Not Started | Manual Only | Needs Verification | Source-audited for nearby UI call shape with `allowedDiffToMinLevel = 2`, `warn = false`, and all skip flags false. No C# implementation exists. Missing quest template data, repeat semantics, XML conditions, inventory preconditions, combine-skill checks, NPC faction checks, exception/log behavior, and warning packet behavior. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | Future C# level-diff projector | Utility / Quest Predicate | Not Started | Manual Only | Needs Verification | Source-audited. Java returns `99` for missing templates, otherwise `minlevel_permitted - playerLevel`. Needed for the `SmNearbyQuests` positive-diff marker bit. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | Future C# quest template dataholder | Dataholder / DTO | Not Started | No Tests | Unknown | Required fields include min/max level, race, class, gender, rank, max repeat count, XML start conditions, inventory items, combine skill, NPC faction, category, time-based/repeat metadata, and master-crafting adjustment inputs. JAXB defaults and enum mapping are unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | No Tests | Unknown | Finished/unfinished/acquired/noacquired/title checks are unported. Equipped-item checks intentionally do not affect nearby UI when `warn = false`, but this needs implementation tests once ported. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Player Quest State | Partial | Unit Tested elsewhere | Needs Verification | C# stores status, vars, flags, and complete count for packet serialization, but repeatability and next-repeat timing needed by `QuestState.canRepeat()` are not ported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `docs/QuestStartConditions-Nearby-Audit.md` | Manual Source Audit | Java `QuestService.checkStartConditions`, `QuestService.getLevelRequirementDiff`, `QuestTemplate`, and `XMLStartCondition` | Documents the nearby UI predicate gate order, level-diff marker rule, and missing C# dependencies. | Source-reviewed Java audit with explicit C# gap list. | No executable C# predicate, no Java runtime comparison, and no C# quest-template data boundary yet. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No C# predicate exists for nearby `checkStartConditions`.
- Quest template static data for start-condition fields is not ported.
- Repeatability, repeat reset timing, and `QuestState.canRepeat()` are not modeled.
- XML start-condition semantics are source-audited only.
- NPC faction, combine-skill, inventory, abyss-rank, title, class/race/gender enum mapping, and exception/log behavior need C# homes before runtime candidate filtering can be claimed.
- Packet sends and production ItemPurification dispatch must remain disabled.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 0 in this audit-only unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including quest template data, XML start conditions, repeatability, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies the next predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged quest-template/start-condition data boundary for nearby filtering, starting with the fields used by level and race/class/gender/rank gates plus `getLevelRequirementDiff`. Keep XML start conditions, NPC faction, combine skill, packet sends, and production ItemPurification dispatch disabled until each dependency has tests.

Suggested narrow shape:

1. Add a C# DTO or staged table for the minimal nearby quest template fields: quest id, min/max level, race, class list, gender, required rank, and max repeat count if needed for repeat gates.
2. Add a pure level-diff helper that returns Java's `99` for missing templates and `minlevel_permitted - playerLevel` otherwise.
3. Add tests for level grace (`allowedDiffToMinLevel = 2`) and level-diff marker inputs without evaluating XML start conditions or sending packets.
4. Document unsupported XML conditions, NPC faction, combine skill, inventory, repeat timing, and warning packet behavior explicitly in the parity table.
