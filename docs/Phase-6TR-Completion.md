# Phase 6TR Completion - UOW-1026 Quest-Finish Callback Plan Composition

## Scope

UOW-1026 composes the non-live completion callback dispatch plan into `QuestFinishOperationPlanService`.

This unit does not execute callback handlers, load dynamic quest scripts, send callback-triggered packets, mutate follow-up quests, enable live packet sends, or enable DAO writes.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Compose callback plan into quest finish | `QuestService.finishQuest`, `QuestEngine.onQuestCompleted`, `AbstractQuestHandler.onQuestCompletedEvent` | `QuestFinishOperationPlanService` + tests | Service Composition | Selected | Medium | Direct handoff from UOW-1025; shared operation-plan ordering surface requires exclusive Orchestrator ownership. |
| B | Follow-up quest packet/result planning | `AbstractQuestHandler.defaultOnQuestCompletedEvent`, `QuestService.addOrUpdateQuest`, `SM_QUEST_ACTION` | new service + tests/docs | Service Port | Yes later | Medium | Best after callback composition exists. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/static-data projections | Java Analysis | Yes later | Medium | Separate from operation-plan files. |
| D | Live callback execution | quest handler registry/runtime | runtime service + handler ports | Runtime Port | No | High | Requires handler registration, static data, packet side effects, and mutation surfaces first. |

Selected batch: A only. File ownership was `QuestFinishOperationPlanService`, its focused tests, and Orchestrator-owned docs.

## Java Breadcrumbs

- `QuestService.finishQuest` sends the completed quest update packet before `QuestEngine.onQuestCompleted`.
- `QuestEngine.onQuestCompleted` invokes registered handlers before NPC-faction completion and nearby refresh.
- The C# operation plan now preserves that position for detailed callback descriptors.
- Supplied no-handler callback plans omit the legacy placeholder to avoid implying live handler execution.
- When no callback plan is supplied, the legacy generic non-live callback descriptor remains for compatibility with existing staged ordering tests.

## Deliverables

- Extended `QuestFinishOperationDescriptor` with optional `CompletionCallbackOperation`.
- Extended `QuestFinishOperationPlanService.CreatePlan` with optional `QuestCompletionCallbackPlan`.
- Preserved old generic callback placeholder behavior when no callback plan is supplied.
- Added detailed callback descriptors after quest update packet and before NPC-faction completion when a callback plan is supplied.
- Added focused tests for detailed callback composition and empty-plan behavior.
- Updated callback audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestFinishOperationPlanServiceTests --nologo` | Passed: 11 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1772 |

## Migration Parity Table - UOW-1026

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService.CreatePlan` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | C# composes optional callback dispatch descriptors after the quest update packet and before NPC-faction completion, preserving Java ordering. It remains non-live and does not send packets, run callbacks, mutate follow-up quests, or execute persistence. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onQuestCompleted` | `Aion.GameServer.Services.QuestCompletionCallbackPlanService`; `QuestFinishOperationDescriptor.CompletionCallbackOperation` | Service / Callback Dispatch Composition | Partial | Unit Tested | Partial Parity | Detailed callback descriptors can now be carried by quest-finish plans. Handler registry order remains caller-provided; no Java runtime comparison or live execution exists. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.onQuestCompletedEvent` | `QuestFinishOperationDescriptor.CompletionCallbackOperation` | Handler API / Descriptor Composition | Partial | Unit Tested | Needs Verification | C# records planned handler invocation metadata inside quest-finish ordering but does not execute handler code or model arbitrary side effects. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent` | `QuestCompletionCallbackDescriptor.UsesDefaultFollowUp`; `QuestFinishOperationDescriptor.CompletionCallbackOperation` | Handler Helper / Follow-Up Metadata Composition | Partial | Unit Tested | Needs Verification | C# carries default follow-up metadata through quest-finish planning. It does not run start-condition checks, mutate follow-up quests, or emit `SM_QUEST_ACTION ADD/UPDATE`. |
| `com.aionemu.gameserver.questEngine.model.QuestEnv` | `QuestCompletionCallbackDescriptor.CompletedQuestId`; `UsesSharedQuestEnv`; `QuestFinishOperationDescriptor.CompletionCallbackOperation` | DTO / Callback Context Projection | Partial | Unit Tested | Needs Verification | C# carries shared-env intent through quest-finish planning, but does not model mutable env identity, player/target references, handler mutation, threading, or Java object identity. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_ComposesDetailedCallbackPlanAfterUpdatePacketBeforeNpcFaction` | Detailed callback descriptors are placed after `QuestUpdatePacket` and before `NpcFactionCompletion`; default follow-up metadata survives composition. | Source-reviewed `QuestService.finishQuest` and `QuestEngine.onQuestCompleted`. |
| `CreatePlan_UsesProvidedEmptyCallbackPlanWithoutLegacyPlaceholder` | Supplied no-handler callback plan does not emit the legacy callback placeholder. | Source-reviewed empty callback loop behavior; no runtime capture. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Detailed callback descriptors are still non-live; no handlers are executed.
- Handler registration order is caller-provided and not derived from Java reflection/script loading.
- Mutable `QuestEnv` identity, player/target references, and handler-side env mutation remain unmodeled.
- Callback-triggered follow-up quest `ADD`/`UPDATE` packets remain unplanned.
- Live packet sends, nearby refresh, callbacks, and DAO writes remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial operation-plan composition update
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, handler loader order, live handler execution, mutable `QuestEnv`, follow-up quest mutation/packets, and live quest-finish execution
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add a non-live follow-up quest callback result plan for `AbstractQuestHandler.defaultOnQuestCompletedEvent`: model possible follow-up `LOCKED`/`START` quest updates and callback-triggered `SM_QUEST_ACTION ADD/UPDATE` descriptors without executing handler logic.

## Next Unit Handoff

Start with this file, `docs/QuestCompletionCallback-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1026.

Recommended next slice:

1. Inspect `AbstractQuestHandler.defaultOnQuestCompletedEvent` and `QuestService.addOrUpdateQuest`.
2. Add a pure follow-up quest callback result planner and DTOs.
3. Represent possible follow-up quest status as `LOCKED` or `START` with non-live packet descriptors.
4. Keep start-condition evaluation caller-provided or explicitly marked unsupported.
5. Add focused tests and keep live callback execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Follow-up quest callback result planner | new service + tests | Medium | Best next implementation slice. |
| B | Reward XML projection planning | docs/static-data audit | Medium | Safe if it avoids operation-plan files. |
| C | Persistence failure-ordering policy audit | docs-only | Medium | Safe read-only analysis before live writes. |

## Do Not Parallelize

- Live callback execution.
- Quest-finish operation plan edits from multiple workers.
- Live DAO write implementation.
- Phase progress/completion docs.
