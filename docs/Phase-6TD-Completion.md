# Phase 6TD Completion - UOW-1012 Staged Quest-Finish State Mutation

## Scope

UOW-1012 adds a non-sending, non-persistent quest-finish state mutation boundary for the Java `QuestService.finishQuest` state-only effects. It completes `REWARD` quest states, clears quest vars, increments complete count, stamps complete time, and calls the configured-timezone repeat-date calculator for time-based quests.

This unit does not implement quest rewards, inventory changes, `SM_QUEST_ACTION`, reset system messages, quest callbacks, NPC faction completion mutation, nearby refresh, DAO writes, or ItemPurification dispatch.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Staged quest-finish state mutation | `QuestService.finishQuest`, `QuestState.setStatus`, `QuestState.setQuestVar`, `QuestTemplate.isTimeBased` | `QuestFinishStateMutationService.cs`, focused tests, docs | Service Port | Selected sequential | Medium | Uses the just-added repeat-date timezone path and shared quest-state DTO. |
| B | NPC faction completion lifecycle audit | `NpcFactions.completeQuest`, `NpcFaction.completeQuest`, DAO writes | docs/read-only | Java Analysis | Yes | Medium | Independent, but quest-finish state mutation was the direct follow-up to UOW-1011. |
| C | Quest-finish packet boundary audit | `SM_QUEST_ACTION`, reset system messages | docs/read-only | Java Analysis | Yes | Medium | Deferred until state mutation is staged. |
| D | Persistence write boundary | `PlayerQuestListDAO.store/delete` | future repository contract/tests | Repository Port | No | High | Requires deciding update ordering and rollback behavior after state mutation, packets, rewards, and callbacks are staged. |

Selected batch: local-only A. No sub-agent was spawned because the selected work modified shared quest-state behavior.

## Java Breadcrumbs

- `QuestService.finishQuest` returns false when the quest state is missing or not `QuestStatus.REWARD`.
- `QuestService.finishQuest` blocks repeat mission completion when `template.getCategory() == MISSION && qs.getCompleteCount() != 0`.
- `QuestState.setStatus(QuestStatus.COMPLETE)` increments `completeCount` and stamps `completeTime` when transitioning from non-complete status.
- `QuestState.setQuestVar(0)` clears packed quest vars.
- `QuestService.finishQuest` calls `qs.setNextRepeatTime(calculateRepeatDate(player, template))` only when `template.isTimeBased()`.

## Implementation

- Added `QuestFinishStateMutationService.ApplyRewardCompletion`.
- Added `QuestFinishStateMutationStatus` and `QuestFinishStateMutationResult`.
- The staged service returns a new immutable `PlayerQuestState` instead of mutating the input record.
- Preserves flags and reward group while clearing quest vars, matching the Java distinction between quest vars and flags.
- Leaves non-time-based `NextRepeatTime` unchanged because Java only sets the value when the template is time-based.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestFinishStateMutationServiceTests` | Passed, 8 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1734 tests |

## Migration Parity Table - UOW-1012

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishStateMutationService.ApplyRewardCompletion` | Service / Quest Completion Boundary | Partial | Unit Tested | Partial Parity | Stages only the state mutation subset: missing/non-REWARD rejection, repeated mission guard, status completion, var clear, complete-count/time update, and time-based next-repeat calculation. Rewards, inventory, packets, callbacks, NPC faction completion, persistence, and nearby refresh remain unported. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | DTO / Player Quest State | Partial | Unit Tested | Partial Parity | C# immutable record returns a new state instead of Java in-place mutation. Flags and reward group are preserved; quest vars are cleared; `CompleteTime` uses injected `now` for deterministic tests. DAO update-state flags and write behavior remain missing. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary` | DTO / Template Dependency | Partial | Unit Tested | Needs Verification | Reuses staged nearby template fields for category, time-based flag, and repeat-cycle tokens. Full Java `QuestTemplate` JAXB model, reward data, category enum, and production static-data loading remain unported. |
| `com.aionemu.gameserver.services.QuestService.calculateRepeatDate` | `Aion.GameServer.Services.QuestRepeatDateService.CalculateNextRepeatTime` | Utility / Quest Timing | Partial | Unit Tested | Partial Parity | Now called by the staged finish-state service for time-based quests through `GameServerOptions`. Reset system messages, persistence, SQL timestamp conversion, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle` | `NearbyQuestTemplateSummary.RepeatCycle`; `QuestRepeatDateService` | Enum / XML Token Dependency | Partial | Unit Tested | Partial Parity | Weekly/daily token behavior is exercised through the finish-state service. Full enum modeling and reset-message l10n values remain unported. |

## Tests Added

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_CompletesRewardQuestLikeJavaQuestState` | Unit | `REWARD` state becomes `COMPLETE`, vars clear, flags/reward group remain, complete count/time update. | Source-reviewed Java `finishQuest`, `QuestState.setStatus`, and `setQuestVar`. |
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_SetsNextRepeatTimeForTimeBasedQuest` | Unit | Time-based completion calls the repeat-date calculator with configured timezone options. | Source-reviewed Java `template.isTimeBased()` and `calculateRepeatDate` call. |
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_RejectsMissingQuestState` | Unit | Missing state returns the staged missing-state status. | Source-reviewed Java null guard. |
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_RejectsNonRewardQuestState` | Unit | `START`, `COMPLETE`, and `LOCKED` states are not completed. | Source-reviewed Java `QuestStatus.REWARD` guard. |
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_RejectsRepeatedMissionCompletionLikeJava` | Unit | Mission with nonzero complete count is rejected. | Source-reviewed Java mission repeat guard. |
| `QuestFinishStateMutationServiceTests.ApplyRewardCompletion_AllowsFirstMissionCompletion` | Unit | First mission completion is allowed and updates state. | Source-reviewed Java mission guard. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- This is not wired to a production quest-finish packet handler or reward path.
- No DAO persistence, update-status flag, transaction ordering, or rollback behavior is implemented.
- No `SM_QUEST_ACTION`, reset system message, quest-completed callback, NPC faction completion, or nearby-refresh side effect is implemented.
- C# returns a new immutable `PlayerQuestState`; Java mutates `QuestState` in place. This is an intentional C# representation difference, but production caller semantics still need verification.
- Template category and repeat-cycle data come from staged summary fields, not full Java `QuestTemplate` static-data loading.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 2 staged partial artifacts in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Audit and then stage the NPC faction completion side of `QuestService.finishQuest`, specifically `player.getNpcFactions().completeQuest(template)`, while keeping quest rewards, packets, DAO writes, live nearby refresh, and ItemPurification dispatch disabled. A second good follow-up is a read-only packet/persistence ordering audit for `SM_QUEST_ACTION`, reset messages, and `PlayerQuestListDAO.store`.

## Next Unit Handoff

Start with `docs/QuestRepeatDate-Audit.md`, this file, and `docs/PHASE-6-PROGRESS.md` Session 1012. Recommended implementation target:

1. Source-audit Java `NpcFactions.completeQuest`, `NpcFaction.completeQuest`, reset behavior, and DAO persistence.
2. Decide whether the next unit should be read-only documentation or a staged pure helper for NPC faction completion mutation.
3. Do not wire the helper into live quest completion yet.
4. Add or update a Migration Parity Table for every Java artifact touched.
5. Run focused tests plus the full `Aion.GameServer.Tests` project before committing.
