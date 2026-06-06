# Phase 6 Session 2759 Completion

## Unit of Work

[Phase 6][UOW-2759] Send live legion challenge task lists

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_CHALLENGE_LIST` owner type `1` now dispatches a live LEGION challenge-list path instead of being deferred.
- Java source of truth: `CM_CHALLENGE_LIST.runImpl`, `ChallengeTaskService.showTaskList`, `ChallengeTaskService.buildTaskList`, `ChallengeTasksDAO.load/storeTask`, `ChallengeTask.isCompleted`, and `SM_CHALLENGE_LIST.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleChallengeListAsync`, `ChallengeTaskService.BuildLegionTaskListAsync`, `SmChallengeList`, `ChallengeTaskTable`, static-data challenge task loading, and `IPlayerEnterWorldRepository.SaveNewLegionChallengeTaskAsync`.
- Client-visible/state/persistence effect changed: a live legion challenge-list request can create newly available Java static-data tasks in the existing `challenge_tasks` table and sends real `SM_CHALLENGE_LIST` action `2` plus action `7` packets to the client.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred client-packet path, persists runtime challenge-task rows, and sends real server packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_CHALLENGE_LIST.java`
- `game-server/src/com/aionemu/gameserver/services/ChallengeTaskService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CHALLENGE_LIST.java`
- `game-server/src/com/aionemu/gameserver/dao/ChallengeTasksDAO.java`
- `game-server/src/com/aionemu/gameserver/model/challenge/ChallengeTask.java`
- `game-server/src/com/aionemu/gameserver/model/challenge/ChallengeQuest.java`
- `game-server/src/com/aionemu/gameserver/model/templates/challenge/ChallengeTaskTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/challenge/ChallengeQuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/model/templates/challenge/ChallengeType.java`

## C# Runtime Changes

- Added `SmChallengeList` with Java packet opcode `280` and action `2` / action `7` serialization.
- Wired `CmChallengeList` owner type `1` in `GameServerConnection`.
- Extended challenge static-data loading with Java template fields needed by `buildTaskList`: `repeat`, `prev_task`, and quest `score`.
- Added runtime challenge task state objects and Java-equivalent LEGION task-list building: load stored tasks, return repeatable/incomplete tasks, create available race/level templates, and persist new quest rows.
- Added repository insertion for new legion challenge tasks using the existing `challenge_tasks` schema.
- Left TOWN challenge-list requests deferred because they require town runtime state not scoped into this UOW.

## Validation Decision

