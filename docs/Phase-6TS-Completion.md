# Phase 6TS Completion - UOW-1027 Quest Completion Follow-Up Result Plan

## Scope

UOW-1027 adds a pure, non-live follow-up quest callback result planner for Java `AbstractQuestHandler.defaultOnQuestCompletedEvent` and `QuestService.addOrUpdateQuest`.

This unit does not execute handler logic, evaluate Java quest start conditions, mutate quest state, send packets, compose follow-up results into callback dispatch plans, or enable live callback execution.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Follow-up quest callback result planner | `AbstractQuestHandler.defaultOnQuestCompletedEvent`, `QuestService.addOrUpdateQuest`, `SM_QUEST_ACTION` | new service + tests | Service Port | Selected | Medium | Direct handoff from UOW-1026; isolated files and no live handler execution. |
| B | Compose follow-up result plan into callback descriptors | `QuestEngine.onQuestCompleted`, `AbstractQuestHandler.defaultOnQuestCompletedEvent` | callback/operation plan + tests | Service Composition | Yes later | Medium | Best after planner exists. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/static-data projections | Java Analysis | Yes later | Medium | Separate from callback files. |
| D | Live callback execution | quest handler registry/runtime | runtime service + handler ports | Runtime Port | No | High | Requires handler registry, start-condition evaluation, mutation, and packet side-effect homes first. |

Selected batch: A only. File ownership was new follow-up result service/test files plus Orchestrator-owned docs.

## Java Breadcrumbs

- `AbstractQuestHandler.defaultOnQuestCompletedEvent` can call `QuestService.addOrUpdateQuest` with `QuestStatus.LOCKED` or `QuestStatus.START`.
- It can return without side effects when the target quest is already non-`LOCKED`, start conditions fail, requirements are missing, or level gates block start/lock.
- `QuestService.addOrUpdateQuest` sends `SM_QUEST_ACTION(ActionType.ADD, qs)` for missing quest states.
- Existing states with the target status return without packet emission.
- Existing `COMPLETE` states use `ActionType.ADD`; other changed existing states use `ActionType.UPDATE`.
- C# start-condition and requirement decisions are caller-provided because Java static-data and handler execution are not live.

## Deliverables

- Added `Aion.GameServer.Services.QuestCompletionFollowUpPlanService`.
- Added request, descriptor, packet-action, decision, and status DTOs for non-live follow-up planning.
- Added focused tests for missing quest `LOCKED`/`START`, existing non-complete update, existing complete add, and no-op behavior.
- Updated callback audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestCompletionFollowUpPlanServiceTests --nologo` | Passed: 5 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1777 |

## Migration Parity Table - UOW-1027

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent` | `Aion.GameServer.Services.QuestCompletionFollowUpPlanService.CreatePlan` | Handler Helper / Follow-Up Result Plan | Partial | Unit Tested | Partial Parity | C# models caller-supplied no-op, `LOCKED`, and `START` follow-up outcomes as non-live descriptors. It does not execute Java start-condition checks, XML condition recursion, level gates, or handler logic. |
| `com.aionemu.gameserver.services.QuestService.addOrUpdateQuest` | `QuestCompletionFollowUpPlanService.CreatePlan` | Service / Quest Mutation Packet Plan | Partial | Unit Tested | Partial Parity | C# models `ADD` for missing or existing `COMPLETE`, `UPDATE` for changed existing non-complete, and no-op for same status. It does not mutate `QuestStateList`, reset quest vars for `COMPLETE`, send packets, or persist state. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` | `QuestCompletionFollowUpDescriptor.PacketAction` | Packet / Action Descriptor | Partial | Unit Tested | Needs Verification | C# records callback-triggered `ADD`/`UPDATE` packet action intent only. It does not serialize or send callback follow-up packets. |
| `com.aionemu.gameserver.questEngine.model.QuestStatus` | `QuestCompletionFollowUpDecision`; `QuestCompletionFollowUpDescriptor.TargetQuestStatus` | Enum / Status Projection | Partial | Unit Tested | Needs Verification | C# projects only `LOCKED` and `START` statuses needed by default completion callbacks. Other Java statuses and enum serialization remain outside this unit. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState`; `QuestCompletionFollowUpDescriptor.ExistingQuestState` | Model / Quest State Projection | Partial | Unit Tested | Needs Verification | C# reads existing status to choose Java packet action but does not construct Java-style `QuestState`, run constructor timestamps, mutate state, or model persistent-state side effects. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_AddsLockedFollowUpQuestWhenJavaWouldAddMissingCampaign` | Missing follow-up quest with `LOCKED` outcome emits non-live `ADD`. | Source-reviewed `defaultOnQuestCompletedEvent` and `addOrUpdateQuest`. |
| `CreatePlan_StartsMissingFollowUpQuestWithAddPacketLikeJavaAddOrUpdateQuest` | Missing follow-up quest with `START` outcome emits non-live `ADD`. | Source-reviewed `addOrUpdateQuest`. |
| `CreatePlan_UpdatesExistingNonCompleteFollowUpQuest` | Existing non-complete quest changing status emits non-live `UPDATE`. | Source-reviewed `addOrUpdateQuest`. |
| `CreatePlan_AddsExistingCompleteFollowUpQuestLikeJavaCompleteStatusBranch` | Existing `COMPLETE` quest changing status emits non-live `ADD`. | Source-reviewed `addOrUpdateQuest`. |
| `CreatePlan_ReturnsNoActionForSameStatusOrNoActionDecision` | Same target status and no-action decisions emit no descriptors. | Source-reviewed `addOrUpdateQuest` early return and default helper no-op branches. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Start-condition evaluation, XML condition recursion, pre-quest state scanning, and level gates are caller-provided/not executed.
- Follow-up descriptors are not yet composed into callback dispatch descriptors.
- No live `QuestStateList` mutation, packet send, persistent-state transition, or DAO write exists.
- Only `LOCKED` and `START` default callback statuses are projected.
- Callback handler order and live handler execution remain blocked.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial non-live follow-up result planner plus descriptor/status DTOs
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, start-condition execution, XML condition recursion, live quest-state mutation, callback result composition, and packet send/serialization
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Compose `QuestCompletionFollowUpPlan` into `QuestCompletionCallbackDescriptor` or an adjacent callback-result descriptor so callback dispatch plans can carry possible follow-up quest `ADD`/`UPDATE` results without executing handlers.

## Next Unit Handoff

Start with this file, `docs/QuestCompletionCallback-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1027.

Recommended next slice:

1. Extend callback dispatch descriptor shape with optional follow-up result plan data.
2. Preserve existing callback dispatch tests and no-live execution semantics.
3. Add tests proving default follow-up metadata can carry detailed `ADD`/`UPDATE` result descriptors.
4. Keep Java start-condition evaluation caller-provided and documented as not executed.
5. Keep live callback execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose follow-up result plan into callback descriptors | callback plan service + tests | Medium | Best next implementation slice. |
| B | Reward XML projection planning | docs/static-data audit | Medium | Safe if it avoids callback/operation-plan files. |
| C | Persistence failure-ordering policy audit | docs-only | Medium | Safe read-only analysis before live writes. |

## Do Not Parallelize

- Live callback execution.
- Callback/quest-finish operation plan edits from multiple workers.
- Live DAO write implementation.
- Phase progress/completion docs.
