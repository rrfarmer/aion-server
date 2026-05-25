# Phase 6TT Completion - UOW-1028 Callback Follow-Up Result Composition

## Scope

UOW-1028 composes `QuestCompletionFollowUpPlan` into `QuestCompletionCallbackDescriptor` so non-live callback dispatch plans can carry possible follow-up quest `ADD`/`UPDATE` results.

This unit does not execute callback handlers, evaluate Java start conditions, mutate quest state, send packets, compose follow-up results into quest-finish descriptors beyond the existing callback payload, or enable live callback execution.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Compose follow-up result plan into callback descriptors | `QuestEngine.onQuestCompleted`, `AbstractQuestHandler.defaultOnQuestCompletedEvent`, `QuestService.addOrUpdateQuest` | callback plan service + tests | Service Composition | Selected | Medium | Direct handoff from UOW-1027; shared callback descriptor surface requires exclusive ownership. |
| B | Compose callback follow-up payload through quest-finish operation descriptors | `QuestService.finishQuest`, `QuestEngine.onQuestCompleted` | operation plan tests/docs | Service Composition | Yes later | Medium | Existing operation descriptor carries callback descriptors; needs tests/audit after this unit. |
| C | Reward XML projection planning | `QuestTemplate`, `Rewards`, `QuestWorkItems` | docs/static-data projections | Java Analysis | Yes later | Medium | Separate from callback files. |
| D | Live callback execution | quest handler registry/runtime | runtime service + handler ports | Runtime Port | No | High | Requires handler registry, start-condition evaluation, mutation, and packet side-effect homes first. |

Selected batch: A only. File ownership was `QuestCompletionCallbackPlanService`, its focused tests, and Orchestrator-owned docs.

## Java Breadcrumbs

- Java callback dispatch invokes each handler in registration order, and default completion callbacks may call `QuestService.addOrUpdateQuest`.
- UOW-1028 keeps this non-live by carrying an optional follow-up result plan on each callback descriptor.
- Duplicate registered quest ids still preserve the first registration and discard later duplicate payloads, matching Java registration behavior.
- A throwing handler still stops remaining callback descriptors after its descriptor is emitted, matching Java's whole-loop catch behavior.

## Deliverables

- Extended `QuestCompletionCallbackRegistration` with optional `QuestCompletionFollowUpPlan`.
- Extended `QuestCompletionCallbackDescriptor` with optional `QuestCompletionFollowUpPlan`.
- Preserved existing callback dispatch behavior for descriptors without follow-up results.
- Added focused tests for detailed follow-up payload carry-through, duplicate registration behavior, and exception-stop behavior with follow-up payloads.
- Updated callback audit, quest-finish ordering, nearby audit, and Phase 6 progress docs.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter QuestCompletionCallbackPlanServiceTests --nologo` | Passed: 8 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1780 |

## Migration Parity Table - UOW-1028

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.onQuestCompleted` | `Aion.GameServer.Services.QuestCompletionCallbackPlanService.CreatePlan` | Service / Callback Dispatch Plan | Partial | Unit Tested | Partial Parity | C# callback descriptors can now carry optional follow-up result plans while preserving Java registration order, duplicate de-dupe, missing handler skips, and exception-stop behavior. It remains non-live and does not execute handlers. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnQuestCompletedEvent` | `QuestCompletionCallbackDescriptor.FollowUpPlan`; `QuestCompletionFollowUpPlanService` | Handler Helper / Follow-Up Result Composition | Partial | Unit Tested | Partial Parity | C# carries detailed default follow-up `LOCKED`/`START` result descriptors through callback planning. Start-condition checks, XML recursion, level gates, and handler logic remain caller-provided/not executed. |
| `com.aionemu.gameserver.services.QuestService.addOrUpdateQuest` | `QuestCompletionFollowUpPlan`; `QuestCompletionCallbackDescriptor.FollowUpPlan` | Service / Quest Mutation Packet Plan Composition | Partial | Unit Tested | Partial Parity | Callback descriptors can carry non-live `ADD`/`UPDATE` packet-action intent. No live quest-state mutation, packet serialization/send, persistent-state transition, or DAO write exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` | `QuestCompletionFollowUpDescriptor.PacketAction` via `QuestCompletionCallbackDescriptor.FollowUpPlan` | Packet / Action Descriptor Composition | Partial | Unit Tested | Needs Verification | Packet action intent is carried through callback planning, but no callback follow-up packet bytes or sends are produced. |
| `com.aionemu.gameserver.questEngine.model.QuestEnv` | `QuestCompletionCallbackDescriptor`; `QuestCompletionFollowUpPlan` | DTO / Callback Context Projection | Partial | Unit Tested | Needs Verification | C# still records completed quest id/shared-env intent only; mutable env identity, player/target references, and handler-side env mutation remain unmodeled. |

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_CarriesDetailedFollowUpPlanWithoutExecutingHandler` | Callback descriptor carries detailed follow-up `ADD`/`LOCKED` result metadata and remains non-live. | Source-reviewed Java default callback and `addOrUpdateQuest`. |
| `CreatePlan_DoesNotCarryDuplicateFollowUpPlanForDuplicateRegistration` | Duplicate callback registration payloads are ignored after the first registered quest id. | Source-reviewed `registerOnQuestCompleted` de-dupe. |
| `CreatePlan_CarriesThrowingHandlerFollowUpPlanThenStopsRemainingHandlers` | Throwing handler descriptor carries its follow-up payload and stops later descriptors. | Source-reviewed whole-loop `try/catch`. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Follow-up result plans are metadata only; no live handler execution, start-condition execution, quest mutation, packet send, or persistence exists.
- Handler registration order remains caller-provided and not derived from Java reflection/script loading.
- Mutable `QuestEnv` identity and handler-side env mutation remain unmodeled.
- Quest-finish operation-plan tests do not yet assert the nested follow-up payload through `CompletionCallbackOperation`.
- Callback follow-up packet serialization/golden coverage remains missing.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial callback descriptor composition update
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/partial categories: Java runtime comparison, handler loader order, live handler execution, start-condition execution, quest-state mutation/packet sends, and packet serialization
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Add a focused quest-finish operation-plan regression proving nested callback follow-up result payloads survive through `QuestFinishOperationPlanService` callback composition after the quest update packet and before NPC-faction completion.

## Next Unit Handoff

Start with this file, `docs/QuestCompletionCallback-Audit.md`, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1028.

Recommended next slice:

1. Add/extend `QuestFinishOperationPlanServiceTests` to include callback descriptors with nested follow-up plans.
2. Verify descriptor placement and nested `ADD`/`UPDATE` result metadata survives operation-plan composition.
3. Keep live callback execution and packet sending disabled.
4. Update docs/parity table for the operation-plan composition evidence.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Quest-finish nested follow-up regression | operation-plan tests/docs | Medium | Best next implementation slice. |
| B | Reward XML projection planning | docs/static-data audit | Medium | Safe if it avoids callback/operation-plan files. |
| C | Persistence failure-ordering policy audit | docs-only | Medium | Safe read-only analysis before live writes. |

## Do Not Parallelize

- Live callback execution.
- Callback/quest-finish operation plan edits from multiple workers.
- Live DAO write implementation.
- Phase progress/completion docs.
