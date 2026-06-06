# Phase 6 Session 2799 Completion

## Unit Of Work

`[Phase 6][UOW-2799] Wire live quest finish NPC faction completion`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes the non-mentor NPC faction completion state mutation from Java `QuestService.finishQuest`.
- Java source of truth: `QuestService.finishQuest` branch `if (template.getNpcFactionId() != 0) player.getNpcFactions().completeQuest(template)`, plus `NpcFactions.completeQuest`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now calls `PlayerNpcFactionsSnapshot.CompleteActiveQuest` after `SmQuestAction.Update` for templates with `NpcFactionId != 0`, updates `player.NpcFactions`, and persists the completed faction through `PlayerEnterWorldService.PersistNpcFactionUpdateAsync` when the repository service is available.
- Client-visible/state/persistence effect changed: completing a supported NPC-faction quest mutates live active faction state to `Complete` with the next reset timestamp; repository-backed connections also update the existing NPC faction row.
- Why this is not preview-only/test-only/documentation-only: the socket handler now mutates live player NPC faction state and uses the existing database shape for persistence.

## Java Parity Notes

- Java completes the faction after sending `SM_QUEST_ACTION.UPDATE` and after quest completed callbacks, then runs nearby quest refresh.
- C# currently has no live quest-completed callback execution in this path, so the new faction completion runs after `SmQuestAction.Update` and before the existing nearby quest refresh, matching the available runtime ordering.
- This UOW covers non-mentor faction completion state. Java mentor completion additionally updates mentor flag time and sends title info packets; that branch remains deferred until a clean C# packet/state equivalent is wired.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live quest finish side-effect ordering, NPC faction state mutation, and NPC faction persistence wrapper.
- Specific behavior/contract: Java NPC faction quest completion marks the active faction complete with the next reset time after quest update.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishOperationPlanServiceTests|FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` NPC faction completion state mutation.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent NPC faction model/quest finish planning tests.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` for an NPC faction quest, observes quest completion packet ordering, and verifies the live active faction state becomes complete with a reset timestamp.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestFinishOperationPlanServiceTests|FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction"
```

Result: Passed, 82 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` socket-side NPC faction completion.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestCompletesNpcFactionAfterQuestUpdate` | Regression | Java source review: `QuestService.finishQuest` and `NpcFactions.completeQuest` | Live socket quest finish marks active non-mentor NPC faction complete and sends quest update | Filtered C# boundary test with live faction state mutation | Does not cover mentor title packets or repository persistence |
| `CreatePlan_ComposesNpcFactionCompletionAfterCallbackAndBeforeNearbyRefresh` | Unit | Java source review: `QuestService.finishQuest` ordering | Existing planning model keeps NPC faction completion after callback and before nearby refresh | Existing focused C# unit test | Plan-level evidence only |
| `QuestAbandonServiceTests` NPC faction cases | Unit | Java source review: NPC faction start/abort lifecycle | Adjacent NPC faction mutation model remains compatible | Existing focused C# tests | Not finish-specific |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.finishQuest` NPC faction completion branch | `GameServerConnection.CompleteQuestFinishNpcFactionAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Non-mentor active faction completion now mutates live state and persists when repository service exists. |
| `NpcFactions.completeQuest` non-mentor state update | `PlayerNpcFactionsSnapshot.CompleteActiveQuest` | Runtime model state | Partial | Unit Tested / Regression Tested | Partial Parity | Updates active faction state and reset time; mentor flag/title branch remains deferred. |
| `PlayerNpcFactionsDAO.updateNpcFaction` | `PlayerEnterWorldService.PersistNpcFactionUpdateAsync` | Persistence | Partial | Compile/Regression Tested | Partial Parity | Reuses existing repository update shape for completed faction rows. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, regular selectable item rewards, class-selectable item rewards, work-item removal, title rewards, AP rewards, DP rewards, GP rewards, cube expansion rewards, warehouse expansion rewards, and non-mentor NPC faction completion now run live for the guarded branch. Extended selectable rewards, bonus rewards, challenge tasks, quest completion callbacks, mentor NPC faction title/flag side effects, NPC-target dialog quest paths, direct quest-finish reward persistence for some non-item state, and broad nearby quest fanout remain incomplete.
