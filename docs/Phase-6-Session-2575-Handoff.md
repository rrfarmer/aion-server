# Phase 6 Session 2575 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2575: Remove quest work items during live quest abandon. See
[Phase-6-Session-2575-Completion.md](Phase-6-Session-2575-Completion.md).

## Commits Made

- `1a63d60f4` - `[Phase 6][UOW-2574] Wire quest abandon live`
- Current commit - `[Phase 6][UOW-2575] Remove abandon quest work items`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 completed the next Java abandon side effect: `removeQuestWorkItems` now removes matching cube stacks,
  tracks deleted item rows, and sends item-delete/cube-size packets before ABANDON.

## Files Changed In UOW-2575

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDeleteItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `docs/Phase-6-Session-2575-Completion.md`
- `docs/Phase-6-Session-2575-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#removeQuestWorkItems`
- `com.aionemu.gameserver.model.items.storage.Storage#decreaseByItemId`
- `com.aionemu.gameserver.model.items.storage.Storage#decreaseItemCount`
- `com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType`
- `com.aionemu.gameserver.model.templates.quest.QuestWorkItems`
- `com.aionemu.gameserver.model.templates.quest.QuestItems`

## C# Artifacts Touched

- `QuestAbandonService`
- `QuestWorkItemDeletion`
- `NearbyQuestTemplateSummary.QuestWorkItems`
- `NearbyQuestTemplateXmlExtractor`
- `GameServerConnection.HandleDeleteQuestAsync`
- `SmDeleteItem`

## Validation

```text
dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests" --no-restore
```

Result: passed, 14/14. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live inventory mutation was added, but the
focused filter built the affected project and directly covered the new mutation/static-data behavior. Shared inventory
persistence primitives were reused, not changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Manual Only | Partial Parity | Live dispatch wired; timer-clear, work-item delete, abandon packet ordering implemented; task-map cancellation still missing. |
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` | Service | Partial | Unit Tested | Partial Parity | Guards, quest delete/reset, work-item cleanup, ABANDON/TIMER covered; NPC factions, recipes, quest persistence still missing. |
| `QuestService.removeQuestWorkItems` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | Removes all matching cube stacks and tracks deletes; Java quest-engine item-removed callback not live. |
| `Storage.decreaseByItemId(..., QuestStatus)` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | Full-count delete behavior and quest delete types covered; generic partial stack update not ported here. |
| `QuestWorkItems` / `QuestItems` XML | `NearbyQuestTemplateSummary.QuestWorkItems` | DTO/static data | Complete | Unit Tested | Verified Parity | XML projection covered. |
| `ItemPacketService.ItemDeleteType` quest values | `SmDeleteItem.QuestStartDeleteType` / `QuestCompleteDeleteType` | Packet constants | Complete | Unit Tested | Verified Parity | START=0x34 and COMPLETE=0x31 covered. |

## Known Gaps

- NPC-faction abort for abandon remains missing.
- TASK/work-order recipe deletion remains missing.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Quest persistence after abandon remains missing.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Abandon changes quest and inventory state in memory, but quest persistence is still not wired.
- Work-item delete socket ordering is service-tested by recorded cube counts but not directly packet-captured from `GameServerConnection`.

## Next Recommended UOW

**UOW-2576: Wire NPC-faction abort during quest abandon.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getNpcFactions().abortQuest(template).
- Java source method or runtime path: QuestService.abandonQuest and NpcFactions.abortQuest.
- C# runtime artifact to wire or fix: PlayerNpcFactionsSnapshot/runtime mutation service invoked from QuestAbandonService or GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning an NPC-faction quest clears or deactivates the player's matching faction quest state, with persistence status documented or wired if existing shape supports it.
- Why this is not preview-only/test-only/documentation-only: it mutates live player NPC-faction state from the live CM_DELETE_QUEST path.
```

Java artifacts to inspect:

- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#abortQuest`
- `QuestService.abandonQuest` NPC-faction branch
- NPC-faction DAO/persistence methods, if C# already has matching repository shape

C# artifacts likely involved:

- `Player.NpcFactions`
- `PlayerNpcFactionsSnapshot`
- `NpcFactionLevelUpPlanService` (for existing mutation patterns)
- `QuestAbandonService`
- `PlayerEnterWorldRepository` NPC-faction persistence/load shape

Focused validation recipe:

- Behavior/contract: abandoning a quest with `NpcFactionId != 0` aborts the matching active faction state like Java.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added; inspect `NpcFactions.abortQuest` first.
- Broad-validation trigger: live player NPC-faction state mutation applies; start focused and broaden only if focused evidence exposes wider risk.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, work-item inventory cleanup, and packet output.
- `QuestAbandonService.Abandon` owns the Java branch behavior and now returns ordered timer packets, work-item deletions, and an abandon packet.
- `QuestWorkItemDeletion.CubeItemCountAfterDeletion` exists to preserve Java per-stack cube-size packet order.
- `NearbyQuestTemplateSummary` has `CannotGiveup`, `IsTimer`, and `QuestWorkItems`.
- Avoid preview/metadata/evidence-only work; the next UOW should mutate the next real abandon-path state.
