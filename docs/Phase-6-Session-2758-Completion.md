# Phase 6 Session 2758 Completion

## Unit of Work

[Phase 6][UOW-2758] Check live legion level challenge tasks

## Runtime Progress Gate

- Deferred/live behavior advanced: level 5+ `CM_LEGION` exOpcode `0x0E` level-up requests now use Java-equivalent loaded challenge-task completion checks instead of being unconditionally denied.
- Java source of truth: `ChallengeTaskService.canRaiseLegionLevel`, `ChallengeTasksDAO.load`, `ChallengeTask.isCompleted`, `ChallengeTaskTemplate.isLegionLevelTask`, `ChallengeTaskTemplate.getMinLevel`, and `LegionRestrictions.canChangeLevel`.
- C# runtime artifact wired/fixed: `StaticData.ChallengeTasks`, `ChallengeTaskTable`, `ChallengeTaskService.CanRaiseLegionLevel`, `IPlayerEnterWorldRepository.LoadLegionChallengeTasksAsync`, and `GameServerConnection.HandleLegionLevelUpAsync`.
- Client-visible/state/persistence effect changed: a level 5+ Brigade General with completed loaded legion-level challenge tasks can continue into the existing live level-up mutation, history insert, and level-up packets; incomplete or missing loaded tasks still send `SM_SYSTEM_MESSAGE 904452`.
- Why this is not preview-only/test-only/documentation-only: this UOW changes the live packet handler branch and uses runtime Java XML plus existing `challenge_tasks` DB rows to decide whether live legion state/persistence/packets execute.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/ChallengeTaskService.java`
- `game-server/src/com/aionemu/gameserver/dao/ChallengeTasksDAO.java`
- `game-server/src/com/aionemu/gameserver/model/templates/challenge/ChallengeTaskTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/challenge/ChallengeTask.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/sql/aion_gs.sql`
- `game-server/data/static_data/challenge_tasks.xml`

## C# Runtime Changes

- Added a runtime challenge-task table populated from Java `challenge_tasks.xml` with task id, type, race, level bounds, legion-level flag, and quest repeat counts.
- Added a challenge-task service that mirrors Java's loaded-task behavior: only loaded DB task ids are considered, then their static templates are filtered by `LEGION`, `legion_level_task`, and current legion level.
- Added repository loading for existing `challenge_tasks` rows where `owner_id = legionId` and `owner_type = 'LEGION'`.
- Replaced the Phase 2757 conservative level 5+ denial with the live challenge-task check inside `HandleLegionLevelUpAsync`.

## Validation Decision

- Changed surface: static-data loading, live client-packet branch, repository read contract, level-up state/persistence gate, and server-packet output.
- Specific behavior/contract: Java `ChallengeTaskService.canRaiseLegionLevel` returns false when no loaded required tasks exist, false when any required quest is incomplete, and true when all quests in a loaded required legion-level task meet `repeat_count`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ChallengeTaskServiceTests|FullyQualifiedName~HandleInfrastructurePacketAsync_LevelUp|FullyQualifiedName~StaticData_LoadsChallengeTasksForLegionLevelGateLikeJavaDataholder|FullyQualifiedName~PlayerEnterWorldServiceTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java test fixture exists for this service path.
- Broad-validation trigger: live dispatch/state/persistence gate plus static-data loading and DB read contract.
- Broad .NET decision: skipped after focused validation because the selected tests compiled the affected project and directly exercised the edited service, live handler branch, XML loader slice, and repository fake contract.
- Why this scope is sufficient: the UOW touched a single already-live legion level-up gate and its direct data/repository dependencies; no shared socket primitive, packet framing primitive, scheduler, or schema migration changed.

## Validation Result

- Focused C# result: Passed, 85 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CanRaiseLegionLevel_ReturnsFalseWhenNoLoadedLegionLevelTaskMatchesJava` | Unit | `ChallengeTaskService.canRaiseLegionLevel` | Missing loaded required task rows deny the level-up gate. | Java source review plus service assertion. | No DB integration. |
| `CanRaiseLegionLevel_ReturnsFalseWhenLoadedTaskQuestIsIncompleteLikeJava` | Unit | `ChallengeTask.isCompleted` | A loaded task with any incomplete quest denies the gate. | Java source review plus repeat-count assertion. | No quest-progress mutation path. |
| `CanRaiseLegionLevel_ReturnsTrueWhenLoadedTaskQuestsAreCompleteLikeJava` | Unit | `ChallengeTask.isCompleted` | A loaded required task with all quest counts complete allows the gate. | Java source review plus service assertion. | No real client validation. |
| `CanRaiseLegionLevel_DoesNotRequireOtherRaceTaskWhenOnlyOneLevelTaskIsLoadedLikeJava` | Unit | `ChallengeTasksDAO.load` and `ChallengeTaskService.canRaiseLegionLevel` | Static tasks for another race are not required when they are not loaded for the legion. | Java loaded-map behavior plus service assertion. | Race-specific task creation remains outside this UOW. |
| `StaticData_LoadsChallengeTasksForLegionLevelGateLikeJavaDataholder` | Unit | `challenge_tasks.xml` and `ChallengeTaskTemplate` | Java XML task/quest attributes load into runtime C# structures used by the gate. | XML loader assertion. | Minimal fixture, not full XML count verification. |
| `HandleInfrastructurePacketAsync_LevelUpRejectsMissingChallengeTaskForLevelFivePlusLikeJavaDefault` | Unit | `LegionRestrictions.canChangeLevel` and `ChallengeTaskService.canRaiseLegionLevel` | Live level-up branch loads legion challenge rows and sends `904452` when no required task is complete. | Live handler assertion. | No real client validation. |
| `HandleInfrastructurePacketAsync_LevelUpWithCompletedChallengeTasksMutatesLikeJava` | Unit | `ChallengeTaskService.canRaiseLegionLevel` and `LegionService.changeLevel` | Completed task rows allow level 5 to 6 mutation, Kinah decrement, `LEVEL_UP` history, and level-up packets. | Live handler and packet assertions. | No DB integration. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.ChallengeTaskService.canRaiseLegionLevel` | `ChallengeTaskService.CanRaiseLegionLevel` | Service Logic | Partial | Unit Tested | Partial Parity | Level-up gate behavior is ported for loaded legion task rows. Task-list display and quest-finish progress updates remain unported. |
| `com.aionemu.gameserver.dao.ChallengeTasksDAO.load` | `IPlayerEnterWorldRepository.LoadLegionChallengeTasksAsync` and `MySqlPlayerEnterWorldRepository` | Repository | Partial | Unit Tested | Needs Verification | Reads existing Java `challenge_tasks` rows for `owner_type = 'LEGION'`; no DB integration test was run. |
| `com.aionemu.gameserver.model.templates.challenge.ChallengeTaskTemplate` | `ChallengeTaskTable` and `StaticData.ChallengeTasks` | Static Data | Partial | Unit Tested | Partial Parity | Loads the fields required for level-up gating from Java XML. Full challenge task template behavior is not ported. |
| `com.aionemu.gameserver.model.challenge.ChallengeTask` | `ChallengeTaskProgressRow` plus `ChallengeTaskService` repeat-count checks | Runtime State | Partial | Unit Tested | Partial Parity | Completion checks use loaded DB counts and template repeat counts. Runtime mutation of challenge progress is not ported. |
| `com.aionemu.gameserver.services.LegionService.LegionRestrictions` | `GameServerConnection.HandleLegionLevelUpAsync` | Live Handler Gate | Partial | Unit Tested | Partial Parity | Level 5+ challenge-task success and failure now feed the live level-up branch. Other legion restrictions remain as in UOW-2757. |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported in this UOW: 5 partial runtime/data/repository artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 2 (`CM_CHALLENGE_LIST` live packets, quest-finish challenge-task progress mutation)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `ChallengeTaskService.showTaskList` / `SM_CHALLENGE_LIST` are not ported; `CM_CHALLENGE_LIST` remains deferred.
- Java quest-finish challenge-task progress mutation remains represented by existing quest-finish placeholder planning, not live progress updates.
- No DB integration test was run for `LoadLegionChallengeTasksAsync`.
- No real client validation was performed.
- Full challenge task lifecycle, caching, task creation, and reward dispatch remain outside this UOW.
