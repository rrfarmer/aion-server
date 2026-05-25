# Phase 6TF Completion - UOW-1014 Quest Finish Ordering Audit

## Scope

UOW-1014 documents Java quest-finish packet, callback, nearby-refresh, and persistence ordering. It is a read-only audit for the future operation-plan boundary.

No C# runtime code changed in this unit.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Quest-finish ordering audit | `QuestService.finishQuest`, `SM_QUEST_ACTION`, `QuestEngine.onQuestCompleted`, quest/faction DAOs | docs/read-only | Java Analysis | Selected | Low | Direct follow-up after staged quest and faction state helpers. |
| B | Staged quest-finish operation plan | same plus C# operation DTOs | future service/tests | Service Port | No | Medium | Needs ordering audit first. |
| C | `SM_QUEST_ACTION` update packet port | `SM_QUEST_ACTION` | future packet/tests | Packet Port | No | Medium | Depends on deciding extra-category suppression and send boundary. |
| D | Quest/faction persistence contracts | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | future repositories/tests | Repository Port | No | High | Requires operation-plan and rollback decisions. |

Selected batch: A, documentation-only.

## Java Breadcrumbs

- `QuestService.finishQuest` sends `SM_QUEST_ACTION(ActionType.UPDATE, qs)` after state mutation and before quest-completed callbacks.
- `QuestEngine.onQuestCompleted` runs before NPC faction completion and nearby refresh.
- `NpcFactions.completeQuest` runs after quest-completed callbacks.
- `PlayerController.updateNearbyQuests` runs after NPC faction completion.
- `finishQuest` does not call quest or NPC faction DAOs.
- `PlayerService.storePlayer` later stores quest state before NPC faction state.

## Deliverables

- Added `docs/QuestFinishOrdering-Audit.md`.
- Updated Phase 6 progress and nearby audit docs with the new ordering findings.
- Updated the next recommended unit to a staged quest-finish operation plan.

## Validation

| Command | Result |
|---|---|
| Manual source audit of Java files listed in `docs/QuestFinishOrdering-Audit.md` | Completed |

## Migration Parity Table - UOW-1014

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | Future C# quest-finish operation plan | Service / Ordering Boundary | Partial | Manual Only | Needs Verification | Source-audited ordering only. C# has staged quest and faction state helpers, but no composed operation plan, rewards, packets, callbacks, persistence, or nearby refresh. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` | Future C# quest action update packet | Packet | Not Started | Manual Only | Needs Verification | Audit captures update packet fields and extra-category suppression. No C# packet implementation for quest-finish update exists yet. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onQuestCompleted` | Future C# quest callback dispatcher | Service / Callback Dispatcher | Not Started | Manual Only | Needs Verification | Java callback runs after update packet and before NPC faction completion. C# has no quest handler runtime equivalent. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.store` | Future C# quest-state write repository | Repository | Not Started | Manual Only | Needs Verification | Audit captures deferred persistence and delete/insert/update commit phases. C# currently hydrates quest state only. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.storeNpcFactions` | Future C# NPC faction write repository | Repository | Not Started | Manual Only | Needs Verification | Audit captures deferred faction persistence after quest-state store in `PlayerService.storePlayer`. C# currently hydrates faction state only. |
| `com.aionemu.gameserver.services.player.PlayerService.storePlayer` | Future C# player-store ordering boundary | Service / Persistence Orchestration | Not Started | Manual Only | Needs Verification | Relevant Java ordering stores quests before NPC factions. C# equivalent persistence orchestration is not ported. |

## Tests Added

No tests were added in this read-only audit unit.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward calculation and inventory mutation ordering are still not ported.
- `SM_QUEST_ACTION` extra-category suppression can make sends conditional.
- Java quest-completed callbacks can perform additional mutations and sends.
- Persistence is deferred and not transactional with `finishQuest`.
- Java quest DAO delete/insert/update phases commit separately; C# must decide whether to match or intentionally improve this behavior.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Create a staged quest-finish operation plan that composes the existing quest-state mutation helper and NPC faction completion helper into ordered descriptors for packet update, callback dispatch, nearby refresh, and future persistence. Keep rewards, live sends, DAO writes, and ItemPurification dispatch disabled.

## Next Unit Handoff

Start with `docs/QuestFinishOrdering-Audit.md`, `docs/Phase-6TD-Completion.md`, `docs/Phase-6TE-Completion.md`, and `docs/PHASE-6-PROGRESS.md` Session 1014.

Recommended next slice:

1. Add a pure operation-plan DTO/service for quest finish.
2. Compose `QuestFinishStateMutationService.ApplyRewardCompletion`.
3. Compose `PlayerNpcFactionsSnapshot.CompleteActiveQuest` only when `NpcFactionId != 0`.
4. Emit descriptors for future `SM_QUEST_ACTION`, callback dispatch, and nearby refresh, but do not send them.
5. Add parity tests for ordering and no-op cases.
