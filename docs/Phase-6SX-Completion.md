# Phase 6SX Completion - UOW-1006 NPC Faction Static Data

## Scope

UOW-1006 adds the static NPC faction table needed before Java-schema `player_npc_factions` rows can be hydrated into the staged player NPC faction snapshot. This remains offline: no repository hydration, live nearby sends, production player-controller refresh, or production `CM_ITEM_PURIFICATION` dispatch is enabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | NPC faction static-data table | `NpcFactionsData`, `NpcFactionTemplate`, `npc_factions.xml` | `StaticData.cs`, `NpcFactionTable.cs`, static-data tests, docs | Dataholder/XML port | Not with repo hydration in same files | Low | Needed before DB hydration can derive Java mentor flags. |
| B | `player_npc_factions` repository hydration | `PlayerNpcFactionsDAO`, SQL schema | `PlayerEnterWorldRepository.cs`, `PlayerEnterWorldService.cs`, repository/service tests | Repository port | Depends on A | Medium | Java `NpcFaction` derives mentor from static metadata. |
| C | Broader archetype real-data projection audit | Java nearby predicate/data | real-data audit tests | Test creation | Yes after A/B settle | Medium | Baselines depend on player faction/config assumptions. |
| D | Quest-finish repeat-date audit | `QuestService.calculateRepeatDate`, `QuestState.setNextRepeatTime` | Read-only report | Java analysis | Yes | Medium | Independent but larger date/time surface. |

Selected batch: local-only A. No sub-agent was spawned because this unit touched central static-data parsing plus one new table and tests.

## Java Breadcrumbs

- `NpcFactionsData.afterUnmarshal` indexes `NpcFactionTemplate` rows by faction id and registrar NPC id.
- `NpcFactionTemplate.isMentor()` returns true only when `category == FactionCategory.MENTOR`.
- `NpcFaction` constructor derives its `mentor` flag from `DataManager.NPC_FACTIONS_DATA.getNpcFactionById(id).isMentor()`.

## Implementation

- Added `NpcFactionTable` and `NpcFactionSummary`.
- Added `StaticData.NpcFactions`.
- Parsed `<npc_faction>` rows from merged static data, including `npc_ids`, `category`, Java default `max_level = 99`, and `skill_points`.
- Added lookup helpers for faction id, registrar NPC id, and mentor-category checks.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests.StaticData_LoadsNpcFactionTemplatesLikeJavaDataholder|FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts"` | Passed, 2 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1712 tests |

## Migration Parity Table - UOW-1006

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.NpcFactionsData` | `Aion.GameServer.Dataholders.NpcFactionTable`; `StaticData.NpcFactions` | Dataholder / Static Data Table | Partial | Unit Tested; Regression Tested | Partial Parity | C# indexes faction templates by faction id and registrar NPC id like Java `afterUnmarshal`. Production startup loading is covered through existing `DataManager.LoadAsync` real-data test, but Java JAXB runtime comparison remains missing. |
| `com.aionemu.gameserver.model.templates.factions.NpcFactionTemplate` | `Aion.GameServer.Dataholders.NpcFactionSummary` | DTO / Static Data Template | Partial | Unit Tested; Regression Tested | Partial Parity | C# preserves id, name, name id, category, min/max level, race, NPC ids, and skill points. Java `maxLevel` default 99 and mentor-category detection are unit-tested. Full `FactionCategory`/`Race` enums are represented as strings. |
| `com.aionemu.gameserver.model.templates.factions.FactionCategory` | `NpcFactionSummary.Category`; `NpcFactionSummary.IsMentor` | Enum-like XML Dependency | Partial | Unit Tested | Partial Parity | C# preserves category token as a string and implements only the `MENTOR` predicate needed for NPC faction snapshot hydration. Full enum modeling remains out of scope. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `StaticDataLoadingTests.StaticData_LoadsNpcFactionTemplatesLikeJavaDataholder` | Unit | Id lookup, NPC-id lookup, default max level, mentor category, skill points, and missing lookups. | Source-reviewed Java dataholder/template behavior. |
| `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts` | Regression | Current repository `npc_factions.xml` loads 18 templates with expected mentor/daily/skill metadata. | Real C# load over Java XML data; no Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `player_npc_factions` repository hydration still is not implemented.
- Category/race values are string tokens, not full Java enums.
- Daily assignment, `questId` start guard, join/leave mutation, packet fanout, and persistence states remain unported.
- Live nearby sends and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported: 2 staged partial artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 3
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 70%

## Next Recommended Unit Of Work

Hydrate `player_npc_factions` into `Player.NpcFactions` using `StaticData.NpcFactions` to derive mentor flags, with Java-schema integration coverage where possible. Keep live sends, production player-controller refresh, and ItemPurification dispatch disabled.
