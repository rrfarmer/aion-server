# Phase 6WU Completion - UOW-1107 Quest Bonus Static Data Bridge

Date: May 26, 2026

## Unit Of Work

UOW-1107: `[Phase 6][UOW-1107] Bridge quest bonus static data`

## Summary

UOW-1107 adds the narrow `StaticData.QuestBonusItemGroups` bridge recommended by UOW-1106.

The C# static-data loader now collects supported quest bonus item groups while reading the flattened static-data cache and exposes them as a `QuestBonusItemGroupTable`. This makes supported Java `BonusService` group data available through `DataManager.StaticData` without wiring quest finish or live rewards.

The bridge remains intentionally narrower than Java `ItemGroupsData`: it covers supported quest bonus groups only and leaves pet-food and broader item-group consumers unported.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestBonusItemGroupXmlProjectionExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestBonusStaticDataBridgeTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WU-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusStaticDataBridgeTests\|QuestBonusItemGroupTableTests\|QuestBonusItemGroupXmlProjectionExtractorTests\|QuestBonusRewardPlanningInputAdapterServiceTests" --nologo` | Passed: 12 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,190 tests. |

## Migration Parity Table - UOW-1107

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.StaticData.itemGroupsData` | `Aion.GameServer.Dataholders.StaticData.QuestBonusItemGroups` | Static Data Root | Partial | Unit Tested | Partial Parity | C# now exposes supported quest bonus item groups from loaded static data. This is narrower than Java `ItemGroupsData`; pet-food and broader group consumers remain unported. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_GROUPS_DATA` | `DataManager.StaticData.QuestBonusItemGroups` | Static Data Dependency | Partial | Unit Tested | Partial Parity | Runtime access exists through `DataManager.StaticData`. It is not Java-style static global field parity, and no production caller consumes it yet. |
| `com.aionemu.gameserver.dataholders.ItemGroupsData.afterUnmarshal` | `StaticData.LoadFromCacheAsync` quest bonus group collection; `QuestBonusItemGroupTable` | Static Data Table / Loader | Partial | Unit Tested | Partial Parity | Loader builds the supported quest bonus group table in stream order. Java pet-food map construction, temporary-list clearing, missing-group null behavior, and broad group families remain unported. |
| `com.aionemu.gameserver.services.reward.BonusService.getBonusGroups` | `QuestBonusItemGroupTable.GetGroupsByBonusType`; static-data bridge | Reward Service Static-Data Lookup | Partial | Unit Tested | Partial Parity | Real-data tests cover supported group counts and absence of BOSS/GATHER/ENCHANT from the quest bonus table. Java runtime `Chance` selection and unsupported-type warnings remain unverified. |
| `game-server/data/static_data/static_data.xml` / `static_data.xsd` | `XmlMerger`; `XmlDataLoader`; `StaticData.LoadFromCacheAsync` | Static XML Import / Loader | Partial | Unit Tested | Partial Parity | Real static-data load now exposes quest bonus groups from the flattened cache. XSD validation timing and invalid XML behavior remain covered only by existing cache validation paths, not Java runtime comparison. |
| `game-server/data/static_data/items/item_groups.xml` / `item_groups.xsd` | `QuestBonusItemGroupXmlProjectionExtractor`; `QuestBonusItemGroupTable`; `StaticData.QuestBonusItemGroups` | Static XML Data | Partial | Unit Tested | Partial Parity | Tests assert 12 supported groups and 4,701 supported items from real data. JAXB schema/after-unmarshal parity and unsupported group consumers remain open. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | future adapter caller using `StaticData.QuestBonusItemGroups.Groups` | Reward Service Integration Dependency | Partial | No Tests | Needs Verification | Static-data input is now available, but production quest finish still does not invoke the adapter or apply live bonus rewards. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestBonusStaticDataBridgeTests.LoadFromCacheAsync_ExposesSupportedQuestBonusItemGroups` | Inline flattened cache exposes supported groups and filters unsupported BOSS from the quest bonus table while preserving all XML item counts. | Source-reviewed from Java `StaticData.itemGroupsData` and `ItemGroupsData`; no Java runtime comparison. |
| `QuestBonusStaticDataBridgeTests.LoadStaticDataAsync_RealDataExposesQuestBonusItemGroupTable` | Real static-data load exposes expected supported group/item counts and excludes BOSS/GATHER/ENCHANT. | Uses real repository XML data through C# `XmlDataLoader`, not Java runtime output. |

## Remaining Risks

- Production quest-finish does not pass `StaticData.QuestBonusItemGroups.Groups` to the disabled adapter yet.
- C# bridge intentionally covers quest bonus groups only, not broad Java `ItemGroupsData` or pet-food `isFood` behavior.
- Java JAXB/schema validation, missing-group/null behavior, after-unmarshal side effects, unsupported-type warnings, and collection ordering remain only partially covered.
- Dynamic handler dispatch, Java RNG/Chance behavior, selected `QuestItems`, random count rolls, live item mutation, packet sends, persistence, rollback, and Java runtime comparison remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 partial static-data bridge for supported quest bonus item groups
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production quest-finish adapter invocation, broad `ItemGroupsData`/pet-food parity, Java JAXB/schema validation, dynamic handler dispatch, Java RNG/Chance selection, selected item/count creation, live reward mutation, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a read-only quest-finish adapter caller-input audit: map where the production socket/quest-finish path can source reward projection, player race, `PlayerQuestState`, `StaticData.ItemTemplates`, `StaticData.QuestBonusItemGroups`, and handler loaded-state inputs before invoking `QuestBonusRewardPlanningInputAdapterService`.

Keep adapter invocation and live rewards disabled until that call boundary is explicit.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Quest-finish adapter caller-input audit | read-only audit doc | Low/Medium | Recommended next unit before production invocation. |
| B | Disabled quest-finish operation descriptor for bonus report | new service/tests | Medium | Only after caller-input audit confirms inputs. |
| C | Java handler exception/failure-ordering audit | read-only Java analysis | Low | Needed before dynamic handler dispatch. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Audit quest-finish caller inputs | Read-only inspection or new audit doc | `StaticData.cs`, adapter service, production quest-finish call sites |
| Orchestrator | Review audit and update docs | Phase 6 docs after audit | Production wiring unless selected exclusively |

## Do Not Parallelize

- `QuestBonusRewardPlanningInputAdapterService.cs`: adapter contract should remain stable.
- `QuestFinishOperationPlanService.cs`, `GameServerConnection.cs`, and `Program.cs`: production quest-finish/runtime surfaces.
- `StaticData.cs`: bridge is new; avoid simultaneous edits unless selected exclusively.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1098 through UOW-1107 are non-live quest bonus preparation units.
- Supported item groups are now available as `StaticData.QuestBonusItemGroups`.
- The adapter still is not invoked from production quest finish.
- Next safest unit is an audit of the production quest-finish call boundary and which inputs are truly available there.
