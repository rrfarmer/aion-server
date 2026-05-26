# Phase 6WK Completion - UOW-1097 Supported Bonus Item-Group Projection

Date: May 26, 2026

## Unit Of Work

UOW-1097: `[Phase 6][UOW-1097] Project supported quest bonus item groups`

## Summary

UOW-1097 adds a non-live XML projection for Java-supported quest bonus item groups from `item_groups.xml`. It preserves group metadata and raw item attributes needed for later candidate filtering and random selection.

No random selection or production reward execution path was enabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestBonusItemGroupXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestBonusItemGroupXmlProjectionExtractorTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WK-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusItemGroupXmlProjectionExtractorTests" --nologo` | Passed: 2 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,155 tests. |

## Migration Parity Table - UOW-1097

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.reward.BonusService` | `QuestBonusItemGroupXmlProjectionExtractor` | Reward Service Dependency | Partial | Unit Tested / Regression Tested | Partial Parity | C# now projects only Java-live group families used by `getBonusGroups`. It does not yet run `Chance.selectElement`, group retry, player/quest filtering, selected item creation, or handler approval. |
| `com.aionemu.gameserver.dataholders.ItemGroupsData` | `QuestBonusItemGroupXmlProjectionExtractor` | Static Data Holder Dependency | Partial | Regression Tested | Partial Parity | Supported group element names are mapped from Java `ItemGroupsData`; non-live groups with data remain intentionally excluded. JAXB unmarshal validation, schema validation, and full static-data integration remain unverified. |
| `com.aionemu.gameserver.model.templates.itemgroups.BonusItemGroup` | `QuestBonusItemGroupProjection` | DTO Projection | Partial | Unit Tested / Regression Tested | Partial Parity | Projects `bonusType`, group `chance`, and item list for supported families. Java collection mutability, JAXB defaults beyond chance, and chance-selection behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry` | `QuestBonusItemProjection` | DTO Projection | Partial | Unit Tested / Regression Tested | Needs Verification | Raw `id` and `race` attributes are preserved, but item-template race validation and `matches(playerRace, questTemplate)` behavior are not implemented. |
| `com.aionemu.gameserver.model.templates.rewards.IdLevelReward` | `QuestBonusItemProjection.Level` | DTO Projection | Partial | Unit Tested | Needs Verification | Raw `level` is preserved for item shapes using XML level matching. Java subclass-specific level matching is not implemented. |
| `com.aionemu.gameserver.model.templates.rewards.FullRewardItem` | `QuestBonusItemProjection.Count`; `Chance` | DTO Projection | Partial | Unit Tested | Needs Verification | Raw `count` and item `chance` are preserved. Java primitive default `count=0`/`chance=0` behavior and weighted item selection remain unverified. |
| `com.aionemu.gameserver.model.templates.rewards.CraftItem` | `QuestBonusItemProjection.Skill/MinLevel/MaxLevel` | DTO Projection | Partial | Unit Tested | Needs Verification | Raw craft skill and skill-point bounds are preserved. Java `combine_skill` / `combine_skillpoint` filters and random `Rnd.get(3,5)` count remain unimplemented. |
| `com.aionemu.gameserver.model.templates.rewards.CraftRecipe` | `QuestBonusItemProjection.Skill/Level` | DTO Projection | Partial | Unit Tested | Needs Verification | Raw craft recipe skill and level are preserved. Java max-level calculation and quest filter remain unimplemented. |
| `com.aionemu.gameserver.model.templates.rewards.FoodItem` | `QuestBonusItemShape.FoodItem` | DTO Projection | Partial | Unit Tested | Needs Verification | Shape is preserved so future code can apply Java random `5` or `10` count. Random count behavior is not implemented. |
| `com.aionemu.gameserver.model.templates.rewards.MedicineItem` | `QuestBonusItemShape.MedicineItem` | DTO Projection | Partial | Unit Tested | Needs Verification | Shape is preserved so future code can apply Java random `1..3` count. Random count behavior is not implemented. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestBonusItemGroupXmlProjectionExtractorTests.ExtractSupportedGroups_ReadsJavaLiveBonusGroupShapesAndRawItemAttributes` | Supported XML group elements map to Java-live bonus type and item shape, preserve raw attributes, and exclude `boss_rare`. | Source-reviewed from `BonusService.getBonusGroups`, `ItemGroupsData`, and item group DTOs; no Java runtime comparison. |
| `QuestBonusItemGroupXmlProjectionExtractorTests.RealDataAudit_LoadsSupportedJavaBonusGroupsWithoutSelection` | Confirms 12 supported groups and 4,701 supported raw item entries split by Java-live bonus type. | Real Java XML loaded through the C# extractor; not a Java object/runtime comparison. |

## Remaining Risks

- Random bonus item selection is still not implemented: no weighted Chance RNG, selected-group retry after empty filters, selected item descriptor, or random count calculation.
- Item filters are raw metadata only: item-template race validation, player race matching, XML race override, bonus level matching, craft skill matching, and craft skill-point bounds remain unimplemented.
- `QuestEngine.onBonusApplyEvent` can mutate/approve/fail bonus reward application and remains unmodeled.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard or reward projection planners.
- Live `ItemService.addItem`, inventory capacity/stacking, packet emission, persistence, and rollback behavior remain disabled in this path.
- Serialization/JAXB lifecycle differences, schema validation, reflection differences, threading differences, date/time reward-repeat handling, and precision/rate application remain unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 10 in this unit
- Total artifacts ported: 1 partial supported bonus item-group projection slice
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10
- Total blocked artifacts: 9 blocked/partial categories: production socket integration, live item reward mutation, live non-item reward mutation, bonus item selection, `QuestEngine.onBonusApplyEvent`, item-template validation, all-group item projection, custom reward/mail execution, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a non-live bonus candidate filtering planner over `QuestBonusItemGroupProjection`: preserve Java's supported group mapping, but only compute deterministic eligible group/item metadata for explicit player race, quest bonus level, and craft skill inputs.

Keep weighted random selection, handler events, and live item rewards disabled until the candidate pool behavior is pinned down.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Bonus candidate filtering planner | new focused service/tests | Medium | Recommended next unit; no production wiring. |
| B | Production self-report quest-finish planning bridge audit | read-only or isolated helper/tests | Medium | Useful if bonus filtering is deferred. |
| C | Bonus handler event analysis | read-only Java handler analysis | Low/Medium | Map `QuestEngine.onBonusApplyEvent` registrations before handler mutation modeling. |

## Do Not Parallelize

- `QuestBonusItemGroupXmlProjectionExtractor.cs`: owns supported group projection contract.
- `QuestFinishRewardPlanService.cs`: shared reward planner contract.
- `QuestFinishOperationPlanService.cs`: shared operation planner contract.
- Phase 6 progress/handoff docs: orchestrator-owned.
