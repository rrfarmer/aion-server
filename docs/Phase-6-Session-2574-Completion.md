# Phase 6 Session 2574 Completion

## UOW

[Phase 6] UOW-2574: Wire CM_DELETE_QUEST live quest-abandon mutation

## Status

Completed and validated with a focused test filter. `CM_DELETE_QUEST` is no longer deferred: the client packet now
resolves the live quest template, executes Java-shaped abandon guards, mutates `Player.Quests`, sends
`SM_QUEST_ACTION.ABANDON`, sends timer-clear `SM_QUEST_ACTION.TIMER` when the template is timer-based, and attempts
the live nearby-quest refresh when runtime world/template context is available.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: CM_DELETE_QUEST client packet dispatch and quest-abandon execution.
- Java source method or runtime path: CM_DELETE_QUEST.runImpl and QuestService.abandonQuest.
- C# runtime artifact to wire or fix: GameServerConnection dispatch, QuestAbandonService, SmQuestAction, NearbyQuestTemplateSummary/XML extraction.
- Client-visible/state/persistence effect expected: active player quest state is deleted or reset to COMPLETE; ABANDON/TIMER packets are sent; nearby quest refresh is sent when runtime context is present.
- Why this is not preview-only/test-only/documentation-only: the packet case now calls live code that mutates Player.Quests and sends real server packets.
```

## Java Source Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DELETE_QUEST.runImpl`:
  - read `questId`;
  - if template exists and `isTimer()`, cancel `TaskId.QUEST_TIMER` and send `SM_QUEST_ACTION(questId, 0)`;
  - call `QuestService.abandonQuest(player, questId)`.
- `com.aionemu.gameserver.services.QuestService.abandonQuest`:
  - missing template -> `false`;
  - `template.isCannotGiveup()` -> `false`;
  - missing state, COMPLETE, or LOCKED -> `false`;
  - `completeCount > 0` -> set status COMPLETE without incrementing count, set quest var 0, set flags 0;
  - otherwise delete quest from player quest list;
  - abort NPC faction, remove quest work items, delete work-order recipe when applicable;
  - if `TaskId.QUEST_TIMER` exists, `questTimerEnd`;
  - send `SM_QUEST_ACTION(ActionType.ABANDON, qs)`;
  - `player.getController().updateNearbyQuests()`.
- `SM_QUEST_ACTION.writeImpl`: ABANDON writes `D(0)` after action id and quest id; TIMER writes `D(timer)` and `C(timer > 0 ? 1 : 0)`.
- `QuestTemplate.cannotGiveup` and `QuestTemplate.timer` JAXB attributes default to false.

## C# Changes

- `QuestAbandonService` added Java-shaped abandon guards and live `Player.Quests` mutation.
- `GameServerConnection` dispatches `CmDeleteQuest` to `HandleDeleteQuestAsync`.
- `SmQuestAction` now supports ABANDON and TIMER factories/wire branches.
- `NearbyQuestTemplateSummary` and `NearbyQuestTemplateXmlExtractor` now carry `CannotGiveup` and `IsTimer`.
- Focused tests added for abandon branches and updated extractor real-data counts.

## Known Gaps

- C# does not yet cancel a live `TaskId.QUEST_TIMER` task map entry; it sends the timer-clear packet when the template is timer-based.
- `QuestService.removeQuestWorkItems` is not live yet, so quest work-item inventory removal remains missing.
- NPC-faction abort and TASK/work-order recipe deletion remain missing.
- Quest persistence for the mutated in-memory quest list is still not live in this handler.
- `HandleDeleteQuestAsync` glue is covered by focused compile and source review; branch behavior is service-tested.

## Validation Decision

```text
Validation decision:
- Changed surface: production-code, live packet dispatch, live player quest-state mutation, packet serialization, runtime XML/static-data extraction.
- Specific behavior/contract: CM_DELETE_QUEST/QuestService.abandonQuest guards, delete/reset mutation, ABANDON/TIMER packet payloads, cannot_giveup/timer template extraction.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests" --no-restore -> 12/12 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live dispatch/state mutation applies.
- Broad .NET decision: skipped after focused pass; the filtered test built Aion.GameServer and directly covered the new mutation/packet/extractor behavior. No shared packet primitive or persistence code changed.
- Why this scope is sufficient: the service tests cover every Java abandon guard plus both mutation branches and packet bytes; extractor tests cover Java attribute defaults and real-data counts.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Manual Only | Partial Parity | Live dispatch is wired; task-map cancellation is not live; abandon branch behavior is delegated to tested service. |
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` | Service | Partial | Unit Tested | Partial Parity | Guards, delete/reset mutation, ABANDON packet, timer-clear packet are covered; work items, NPC factions, recipes, persistence remain missing. |
| `SM_QUEST_ACTION.writeImpl` ABANDON/TIMER | `SmQuestAction.Abandon` / `SmQuestAction.Timer` | Packet | Complete | Unit Tested | Verified Parity | Payload bytes covered in `QuestAbandonServiceTests`. |
| `QuestTemplate.cannotGiveup` / `timer` | `NearbyQuestTemplateSummary.CannotGiveup` / `IsTimer` | DTO/static data | Complete | Unit Tested | Verified Parity | XML extractor tests cover explicit/default values and real-data counts. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestAbandonServiceTests` | Unit | Java source review | Abandon guards, delete/reset mutation, ABANDON/TIMER packet bytes | Java source + packet byte assertions | Handler glue not directly unit-tested. |
| `NearbyQuestTemplateXmlExtractorTests` updates | Unit/real-data audit | `QuestTemplate` JAXB attributes and real XML | `cannot_giveup`/`timer` extraction/defaults/counts | Java source + real data counts | No Java fixture run. |

## Summary Metrics

- New automated coverage: 7 `QuestAbandonServiceTests` plus 5 updated extractor tests in the focused filter.
- One deferred client packet (`CM_DELETE_QUEST`) is now live for core quest-state mutation and packet output.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Abandon side effects that touch inventory, NPC factions, work-order recipes, persistence, and timer task storage are still incomplete.
- Nearby quest refresh is best-effort based on available runtime context.
- Full real-client abandon flow remains unvalidated.

## Next Recommended UOW

UOW-2575: Port live `QuestService.removeQuestWorkItems` for quest abandon. This should load quest work-item template
rows into `NearbyQuestTemplateSummary`, remove all matching item counts from the live player inventory during abandon,
and emit the existing inventory update/delete packets using current C# inventory helpers where possible.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: Java's quest-abandon work-item inventory cleanup.
- Java source method or runtime path: QuestService.abandonQuest -> removeQuestWorkItems.
- C# runtime artifact to wire or fix: NearbyQuestTemplateXmlExtractor quest_work_items projection, QuestAbandonService/GameServerConnection inventory mutation/fanout.
- Client-visible/state/persistence effect expected: abandoned quests remove their quest work items from player inventory and send inventory item update/delete packets.
- Why this is not preview-only/test-only/documentation-only: it mutates live inventory state from the live CM_DELETE_QUEST path.
```

Focused validation recipe:

- Behavior/contract: abandon removes all inventory count for each `quest_work_item` item id and leaves unrelated items untouched.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of `removeQuestWorkItems` should be documented.
- Broad-validation trigger: live inventory mutation applies; start focused and broaden only if focused evidence exposes wider risk.