- Changed surface: live client-packet dispatch, server-packet serialization, challenge static-data loading, repository persistence, and the already-live level-up challenge gate's shared data model.
- Specific behavior/contract: Java `CM_CHALLENGE_LIST` LEGION requests send `SM_CHALLENGE_LIST(2)` for available tasks and one `SM_CHALLENGE_LIST(7)` per task, creating new owner task rows for available race/level templates.
- Focused C# commands:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmChallengeListTests|FullyQualifiedName~ChallengeTaskServiceTests|FullyQualifiedName~StaticData_LoadsChallengeTasksForLegionLevelGateLikeJavaDataholder" --logger "console;verbosity=minimal" --no-restore
```

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_LevelUp|FullyQualifiedName~PlayerEnterWorldServiceTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live packet dispatch plus server-packet serialization and runtime persistence insert.
- Broad .NET decision: skipped after focused validation because the selected tests compiled the affected project and directly exercised the edited packet, live handler branch, task service, XML loader slice, repository fake, and adjacent level-up gate.
- Why this scope is sufficient: the UOW touched one client packet branch plus its direct packet/data/repository dependencies; no shared packet primitive, crypto primitive, scheduler, or schema migration changed.

## Validation Result

- Focused challenge-list C# result: Passed, 11 total, 0 failed, 0 skipped.
- Focused adjacent level-up C# result: Passed, 80 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `TryCreatePacket_RegistersJavaChallengeListOpcodeAsInGameOnly` | Unit | `AionClientPacketFactory` and `CM_CHALLENGE_LIST` | Opcode `232` is registered for in-game state only. | Java source review plus parser assertion. | Does not execute handler. |
| `SmChallengeList_TaskListSerializesJavaActionTwoShape` | Unit | `SM_CHALLENGE_LIST.writeImpl` action `2` | Task-list packet fields, constants, task id, and complete time shape. | Java source review plus byte-level readback. | Current time is injected in test. |
| `SmChallengeList_TaskInfoSerializesJavaActionSevenShape` | Unit | `SM_CHALLENGE_LIST.writeImpl` action `7` | Individual task quest id, repeat count, score, and complete count serialization. | Java source review plus byte-level readback. | Does not validate client UI. |
| `BuildLegionTaskListAsync_CreatesStoresAndReturnsAvailableRaceTaskLikeJava` | Unit | `ChallengeTaskService.buildTaskList` and `ChallengeTasksDAO.storeTask` | Missing available LEGION race/level task is created, persisted, and returned. | Java source review plus repository-call assertions. | No DB integration. |
| `HandleInfrastructurePacketAsync_LegionChallengeListCreatesStoresAndSendsLikeJava` | Unit | `CM_CHALLENGE_LIST.runImpl`, `showTaskList`, and `SM_CHALLENGE_LIST` | Live handler creates/stores a legion task and sends action `2` plus action `7` packets. | Live handler and packet-byte assertions. | No real client validation. |
| `HandleInfrastructurePacketAsync_ChallengeListTownRequestStaysDeferredWithoutPackets` | Unit | `CM_CHALLENGE_LIST.runImpl` TOWN branch | TOWN requests are not claimed as ported in this LEGION-scope UOW. | Live handler assertion. | TOWN runtime remains deferred. |
| `StaticData_LoadsChallengeTasksForLegionLevelGateLikeJavaDataholder` | Unit | `ChallengeTaskTemplate` / `ChallengeQuestTemplate` | Runtime XML loads task repeat/prev capability and quest score data used by live challenge lists. | XML loader assertion. | Minimal fixture, not full XML count verification. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHALLENGE_LIST` | `CmChallengeList` and `GameServerConnection.HandleChallengeListAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | LEGION owner type is live; TOWN owner type remains deferred. Java ignores client action/player/date fields for dispatch; C# follows that for LEGION. |
| `com.aionemu.gameserver.services.ChallengeTaskService.showTaskList/buildTaskList` | `ChallengeTaskService.BuildLegionTaskListAsync` | Service Logic | Partial | Unit Tested | Partial Parity | LEGION task list build/create/send path is ported. TOWN list, quest-finish updates, rewards, and in-memory service cache fanout remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CHALLENGE_LIST` | `SmChallengeList` | Server Packet | Partial | Unit Tested | Partial Parity | Action `2` and `7` shapes are covered for task list/info. Other owner types use the same packet but are not wired live yet. |
| `com.aionemu.gameserver.dao.ChallengeTasksDAO` | `IPlayerEnterWorldRepository.LoadLegionChallengeTasksAsync` and `SaveNewLegionChallengeTaskAsync` | Repository | Partial | Unit Tested | Needs Verification | Existing row load and new LEGION task insert are implemented against Java schema; update-on-quest-finish and DB integration remain missing. |
| `com.aionemu.gameserver.model.templates.challenge.ChallengeTaskTemplate` | `ChallengeTaskTable` / `ChallengeTaskSummary` | Static Data | Partial | Unit Tested | Partial Parity | Fields required for LEGION list creation and level-up checks are loaded. Reward/contrib/town-residence behavior remains unported. |
| `com.aionemu.gameserver.model.challenge.ChallengeTask` / `ChallengeQuest` | `ChallengeTaskState` / `ChallengeQuestState` | Runtime State | Partial | Unit Tested | Partial Parity | Completion and list serialization state are represented. Persistent state enum, synchronized increment, and reward lifecycle are not ported. |

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported in this UOW: 6 partial runtime/packet/data/repository artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 6
- Total blocked artifacts: 2 (`CM_CHALLENGE_LIST` TOWN branch, live quest-finish challenge progress/reward updates)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- `CM_CHALLENGE_LIST` TOWN branch remains deferred.
- Java challenge quest finish updates, legion challenge-score increases, completion rewards, mail rewards, and online legion fanout remain unported.
- Challenge task service does not keep Java's in-memory `cityTasks`/`legionTasks` cache across calls; C# reloads/persists through the repository for the scoped live path.
- No DB integration test was run for `SaveNewLegionChallengeTaskAsync`.
- No real client validation was performed.
