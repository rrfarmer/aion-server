# Phase 6TQ Completion - UOW-1025 Quest Completion Callback Dispatch Plan

## Scope

UOW-1025 adds a pure, non-live completion callback dispatch planner for Java `QuestEngine.onQuestCompleted`.

This unit does not execute handlers, load scripts through reflection, send callback-triggered packets, mutate quest state, or integrate callbacks into live quest finish.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Completion callback dispatch plan | `QuestEngine.onQuestCompleted`, `registerOnQuestCompleted`, `AbstractQuestHandler.onQuestCompletedEvent`, `QuestEnv` | new service + tests | Service Port | Selected | Medium | Direct handoff from UOW-1024; isolated files and no live handler execution. |
| B | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/static-data projections | Java Analysis | Yes later | Medium | Separate from callback files. |
| C | Persistence failure-ordering policy audit | DAO failure paths | docs-only | Java Analysis | Yes later | Medium | Useful before live writes; does not need callback code. |
| D | Live callback execution | quest handler registry/runtime | runtime service + handler ports | Runtime Port | No | High | Requires handler registration, static data, packet side effects, and mutation surfaces first. |

Selected batch: A only. File ownership was a new callback plan service/test files plus Orchestrator-owned docs.

## Java Breadcrumbs

- `QuestEngine.onQuestCompleted` builds one `QuestEnv(null, player, questId)`.
- It iterates `questOnCompleted`, an `ArrayList` populated by `registerOnQuestCompleted`.
- `registerOnQuestCompleted` de-dupes quest ids and preserves first registration order.
- Each registered id is looked up in `questHandlers`; missing handlers are skipped.
- `onQuestCompletedEvent(env)` receives the same mutable env instance for every invoked handler.
- A single `try/catch` wraps the whole loop, so a throwing handler stops remaining callbacks after logging.

## Deliverables

- Added `Aion.GameServer.Services.QuestCompletionCallbackPlanService`.
- Added registration, descriptor, action, and status DTOs for non-live callback dispatch planning.
- Added duplicate-registration normalization, missing-handler descriptors, shared-env metadata, default-follow-up metadata, and exception-stop modeling.
- Added focused tests for Java order/de-dupe behavior, missing handlers, exception stop, default follow-up metadata, and empty registrations.
- Updated callback audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestCompletionCallbackPlanServiceTests --nologo` | Passed: 5 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1770 |

## Migration Parity Table - UOW-1025

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.onQuestCompleted` | `Aion.GameServer.Services.QuestCompletionCallbackPlanService.CreatePlan` | Service / Callback Dispatch Plan | Partial | Unit Tested | Partial Parity | C# models registration order, missing handler skips, shared completed quest id/env metadata, and exception-stop behavior as non-live descriptors. It does not execute handlers, mutate quest state, send packets, or compare against Java runtime. |
| `com.aionemu.gameserver.questEngine.QuestEngine.registerOnQuestCompleted` | `QuestCompletionCallbackPlanService` duplicate normalization | Service / Handler Registry Projection | Partial | Unit Tested | Partial Parity | C# de-dupes duplicate registered quest ids preserving first order. Java order still depends on reflection/script loading and remains caller-provided. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.onQuestCompletedEvent` | `QuestCompletionCallbackDescriptor` | Handler API / Descriptor | Partial | Unit Tested | Needs Verification | C# records planned handler invocation but does not execute handler code or model arbitrary handler side effects beyond metadata. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent` | `QuestCompletionCallbackDescriptor.UsesDefaultFollowUp`; `FollowUpQuestId` | Handler Helper / Follow-Up Metadata | Partial | Unit Tested | Needs Verification | C# records possible default follow-up behavior but does not run `QuestService.checkStartConditions` or emit `SM_QUEST_ACTION ADD/UPDATE`. |
| `com.aionemu.gameserver.questEngine.model.QuestEnv` | `QuestCompletionCallbackDescriptor.CompletedQuestId`; `UsesSharedQuestEnv` | DTO / Callback Context Projection | Partial | Unit Tested | Needs Verification | C# records shared-env intent and completed quest id only. It does not model mutable env object identity, player reference, target object, or handler-side env mutation. |
| `com.aionemu.gameserver.questEngine.handlers.QuestHandlerLoader` | Future C# handler registration source | Reflection / Script Loader | Not Started | Manual Only | Needs Verification | Java reflection loading and script order remain unported; callback registrations are caller-provided in C#. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_DeduplicatesRegistrationsAndPreservesFirstJavaOrder` | Duplicate registration ids are ignored after first occurrence and descriptors share the completed quest id/env marker. | Source-reviewed `registerOnQuestCompleted` and `onQuestCompleted`. |
| `CreatePlan_RecordsMissingHandlerLookupWithoutStoppingLoop` | Missing handler lookups are recorded and do not stop later registrations. | Source-reviewed `getQuestHandlerByQuestId` null guard. |
| `CreatePlan_StopsAfterThrowingHandlerLikeJavaWholeLoopCatch` | A throwing handler stops descriptor generation for later callbacks. | Source-reviewed whole-loop `try/catch`. |
| `CreatePlan_CarriesDefaultFollowUpMetadataWithoutExecutingHandler` | Default follow-up callback metadata is retained without executing `defaultOnQuestCompletedEvent`. | Source-reviewed default helper. |
| `CreatePlan_ReturnsNoHandlersForEmptyRegistrationList` | Empty registration input emits a no-handler plan. | Source-reviewed empty loop behavior. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Handler registration order is still caller-provided; Java reflection/script loading order is not modeled.
- Callback handlers can mutate quests, inspect inventory/static data, and send packets; C# does not execute them.
- Mutable `QuestEnv` identity and handler-side env mutation are not modeled beyond metadata.
- Follow-up quest `ADD`/`UPDATE` packet planning remains missing.
- Quest-finish operation planning still uses a generic callback descriptor rather than detailed callback dispatch descriptors.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 partial non-live callback dispatch plan plus descriptor/status DTOs
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, handler loader order, live handler execution, mutable `QuestEnv`, follow-up quest mutation/packets, and quest-finish callback composition
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Compose the non-live callback dispatch plan into `QuestFinishOperationPlanService` so the `QuestCompletedCallback` descriptor can optionally carry detailed callback dispatch descriptors without executing handlers.

## Next Unit Handoff

Start with this file, `docs/QuestCompletionCallback-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1025.

Recommended next slice:

1. Extend `QuestFinishOperationDescriptor` with an optional callback dispatch payload.
2. Extend `QuestFinishOperationPlanService.CreatePlan` with an optional `QuestCompletionCallbackPlan`.
3. Preserve the existing generic callback placeholder when no callback plan is supplied.
4. Add tests proving detailed callback descriptors remain after quest update packet and before NPC-faction completion.
5. Keep live callback execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose callback plan into quest finish | `QuestFinishOperationPlanService` + tests | Medium | Best next implementation slice. |
| B | Follow-up quest packet/result planning | new service + tests/docs | Medium | Best after callback composition. |
| C | Reward XML projection planning | docs/static-data audit | Medium | Safe if it avoids operation-plan files. |

## Do Not Parallelize

- Live callback execution.
- Quest-finish operation plan edits from multiple workers.
- Live DAO write implementation.
- Phase progress/completion docs.
