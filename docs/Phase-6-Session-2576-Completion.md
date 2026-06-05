# Phase 6 Session 2576 Completion

## UOW

[Phase 6] UOW-2576: Abort NPC-faction quest state during live quest abandon

## Status

Completed and validated with the focused abandon/NPC-faction test filter. The live `CM_DELETE_QUEST` abandon path now
executes the Java `QuestService.abandonQuest -> player.getNpcFactions().abortQuest(template)` side effect by resetting
the matching active NPC-faction quest state to `NOTING` after the quest-list mutation and before quest work-item cleanup.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getNpcFactions().abortQuest(template).
- Java source method or runtime path: QuestService.abandonQuest and NpcFactions.abortQuest.
- C# runtime artifact wired or fixed: PlayerNpcFactionsSnapshot.AbortQuest and QuestAbandonService.AbortNpcFactionQuest.
- Client-visible/state/persistence effect changed: abandoning an NPC-faction quest now mutates live Player.NpcFactions by resetting the active matching faction state to Noting while preserving active/time/quest id.
- Why this is not preview-only/test-only/documentation-only: the live CM_DELETE_QUEST path now mutates Player.NpcFactions state.
```

## Java Source Reviewed

- `QuestService.abandonQuest(Player, int)`:
  - mutates or deletes the player quest state first;
  - if `template.getNpcFactionId() != 0`, calls `player.getNpcFactions().abortQuest(template)`;
  - then removes quest work items and continues the remaining abandon side effects.
- `NpcFactions.abortQuest(QuestTemplate)`:
  - looks up `factions.get(questTemplate.getNpcFactionId())`;
  - returns when the row is missing or inactive;
  - sets `state` to `ENpcFactionQuestState.NOTING`;
  - calls `sendDailyQuest()`.

## C# Changes

- Added `PlayerNpcFactionsSnapshot.AbortQuest(int factionId)` with Java-shaped missing, inactive, and applied outcomes.
- Added `PlayerNpcFactionAbortResult` and `PlayerNpcFactionAbortStatus`.
- `QuestAbandonService.Abandon` now invokes NPC-faction abort for templates with `NpcFactionId != 0`.
- `QuestAbandonResult` now exposes the NPC-faction abort result for runtime validation and future packet/persistence wiring.

## Known Gaps

- Java `NpcFactions.sendDailyQuest()` packet selection/send is still not live in this abandon path.
- NPC-faction persistence after abandon remains deferred; current C# NPC-faction persistence support is planner-oriented and not wired into player save here.
- TASK/work-order recipe deletion, quest timer task-map cancellation, quest persistence, and Java `QuestEngine.onItemRemoved` remain gaps.

## Validation Decision

```text
Validation decision:
- Changed surface: production-code live NPC-faction state mutation from QuestAbandonService, snapshot mutation primitive, focused tests.
- Specific behavior/contract: active matching NPC-faction quest state is reset to Noting; inactive matching factions are not changed.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction" --no-restore -> 31/31 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live player NPC-faction state mutation applies.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered the changed abandon/NPC-faction runtime state behavior.
- Why this scope is sufficient: tests prove live abandon mutates Player.NpcFactions for active rows and preserves Java's missing/inactive guard behavior.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` | Service | Partial | Unit Tested | Partial Parity | Quest mutation, timer-clear packet, NPC-faction abort, work-item cleanup, ABANDON packet live; recipe, task cancellation, persistence gaps remain. |
| `NpcFactions.abortQuest` | `PlayerNpcFactionsSnapshot.AbortQuest` / `QuestAbandonService.AbortNpcFactionQuest` | Service/state | Partial | Unit Tested | Partial Parity | Active exact faction resets to `Noting`; Java `sendDailyQuest()` packet path and persistence dirty-state tracking are not live. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Abandon_NpcFactionQuestResetsActiveFactionStateLikeJava` | Unit | Java source review | Live abandon resets active matching faction state to `Noting` while preserving active/time/quest id | Java source + live C# state assertions | No `sendDailyQuest()` packet capture. |
| `Abandon_NpcFactionQuestLeavesInactiveFactionUnchangedLikeJava` | Unit | Java source review | Inactive matching faction is not mutated | Java source + live C# state assertions | No persistence verification. |

## Summary Metrics

- Focused validation: 31 tests passed.
- Live abandon path now mutates quest state, NPC-faction state, and quest work-item inventory state.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- NPC-faction row persistence remains incomplete after live state mutation.
- Daily quest packet selection/send after abort remains absent.
- Full real-client NPC-faction abandon flow remains unvalidated.

## Next Recommended UOW

UOW-2577: Wire TASK/work-order recipe deletion during live quest abandon.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest TASK branch -> player.getRecipeList().deleteRecipe(...).
- Java source method or runtime path: QuestService.abandonQuest, WorkOrdersData.getRecipeId, RecipeList.deleteRecipe.
- C# runtime artifact to wire or fix: static work-order recipe-id projection plus Player recipe-list live mutation invoked from QuestAbandonService or GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning a TASK/work-order quest removes the granted recipe from live player recipe state and marks persistence if existing shape supports it.
- Why this is not preview-only/test-only/documentation-only: it mutates live player recipe state from the live CM_DELETE_QUEST path.
```
