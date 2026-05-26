# Phase 6WV Completion - UOW-1108 Quest Bonus Finish Caller Input Audit

Date: May 26, 2026

## Unit Of Work

UOW-1108: `[Phase 6][UOW-1108] Audit quest bonus finish caller inputs`

## Summary

UOW-1108 adds a read-only caller-input audit for the next quest bonus integration step.

The audit confirms that `StaticData.ItemTemplates`, `StaticData.QuestBonusItemGroups`, player race, and player quest state data are available somewhere in the runtime model, but not yet assembled at a live quest-finish call boundary. It also confirms that `GameServerConnection.HandleDialogSelectAsync` currently does not call `QuestFinishOperationPlanService.CreatePlan`, so the bonus adapter remains staged and disabled.

## Files Changed

- `docs/QuestBonusQuestFinishCallerInput-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WV-Completion.md`

## Validation

| Command | Result |
|---|---|
| `rg` / focused source reads over `GameServerConnection`, quest-finish planners, reward adapter, player quest state, and static-data access | Completed source audit. |
| `git diff --check` | Passed. |

No .NET tests were rerun for this docs-only audit. UOW-1107 passed the focused quest bonus bridge tests and the full solution with 2,190 tests.

## Migration Parity Table - UOW-1108

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Packet / Call Boundary | Partial | Manual Only | Needs Verification | Production packet boundary exists and has Java breadcrumbs. It does not currently invoke staged quest-finish planning or bonus planning. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService` | Service / Operation Planner | Partial | Manual Only | Needs Verification | Planner accepts quest state, template summary, optional reward projection, callback/persistence plans, and reward side-effect context. It does not assemble or call the bonus adapter. |
| `com.aionemu.gameserver.model.templates.quest.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardTemplateProjection` | Static Template / DTO | Partial | Manual Only | Needs Verification | Template summary metadata and reward projection models exist, but production reward-projection lookup for finish is not wired. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Quest State DTO | Partial | Manual Only | Needs Verification | Player carries `Quests`; planner accepts the current quest state. Future caller must explicitly locate the selected quest state and any handler quest states. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player`; `QuestFinishRewardSideEffectContext.Player` | Runtime Player Context | Partial | Manual Only | Needs Verification | Player race is available for adapter input if the same player snapshot is passed. Threading/order and mutation timing remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `Aion.GameServer.Dataholders.StaticData.ItemTemplates` | Static Data Dependency | Partial | Manual Only | Needs Verification | Runtime item templates are accessible through `runtimeContext.DataManager.StaticData`, but the quest-finish bonus path has no contract field for them yet. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_GROUPS_DATA` | `Aion.GameServer.Dataholders.StaticData.QuestBonusItemGroups` | Static Data Dependency | Partial | Manual Only | Needs Verification | Supported quest bonus groups are runtime available after UOW-1107, but no finish-side caller passes them to the adapter. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent` | `QuestBonusRewardPlanningInputAdapterService` plus future handler-state inputs | Dynamic Handler Dependency | Partial | Manual Only | Needs Verification | Adapter can accept explicit handler states and loaded ids. Production handler registry, reflection behavior, load state, and exception ordering are not ported. |
| `com.aionemu.gameserver.services.reward.BonusService.getQuestBonus` | `Aion.GameServer.Services.QuestBonusRewardPlanningInputAdapterService` | Reward Service Adapter | Partial | Manual Only | Needs Verification | Adapter is staged and test-covered from explicit inputs, but production finish invocation, RNG selection, selected item creation, and live mutation remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| _None_ | Read-only audit of caller-input availability. | Source-reviewed against Java/C# boundary code; no Java runtime comparison. |

## Remaining Risks

- Production `CM_DIALOG_SELECT` does not route quest-finish auto-reward actions into the staged operation planner.
- Reward projection availability at the production call site is unverified.
- `ItemTemplates` and `QuestBonusItemGroups` are runtime available but not part of the finish-side bonus-planning contract.
- Full handler quest-state lookup and loaded-handler state are missing.
- Dynamic handler dispatch, exception handling, Java RNG/Chance selection, selected reward materialization, live inventory mutation, packet ordering, persistence, rollback, threading/player-ordering, serialization, date/time ordering, and Java runtime comparison remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 0; 1 read-only caller-input audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production quest-finish planner invocation, production reward-projection lookup, finish-side static-data contract, full handler quest-state lookup, loaded-handler registry, dynamic handler dispatch, Java RNG/Chance selection, and live reward mutation/persistence
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a disabled quest-finish bonus input-assembly planner. It should accept the staged finish inputs plus optional runtime static data and handler-state sources, then return either a complete `QuestBonusRewardPlanningInput` or deterministic missing-input diagnostics.

Keep adapter invocation, descriptor insertion into live finish plans, RNG selection, selected item creation, and live rewards disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Disabled quest-finish bonus input-assembly planner | New service/tests | Medium | Recommended next unit; do not call from production socket path. |
| B | Extend finish side-effect context with static bonus inputs | `QuestFinishOperationPlanService` contract/tests | Medium/High | Only after planner shape is agreed; shared service surface. |
| C | Java handler exception/failure-ordering audit | Read-only Java analysis | Low | Needed before dynamic handler dispatch and loaded-handler parity. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add disabled input-assembly planner | New service/test files and docs | Production socket wiring, live rewards |
| Explorer A | Read-only Java handler exception ordering audit | Read-only Java/C# inspection or separate audit doc | Planner files while Orchestrator edits them |

## Do Not Parallelize

- `GameServerConnection.cs`: production packet boundary.
- `QuestFinishOperationPlanService.cs`: shared operation planner.
- `QuestBonusRewardPlanningInputAdapterService.cs`: existing adapter contract.
- `StaticData.cs`: recently changed static-data loader.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1098 through UOW-1108 are non-live quest bonus preparation units.
- Supported item groups are available as `StaticData.QuestBonusItemGroups`.
- The production socket path still does not invoke staged quest finish or bonus planning.
- Next safest code unit is a disabled input-assembly planner that reports missing inputs before any production wiring is attempted.
