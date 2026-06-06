# Phase 6 Session 2822 Completion

## Unit Of Work

`[Phase 6][UOW-2822] Wire challenge task online legion refresh after progress update`

## Runtime Progress Gate

- Deferred/live behavior advanced: after a live LEGION challenge-task quest finish updates progress, online same-legion players now receive refreshed challenge-task list packets.
- Java source of truth: `ChallengeTaskService.onLegionTaskFinish` increments challenge quest progress, then calls `player.getLegion().getOnlinePlayers().forEach(p -> showTaskList(p, ChallengeType.LEGION, legionId))`; `showTaskList` sends `SM_CHALLENGE_LIST` action 2 and action 7 packets when `CustomConfig.CHALLENGE_TASKS_ENABLED` is true.
- C# runtime artifact wired: `ChallengeTaskService.OnChallengeQuestFinishAsync` now returns updated available LEGION task state, and `GameServerConnection.CompleteChallengeTaskQuestAsync` fans out `SmChallengeList.TaskList` plus `TaskInfo` packets to the completing player and online same-legion players.
- Client-visible/state/persistence effect changed: completing a modeled LEGION challenge-task quest still persists progress and now sends real `SmChallengeList` refresh packets from live quest finish code when challenge tasks are enabled.
- Why this is not preview-only/test-only/documentation-only: it sends real server packets from the live quest finish runtime path after a persisted challenge-task progress mutation.

## Java Parity Notes

- The packet send gate intentionally follows Java `showTaskList`: progress persistence can happen regardless of the challenge-task display config, but the list refresh only sends when challenge tasks are enabled.
- C# includes the active player even when the test registry does not enumerate it; in the real socket server the active player should already be registered.
- The refreshed task list uses the updated persisted row state returned by the service rather than reloading a Java-style in-memory `legionTasks` map.
- Legion member challenge score and completion reward mail remain incomplete.
- No narrow Java/Maven fixture was found for challenge-task list refresh; Java source was reviewed directly.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/ChallengeTaskService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ChallengeTaskServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live quest finish packet fanout after persistence.
- Specific behavior/contract: a `CHALLENGE_TASK` quest finish updates LEGION task progress and sends Java-shaped challenge list/action-info packets to the completing player and online same-legion members when challenge tasks are enabled.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~CmChallengeListTests|FullyQualifiedName~ChallengeTaskServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for this service callback/fanout. Java source was reviewed directly.
- Broad-validation trigger: live packet fanout from quest finish.
- Broad .NET decision: skipped after focused validation passed; packet serialization primitives and shared connection registry implementation were not changed.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~CmChallengeListTests|FullyQualifiedName~ChallengeTaskServiceTests"
```

Result: Passed, 50 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for this callback/fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `OnChallengeQuestFinishAsync_UpdatesLoadedLegionQuestProgressLikeJava` | Unit | `ChallengeTaskService.onLegionTaskFinish` and `showTaskList` input state | Progress update returns available task state with incremented count/time | Focused C# service test | Does not cover Java in-memory map behavior |
| `HandleDialogSelectAsync_ReportableChallengeTaskQuestRefreshesOnlineLegionTaskListLikeJava` | Runtime boundary | `onLegionTaskFinish -> showTaskList` | Live quest finish sends action 2 and action 7 challenge-list packets to active and online same-legion players after persistence | Focused C# socket boundary test plus `SmChallengeList` shape assertions | Does not cover all online legion infrastructure or completion reward mail |
| `SmChallengeList_TaskListSerializesJavaActionTwoShape` / `SmChallengeList_TaskInfoSerializesJavaActionSevenShape` | Packet regression | `SM_CHALLENGE_LIST.writeImpl` | Existing packet serialization shapes remain covered for action 2 and action 7 | Focused packet tests in `CmChallengeListTests` | Packet golden is C# asserted from Java source, not Java-generated bytes |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.ChallengeTaskService.onLegionTaskFinish` | `Aion.GameServer.Services.ChallengeTaskService.OnChallengeQuestFinishAsync` | Service | Partial | Unit/Runtime Tested | Partial Parity | Returns updated available task state after persistence so live fanout can mirror Java; score and reward mail remain incomplete. |
| `com.aionemu.gameserver.services.ChallengeTaskService.showTaskList` | `Aion.GameServer.Network.Aion.GameServerConnection.SendChallengeTaskListRefreshAsync` | Runtime packet fanout | Partial | Runtime Tested | Partial Parity | Sends modeled LEGION task-list/task-info packets to active and online same-legion players when challenge tasks are enabled; TOWN remains deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CHALLENGE_LIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmChallengeList` | Packet | Partial | Regression Tested | Partial Parity | Existing action 2/action 7 packet shape tests cover this fanout's packet types. |

## Known Gaps

- TOWN challenge-task finish remains deferred.
- Legion member challenge score is still not modeled or persisted.
- Challenge-task completion reward mail is not wired.
- Java's in-memory `legionTasks` owner map is not modeled; C# uses updated persisted rows as the refresh source.
- Online same-legion enumeration is limited to the current `IGameClientConnectionRegistry` player snapshot.
- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
