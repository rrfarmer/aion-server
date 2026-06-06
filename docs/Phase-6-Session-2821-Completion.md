# Phase 6 Session 2821 Completion

## Unit Of Work

`[Phase 6][UOW-2821] Wire challenge task completion from live quest finish`

## Runtime Progress Gate

- Deferred/live behavior advanced: completed quest templates with category `CHALLENGE_TASK` now invoke modeled challenge-task completion from the live quest finish handler.
- Java source of truth: `QuestService.finishQuest` calls `ChallengeTaskService.getInstance().onChallengeQuestFinish(player, id)` for `QuestCategory.CHALLENGE_TASK`; Java `ChallengeTaskService.onLegionTaskFinish` locates the task by quest id, skips players without a legion, checks the loaded task quest progress, increments the quest complete count, updates completion time, and stores the task row through `ChallengeTasksDAO.storeTask`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now calls `ChallengeTaskService.OnChallengeQuestFinishAsync`, `ChallengeTaskTable` can resolve tasks by quest id, and `IPlayerEnterWorldRepository` can update existing LEGION challenge-task progress rows.
- Client-visible/state/persistence effect changed: live completion of a modeled LEGION challenge-task quest persists a new challenge-task `complete_count` and `complete_time` before normal quest-state completion packets continue.
- Why this is not preview-only/test-only/documentation-only: it wires a live quest finish callback that mutates database-backed runtime challenge-task state.

## Java Parity Notes

- This UOW ports the safe persisted LEGION progress slice of Java `ChallengeTaskService.onChallengeQuestFinish`.
- C# uses the existing repository-loaded `challenge_tasks` rows instead of Java's in-memory `legionTasks` map; the persisted row still follows the Java DAO shape.
- TOWN challenge-task completion remains deferred because the C# town task accept/runtime state is not modeled.
- Legion member challenge score, online legion task-list refresh, and completion reward mail are not yet wired.
- No narrow Java/Maven fixture was found for `ChallengeTaskService.onChallengeQuestFinish`; Java source was reviewed directly.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/ChallengeTaskTable.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ChallengeTaskService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ChallengeTaskServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Validation Decision

- Changed surface: live quest finish callback and persistence side effect for challenge tasks.
- Specific behavior/contract: a `CHALLENGE_TASK` quest finish increments an existing LEGION challenge-task quest row and still sends the normal quest completion packet sequence.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~ChallengeTaskServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this service callback. Java source was reviewed directly.
- Broad-validation trigger: live quest finish callback plus repository contract extension.
- Broad .NET decision: skipped after focused validation passed; the repository interface extension compiled all known implementers in the focused test project, and no shared packet serializer/schema migration was changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~ChallengeTaskServiceTests"
```

Result: Passed, 44 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this callback.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `OnChallengeQuestFinishAsync_UpdatesLoadedLegionQuestProgressLikeJava` | Unit | `ChallengeTaskService.onLegionTaskFinish` and `ChallengeTasksDAO.storeTask` | A loaded LEGION task quest below max repeat increments and persists count/time | Focused C# service test with recording repository | Does not cover score, online task list refresh, reward mail |
| `OnChallengeQuestFinishAsync_DoesNotUpdateCompletedLegionQuestLikeJava` | Unit | `onLegionTaskFinish` max-repeat guard | Completed quest progress is not persisted again | Focused C# service test | Does not cover task-level completion reward side effects |
| `HandleDialogSelectAsync_ReportableChallengeTaskQuestUpdatesLegionProgressLikeJava` | Runtime boundary | `QuestService.finishQuest` challenge-task callback | Live dialog quest finish invokes challenge-task completion and still emits normal completion packets | Focused C# socket boundary test through `HandleDialogSelectAsync` | C# does not yet refresh online legion task lists |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.finishQuest` challenge-task hook | `GameServerConnection.CompleteChallengeTaskQuestAsync` | Quest finish runtime callback | Partial | Runtime Tested | Partial Parity | Hook runs for `CHALLENGE_TASK` templates after rewards and before final quest completion mutation. |
| `ChallengeData.getTaskByQuestId` | `ChallengeTaskTable.GetTaskByQuestId` | Runtime static data lookup | Partial | Unit/Runtime Tested | Partial Parity | Resolves modeled challenge task templates by quest id. |
| `ChallengeTaskService.onLegionTaskFinish` | `ChallengeTaskService.OnChallengeQuestFinishAsync` | Challenge task service | Partial | Unit/Runtime Tested | Partial Parity | Persists LEGION quest count/time; score, packet fanout, and mail rewards remain incomplete. |
| `ChallengeTasksDAO.storeTask` update branch | `IPlayerEnterWorldRepository.SaveLegionChallengeTaskProgressAsync` and MySQL implementation | Persistence | Partial | Unit/Compile Tested | Partial Parity | Updates existing `challenge_tasks` rows for LEGION owners using the existing table shape. |

## Known Gaps

- TOWN challenge-task finish remains deferred.
- Legion member challenge score is not modeled or persisted by this C# path.
- Online legion members are not sent refreshed `SmChallengeList` packets after progress changes.
- Challenge-task completion reward mail is not wired.
- Java's in-memory `legionTasks` owner map is not modeled; C# uses existing persisted rows as the runtime source.
- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
