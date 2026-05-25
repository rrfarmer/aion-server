# Phase 6SY Completion - UOW-1007 NPC Faction Player Hydration

## Scope

UOW-1007 hydrates Java-schema `player_npc_factions` rows into the staged C# `Player.NpcFactions` snapshot during enter-world when static NPC faction metadata is available. This remains a predicate-data slice only: live nearby sends, production player-controller refresh, NPC faction mutation/daily assignment, start-quest assignment guards, and production `CM_ITEM_PURIFICATION` dispatch are still disabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | `player_npc_factions` repository hydration | `PlayerNpcFactionsDAO`, `NpcFaction`, `NpcFactions`, SQL schema | `PlayerEnterWorldRepository.cs`, `PlayerEnterWorldService.cs`, repository/service tests, docs | Repository/service port | Selected local-only | Medium | Depends on UOW-1006 static metadata and touches central enter-world files. |
| B | NPC faction mutation/daily assignment | `NpcFactions`, `NpcFactionService`, faction quest assign/reset flows | Future service/repository mutation files | Service port | Not with A | High | Requires quest assignment, packets, persistence writes, and reset timing. |
| C | Start-quest `questId` guard | `QuestService.checkStartConditions`; `NpcFaction.getQuestId` | Nearby predicate/service tests | Predicate follow-up | Yes after A | Medium | Narrower than mutation, but behavior depends on loaded assigned quest id. |
| D | Quest-finish repeat-date audit | `QuestService.calculateRepeatDate`, `QuestState.setNextRepeatTime` | Read-only report/tests | Java analysis | Yes | Medium | Independent date/time surface; still pending. |

Selected batch: local-only A. No sub-agent was spawned because the unit changed shared repository/service contracts and tests.

## Java Breadcrumbs

- `PlayerNpcFactionsDAO.loadNpcFactions` selects `faction_id`, `active`, `time`, `state`, and `quest_id` from `player_npc_factions` for the player.
- Java wraps the DAO load in a catch/log path and sets a new `NpcFactions` instance on the player.
- `NpcFaction` derives `mentor` through `DataManager.NPC_FACTIONS_DATA.getNpcFactionById(id).isMentor()`.
- `NpcFactions.addNpcFaction` maintains active mentor/non-mentor slots and slot-level `timeLimit`; `time == -1` resets the slot time to current epoch seconds.

## Implementation

- Added `IPlayerEnterWorldRepository.LoadPlayerNpcFactionsAsync`.
- Implemented MySQL hydration from the Java table/column shape.
- Derived the C# `PlayerNpcFactionState.IsMentor` flag from `NpcFactionTable` instead of guessing.
- Parsed Java enum tokens `NOTING`, `START`, and `COMPLETE`, with unknown values failing the load through the existing catch/log path.
- Wired `PlayerEnterWorldService` to hydrate `player.NpcFactions` after quests when `GameServerRuntimeContext.DataManager.StaticData.NpcFactions` is present.
- Kept the no-static-data path fail-closed by leaving the default empty snapshot in place.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests"` | Passed, 27 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1714 tests |

## Migration Parity Table - UOW-1007

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerNpcFactionsAsync` | Repository | Partial | Unit Tested; Integration Tested (gated) | Partial Parity | Reads Java table columns and catch/logs failures to an empty snapshot. Store/insert/update/delete methods, transaction behavior for writes, and Java runtime comparison remain missing. Unknown enum/template failures return an empty snapshot via the catch path, matching the broad Java DAO failure style but not byte/runtime verified. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | DTO / Player State | Partial | Unit Tested; Integration Tested (gated) | Partial Parity | Hydrated rows include id, active flag, mentor flag, time, state, and assigned quest id. Mentor is derived from static metadata like Java. Mutation methods and Java object identity/lifecycle behavior are not modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot` | Player State / Predicate Dependency | Partial | Unit Tested; Integration Tested (gated) | Partial Parity | Repository-loaded snapshots can now feed active slots and slot-level time limits. Daily assignment, join/leave mutation, reset mutation, packet fanout, and persistence writes remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionQuestState`; repository parser | Enum | Partial | Unit Tested; Integration Tested (gated) | Partial Parity | Maps Java tokens `NOTING`, `START`, and `COMPLETE`. DB serialization for writes is not implemented. Unknown tokens fail the load; Java runtime behavior for corrupt DB data has not been captured. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Service | Partial | Unit Tested | Partial Parity | Hydrates NPC factions during enter-world when static data exists. If `DataManager` is absent, C# leaves an empty snapshot; production startup normally registers the runtime context, but this fallback is documented as a staged safety path. |
| `com.aionemu.gameserver.dataholders.NpcFactionsData` | `Aion.GameServer.Dataholders.NpcFactionTable` | Dataholder Dependency | Partial | Unit Tested; Regression Tested | Partial Parity | Used by repository hydration to derive mentor flags. Java JAXB/runtime object-graph comparison remains missing. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `PlayerEnterWorldServiceTests.EnterWorld_LoadsNpcFactionsWhenStaticDataIsAvailable` | Unit | Enter-world passes loaded static NPC faction metadata and a current epoch value to the repository, then assigns the returned snapshot to the player. | Source-reviewed Java enter-world DAO hydration order; no Java runtime comparison. |
| `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerNpcFactions_HydratesActiveSlotsAndTimeLimitsAgainstJavaSchema_WhenEnabled` | Gated integration | Java-schema rows hydrate active normal/mentor faction slots and `time == -1` slot time-limit behavior. | Java schema and source-reviewed `NpcFactions.addNpcFaction`; gated unless `AION_GAMESERVER_DB_INTEGRATION=1`, no Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- NPC faction write paths are not ported: store, insert, update, delete, join, leave, and daily reset remain missing.
- Daily quest assignment and `faction.questId` start-condition guard remain unported.
- C# uses optional runtime-context fallback in enter-world tests and leaves an empty snapshot when static data is unavailable; production startup still needs broader live validation.
- Threading/timing parity for `time == -1` uses a current epoch seconds parameter; Java uses `System.currentTimeMillis() / 1000`.
- Serialization parity for writing `ENpcFactionQuestState` back to MySQL is not implemented.
- Live nearby quest sends and ItemPurification automatic dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 4 staged partial artifacts in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add the nearby start-condition `questId` guard for assigned NPC faction quests now that `PlayerNpcFactionState.QuestId` can be hydrated, or perform the read-only quest-finish repeat-date calculation audit. Keep live sends, production player-controller refresh, faction mutation/daily assignment, and ItemPurification dispatch disabled.
