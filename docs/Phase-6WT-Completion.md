# Phase 6WT Completion - UOW-1106 Quest Bonus Static Data Bridge Audit

Date: May 26, 2026

## Unit Of Work

UOW-1106: `[Phase 6][UOW-1106] Audit quest bonus static data bridge`

## Summary

UOW-1106 audits the Java and C# static-data loading surfaces needed to expose `QuestBonusItemGroupTable` through runtime static data.

The audit confirms that Java owns item groups through `StaticData.itemGroupsData` and `DataManager.ITEM_GROUPS_DATA`, while the current C# loader merges imports into a flattened cache and manually parses supported tables in `StaticData.LoadFromCacheAsync`.

The recommended next bridge is narrow: add a `StaticData.QuestBonusItemGroups` property populated from supported quest bonus group projections while keeping production quest-finish adapter invocation disabled.

No C# loader, `StaticData`, `DataManager`, quest-finish call site, RNG, live reward mutation, packet send, persistence, or rollback behavior was changed.

## Files Changed

- `docs/QuestBonusStaticDataBridge-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WT-Completion.md`

## Validation

| Command | Result |
|---|---|
| Read-only source inspection with `rg` and focused file reads over Java `StaticData`, `DataManager`, `ItemGroupsData`, `BonusService`, C# `DataManager`, `XmlDataLoader`, `StaticData`, and bonus item-group/adapter services | Completed. |
| `git diff --check` | Passed. |

No .NET tests were rerun because this unit is documentation-only. The last full solution validation in UOW-1105 passed 2,188 tests and no C# code changed in this unit.

## Migration Parity Table - UOW-1106

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.StaticData.itemGroupsData` | future `StaticData.QuestBonusItemGroups`; `docs/QuestBonusStaticDataBridge-Audit.md` | Static Data Root / Audit | Partial | Manual Only | Needs Verification | Audit maps Java top-level `item_groups` ownership to the C# `StaticData` constructor/property pattern. No C# loader property added yet. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_GROUPS_DATA` | future `DataManager.StaticData.QuestBonusItemGroups`; `docs/QuestBonusStaticDataBridge-Audit.md` | Static Data Dependency / Audit | Partial | Manual Only | Needs Verification | Audit confirms Java assigns `ITEM_GROUPS_DATA = data.itemGroupsData`. C# still exposes no global/static item-group table. |
| `com.aionemu.gameserver.dataholders.ItemGroupsData` | `QuestBonusItemGroupTable`; future `StaticData` bridge | Static Data Table / Audit | Partial | Manual Only | Needs Verification | Audit documents Java ordered bonus group lists and pet-food side effects. C# table covers quest bonus groups only; pet-food and broader item-group consumers remain unported. |
| `com.aionemu.gameserver.services.reward.BonusService.getBonusGroups` | `QuestBonusItemGroupTable.GetGroupsByBonusType`; future static-data bridge | Reward Service Static-Data Lookup | Partial | Existing Unit Coverage | Partial Parity | Existing table/tests cover supported quest bonus families. Audit recommends exposing the table through `StaticData` without invoking production quest finish. |
| `game-server/data/static_data/static_data.xml` / `static_data.xsd` | `XmlMerger`; `XmlDataLoader`; `StaticData.LoadFromCacheAsync` | Static XML Import / Audit | Partial | Manual Only | Needs Verification | Audit confirms Java imports `items/item_groups.xml` and C# flattened cache already contains imported XML. C# parser does not yet project item groups during load. |
| `game-server/data/static_data/items/item_groups.xml` / `item_groups.xsd` | `QuestBonusItemGroupXmlProjectionExtractor`; `QuestBonusItemGroupTable`; future loader bridge | Static XML Data / Audit | Partial | Existing Unit Coverage | Partial Parity | Existing tests cover real-data supported quest bonus counts. Loader bridge and schema/JAXB invalid XML behavior remain unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| _None_ | Documentation-only static-data bridge audit. | Source inspection only; no runtime comparison. |

## Remaining Risks

- `StaticData.QuestBonusItemGroups` does not exist yet.
- Adding the bridge requires exclusive ownership of the shared `StaticData` loader and focused regression tests.
- A streaming implementation must avoid diverging from the existing `QuestBonusItemGroupXmlProjectionExtractor` semantics.
- Java JAXB/schema validation, `afterUnmarshal` null/missing group behavior, cache/import behavior, collection ordering, and pet-food side effects remain unverified.
- Production quest-finish adapter invocation, dynamic handler dispatch, Java RNG/Chance behavior, selected `QuestItems`, random count rolls, live item mutation, packet sends, persistence, rollback, and Java runtime comparison remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0; 1 read-only bridge audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: static-data bridge implementation, Java JAXB/schema validation, broad `ItemGroupsData`/pet-food parity, production quest-finish wiring, dynamic handler dispatch, Java RNG/Chance selection, live reward mutation, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add the narrow `StaticData.QuestBonusItemGroups` bridge with focused loader tests, preserving existing static-data counts and keeping `QuestBonusRewardPlanningInputAdapterService` explicit and disabled from production quest finish.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Narrow `StaticData.QuestBonusItemGroups` bridge | `StaticData.cs`, focused static-data loader tests | Medium/High | Recommended next unit, but exclusive ownership only. |
| B | Quest-finish adapter caller-input audit | read-only audit doc | Low/Medium | Can map call-site inputs without touching production wiring. |
| C | Java handler exception/failure-ordering audit | read-only Java analysis | Low | Needed before dynamic handler dispatch. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add narrow static-data bridge and tests | `StaticData.cs`, new or focused loader tests, Phase 6 docs | Production quest-finish call sites, adapter invocation, dynamic handler code |
| Explorer A | Optional read-only quest-finish caller-input audit | Read-only inspection or separate audit doc | `StaticData.cs`, loader tests, shared progress docs |

## Do Not Parallelize

- `StaticData.cs`, `XmlDataLoader`, and `DataManager`: exclusive ownership only.
- `QuestBonusItemGroupTable.cs` and `QuestBonusRewardPlanningInputAdapterService.cs`: keep contracts stable unless selected exclusively.
- Production quest-finish call sites: no live adapter invocation until all inputs are explicit.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1098 through UOW-1106 are non-live quest bonus preparation units.
- The static-data bridge audit recommends adding `StaticData.QuestBonusItemGroups` next.
- Existing `QuestBonusItemGroupTable` and real-data count tests should be reused.
- The next bridge should not wire production quest finish; it should only expose supported quest bonus groups from loaded static data.
