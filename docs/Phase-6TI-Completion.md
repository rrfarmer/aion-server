# Phase 6TI Completion - UOW-1017 Quest Finish Reward Work-Item Audit

## Scope

UOW-1017 documents Java reward, challenge-task, and quest-work-item behavior inside `QuestService.finishQuest`.

No C# runtime code changed in this unit.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Reward/work-item audit | `QuestService.validateAndFixRewardGroup`, `getRewardItems`, `giveReward`, `removeQuestWorkItems` | docs/read-only | Java Analysis | Selected | Low | Needed before staging reward descriptors. |
| B | Quest callback dispatcher audit | `QuestEngine.onQuestCompleted`, handlers | docs/read-only | Java Analysis | Yes | Medium | Safe separately, but less immediate than reward/work-item ordering. |
| C | Persistence contract analysis | quest/faction DAOs | docs/read-only | Java Analysis | Yes | Medium | Safe separately; no code changes yet. |
| D | Reward descriptor implementation | future reward plan service/tests | Service Port | No | High | Requires this audit first. |

Selected batch: A, documentation-only.

## Java Breadcrumbs

- Reward group validation happens before reward item selection.
- Fixed and selectable item rewards are collected before `ItemService.addItem`.
- `giveReward` applies kinah, XP, title, AP, DP, GP, cube, and warehouse rewards.
- Challenge-task completion notification occurs before quest work-item removal.
- Quest work-item removal occurs before status changes to `COMPLETE`.
- Work-item removal deletes all owned matching item ids, not the XML count.

## Deliverables

- Added `docs/QuestFinishRewardWorkItem-Audit.md`.
- Updated Phase 6 progress, ordering audit, and nearby audit docs with reward/work-item findings.
- Updated the next recommended unit to staged reward/work-item descriptors.

## Validation

| Command | Result |
|---|---|
| Manual source audit of Java files listed in `docs/QuestFinishRewardWorkItem-Audit.md` | Completed |

## Migration Parity Table - UOW-1017

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.validateAndFixRewardGroup` | Future C# reward-group validation helper | Service / Reward Guard | Not Started | Manual Only | Needs Verification | Source-audited only. C# has `PlayerQuestState.RewardGroup`, but no finish-time correction based on reward group count. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | Future C# quest reward item planner | Service / Reward Planner | Not Started | Manual Only | Needs Verification | Source-audited fixed, selectable, class-specific, extended, and bonus item selection. C# lacks full reward template projection and handler bonus runtime. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | Future C# non-item quest reward planner | Service / Reward Mutation | Not Started | Manual Only | Needs Verification | Source-audited kinah, XP, title, AP, DP, GP, cube, and warehouse side effects. Several target systems are partial and not composed for quest finish. |
| `com.aionemu.gameserver.services.QuestService.removeQuestWorkItems` | Future C# quest work-item removal planner | Service / Inventory Mutation | Not Started | Manual Only | Needs Verification | Source-audited all-owned-count removal behavior and pre-completion status usage. C# has no quest-finish work-item removal plan. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | Future C# reward template projection | DTO / XML Template | Not Started | Manual Only | Needs Verification | Java XML model includes fixed/selectable items and non-item rewards. Full C# static-data projection for finish rewards is missing. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | Future C# quest reward/work item DTO | DTO / XML Template | Not Started | Manual Only | Needs Verification | Java default count is `1`; C# finish reward DTO is missing. Count behavior must be preserved. |
| `com.aionemu.gameserver.model.templates.quest.QuestWorkItems` | Future C# quest work-item projection | DTO / XML Template | Not Started | Manual Only | Needs Verification | Java exposes an empty list when XML is absent and returns a live JAXB list otherwise. C# projection is missing. |

## Tests Added

No tests were added in this read-only audit unit.

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward behavior depends on dialog action, extended reward index, repeat count, class-specific reward data, bonus handlers, and static quest templates.
- Non-item reward side effects fan out to inventory, common data, title, AP, DP, GP, cube, and warehouse systems.
- Work-item removal deletes all owned matching item ids.
- Java logging/warning behavior for reward correction is not modeled.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 0 in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Stage reward/work-item operation descriptors for quest finish without live mutation. Start with reward-group correction and descriptor-only work-item removal using simple template projection inputs; defer full XML static-data reward projection and live inventory mutation.

## Next Unit Handoff

Start with this file, `docs/QuestFinishRewardWorkItem-Audit.md`, `docs/Phase-6TG-Completion.md`, and `docs/PHASE-6-PROGRESS.md` Session 1017.

Recommended next slice:

1. Add a pure helper for reward-group correction based on reward-group count.
2. Add descriptor records for future work-item removal and non-item reward side effects.
3. Keep item grants, inventory removal, AP/XP/title/cube/warehouse mutation, packets, callbacks, DAO writes, and live nearby refresh disabled.
4. Add focused tests for reward-group correction edge cases from Java.
5. Update parity docs conservatively.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Reward-group correction helper | new service + tests | Medium | Sequential if touching operation plan. |
| B | Callback dispatcher audit | new docs-only audit | Low | Can run in parallel if docs file is separate. |
| C | Persistence contract analysis | new docs-only audit | Medium | Can run in parallel if docs file is separate. |

## Do Not Parallelize

- Quest finish operation plan service: shared sequencing boundary.
- Inventory/AP/common-data mutation services: side-effect-heavy systems.
- Phase progress/completion docs: Orchestrator-owned.
