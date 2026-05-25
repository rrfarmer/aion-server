# Phase 6SM Completion - Staged Nearby Quest Marker Projection

Date: May 25, 2026
Unit of Work: UOW-995

## Session Summary

This unit added a staged bridge that filters world-instance quest ids through the partial nearby predicate and produces `NearbyQuestMarker` DTOs plus rejection reasons.

Java remains the source of truth. This unit does not send `SM_NEARBY_QUESTS`, wire `PlayerController.updateNearbyQuests`, or enable production ItemPurification dispatch.

## Completed Work

- Added `NearbyQuestMarkerProjectionService`.
- Added focused tests for:
  - filtering staged world quest ids through the partial predicate
  - preserving positive/negative level-diff values for the later packet marker rule
- Updated nearby-refresh/readiness/progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestMarkerProjectionServiceTests` passed with 2 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1696 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestMarkerProjectionService` | Controller / Quest UI Projection | Partial | Unit Tested | Partial Parity | Stages filtering world quest ids into marker DTOs and rejection reasons. It does not send `SM_NEARBY_QUESTS`, preserve Java `HashMap` ordering, integrate map-region/player-controller lookup, or run unsupported predicate dependencies. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `NearbyQuestMarkerProjectionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Bridge uses the staged partial predicate. XML start conditions, inventory item checks, combine skill, NPC faction, warning packets, exception/log behavior, and time-based repeat cooldowns remain unsupported. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `Aion.GameServer.Services.NearbyQuestStartConditionService.GetLevelRequirementDiff`; `NearbyQuestMarkerProjectionService` | Utility / Quest Predicate | Partial | Unit Tested | Partial Parity | Bridge projects positive/negative level diff values into marker DTOs for later packet serialization. Production packet send integration remains unwired. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestMarkerProjectionServiceTests.ProjectMarkers_FiltersWorldQuestIdsThroughStagedNearbyPredicateWithoutSendingPacket` | Unit | Java `PlayerController.updateNearbyQuests` filtering through `QuestService.checkStartConditions` | Validates staged world quest ids are filtered to marker DTOs and rejected ids carry predicate failure reasons. | Deterministic C# test from source-reviewed Java flow. | Does not send packets or run unsupported predicate dependencies. |
| `NearbyQuestMarkerProjectionServiceTests.ProjectMarkers_PreservesPositiveAndNegativeLevelDiffsForPacketMarkerRule` | Unit | Java `QuestService.getLevelRequirementDiff` and `SM_NEARBY_QUESTS` marker rule | Validates positive and negative level-diff values are projected into marker DTOs for later packet serialization. | Deterministic C# test from source-reviewed Java utility/packet rule. | Packet send/order not wired. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The marker bridge is not integrated into production `StaticData`, `DataManager`, player-controller refresh, packet sends, or ItemPurification dispatch.
- Java `HashMap`/set iteration order is not claimed.
- XML start-condition, inventory item, combine-skill, NPC faction, and repeat-cycle semantics remain unsupported beyond explicit rejection.
- Real-data marker projection is not pinned yet.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 1 staged marker projection bridge in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories, including production send boundary, XML start conditions, repeat timing, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds staged marker projection without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a read-only Java/C# audit for the future player-controller send boundary and production safety gates, or add a staged real-data marker projection only for templates without unsupported dependencies. Keep packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Audit the future player-controller send boundary and production safety gates.
- Why: The offline marker bridge exists, but production send wiring is still unsafe until dependencies and ordering are explicitly documented.
- Files: docs plus read-only Java/C# source inspection.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player-controller send-boundary audit | docs/read-only source | Low-Med | Safe documentation unit. |
| B | Staged real-data marker projection for supported templates only | isolated test file/docs | Medium | Must clearly exclude unsupported dependencies. |
| C | XMLStartCondition dependency expansion | read-only Java/docs | Low | Can run independently. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Send-boundary audit and docs | Phase 6 docs | Production send paths, `StaticData.cs`, ItemPurification dispatch |
| Agent A | Read-only XMLStartCondition dependency expansion | Read-only Java/docs | All writes |

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestMarkerProjectionService.cs`: one owner at a time if projection behavior changes.
- Phase 6 progress/handoff docs: orchestrator-owned.
