# Phase 6SZ Completion - UOW-1008 NPC Faction Assigned Quest Guard

## Scope

UOW-1008 stages the Java `QuestService.startQuest` NPC faction assignment guard on the loaded `PlayerNpcFactionsSnapshot`. This unit does not wire a C# quest-start service because that boundary is not ported yet, and it intentionally does not add the guard to nearby marker start-condition checks because Java does not enforce assigned `questId` there.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Assigned NPC faction quest guard | `QuestService.startQuest`, `NpcFaction.getQuestId`, `NpcFaction.isActive` | `PlayerNpcFactionState.cs`, new snapshot tests, docs | Predicate/helper staging | Selected local-only | Low | Small guard can be staged without inventing a full quest-start service. |
| B | Full quest-start service boundary | `QuestService.startQuest`, quest state mutation, packets | Future service/connection/repository files | Service port | Not with A | High | Requires quest mutation, list-size checks, packet sends, and persistence. |
| C | NPC faction daily assignment/mutation | `NpcFactions.startQuest`, `completeQuest`, `reset` | Future faction service/repository files | Service port | No | High | Needs lifecycle writes, packets, reset timing, and quest assignment selection. |
| D | Quest-finish repeat-date audit | `QuestService.calculateRepeatDate`, `QuestState.setNextRepeatTime` | Read-only report/tests | Java analysis | Yes | Medium | Independent date/time surface; still pending. |

Selected batch: local-only A. No sub-agent was spawned because this was a narrow model/helper addition.

## Java Breadcrumbs

- `QuestService.checkStartConditions` only requires the faction row to exist and be active after checking the non-time-based slot cooldown.
- `QuestService.startQuest` has the stricter packet-hack guard for NPC faction quests: the exact faction must be active and `faction.getQuestId() == id`.
- Java logs/audits and rejects when the assigned quest id does not match.

## Implementation

- Added `PlayerNpcFactionsSnapshot.CanStartAssignedQuest(int factionId, int questId)`.
- The helper returns true only when the exact faction row exists, is active, and has the requested assigned quest id.
- Kept nearby predicate behavior unchanged to preserve Java's separation between nearby marker checks and actual quest-start acceptance.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PlayerNpcFactionsSnapshotTests` | Passed, 1 test |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1715 tests |

## Migration Parity Table - UOW-1008

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.startQuest` | Future C# quest-start boundary using `PlayerNpcFactionsSnapshot.CanStartAssignedQuest` | Service / Quest Start Guard | Partial | Unit Tested | Partial Parity | The NPC faction assigned-quest guard is staged as a helper only. Full quest-start mutation, list-size checks, packets, persistence, audit logging, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot` | Player State / Predicate Dependency | Partial | Unit Tested | Partial Parity | Adds active exact-faction plus assigned quest-id guard for future quest-start wiring. Nearby `CanStartQuest` behavior remains separate to match Java. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | DTO / Player State | Partial | Unit Tested | Partial Parity | Existing `QuestId` field is now consumed by a tested guard. Mutation and DB write serialization remain unported. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `PlayerNpcFactionsSnapshotTests.CanStartAssignedQuest_MatchesJavaNpcFactionStartQuestGuard` | Unit | Exact active faction and assigned quest-id match passes; wrong quest id, inactive faction, and missing faction fail. | Deterministic C# helper test from source-reviewed Java `QuestService.startQuest`. No runtime Java comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No C# quest-start service calls this helper yet.
- Java audit logging for packet-hack detection is not modeled.
- NPC faction daily assignment, start/complete/reset mutation, and persistence writes remain unported.
- Live nearby sends and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported: 1 staged helper
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Perform the read-only quest-finish repeat-date calculation audit or continue staging NPC faction lifecycle behavior with daily assignment/mutation boundaries. Keep live sends, production player-controller refresh, faction write paths, and ItemPurification dispatch disabled.
