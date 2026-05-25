# Phase 6UR Completion - UOW-1052 QuestEngine Level-Changed Callback Plan

Date: May 25, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues after `docs/Phase-6UQ-Completion.md`.

## Last Completed Unit

- UOW-1052: `[Phase 6][UOW-1052] Stage QuestEngine level-change callbacks`
- Recent commits before this unit:
  - `a381c58bf [Phase 6][UOW-1051] Stage NPC faction level-up plan`
  - `4bb8f37b1 [Phase 6][UOW-1050] Stage upgrade-player level-change plan`
  - `1a5b88e20 [Phase 6][UOW-1049] Add level-up action animation constant`
- UOW-1052 status: complete; code, tests, progress notes, parity table, and this next handoff are included in this unit.

## Summary

UOW-1052 adds a non-live C# plan for Java `QuestEngine.onLevelChanged`. The planner records which race-scoped registered quest handlers Java would dispatch after a player level change, and which ones it would skip because the quest is COMPLETE, the handler is missing, or the registration belongs to another race. It does not execute dynamic quest handlers or mutate quest state.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | QuestEngine level-change callback planner | `QuestEngine.onLevelChanged`, `QuestEngine.registerOnLevelChanged`, `AbstractQuestHandler.onLevelChangedEvent` | new service/test files plus docs | Service Plan | Selected sequential | Medium | Concrete next Java-order level-change dependency after NPC faction level-up. |
| B | Faction leave system-message helper | `SM_SYSTEM_MESSAGE.STR_FACTION_LEAVE_BY_LEVEL_LIMIT` | `SmSystemMessage.cs`, packet tests | Yes if isolated | Low-Medium | Useful future prerequisite but not required for callback dispatch. |
| C | Nearby quest refresh audit | `PlayerController.updateNearbyQuests` | read-only Java/C# inspection and docs | Java Analysis | Yes read-only | Medium | Best next dependency after callback dispatch. |
| D | Compose existing sub-plans into XP execution metadata | `PlayerController.onLevelChange` | `QuestXpExecutionPlanService.cs`, XP tests | No | Medium | Shared XP execution order should remain orchestrator-owned. |

## Selected Work

The orchestrator implemented Candidate A sequentially. No sub-agent was spawned because the selected work owns shared level-change documentation and likely future XP execution composition.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestLevelChangedCallbackPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestLevelChangedCallbackPlanServiceTests.cs`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6UR-Completion.md`

## What Changed

- Added `QuestLevelChangedCallbackPlanService.CreatePlan`.
- Added registration metadata for Java `QuestEngine.registerOnLevelChanged`.
- Added descriptor statuses for planned dispatch, COMPLETE-state skip, missing-handler skip, and wrong-race registration metadata.
- Added focused unit coverage for Java callback order, race filtering, duplicate suppression, all non-COMPLETE states, no-dispatch statuses, and missing registration input.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestLevelChangedCallbackPlanServiceTests" --nologo` | Passed: 3 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1838 |

## Migration Parity Table - UOW-1052

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.onLevelChanged` | `Aion.GameServer.Services.QuestLevelChangedCallbackPlanService.CreatePlan`; `QuestLevelChangedCallbackPlan` | Quest Callback Dispatch Plan | Partial | Unit Tested | Partial Parity | Non-live plan preserves race-scoped registered quest order, skips COMPLETE quest states, skips missing handlers, and records planned handler dispatch. It does not invoke dynamic quest handlers, mutate quest state, send quest packets, log Java exceptions, or compose into XP execution. |
| `com.aionemu.gameserver.questEngine.QuestEngine.registerOnLevelChanged` | `QuestLevelChangedRegistration`; race filtering inside `QuestLevelChangedCallbackPlanService` | Registration Metadata | Partial | Unit Tested | Partial Parity | C# models null-race registration as both races and explicit-race registration as race-specific. It suppresses duplicate quest ids per Java list behavior. It does not load registrations from Java handler classes automatically. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.onLevelChangedEvent` | `QuestLevelChangedCallbackDescriptorStatus.PlannedDispatch` | Dynamic Handler Callback | Not Started | Unit Tested as metadata | Needs Verification | C# records dispatch intent only. Dynamic handler invocation, handler-specific logic, and exception handling remain unported. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.defaultOnLevelChangedEvent` | `QuestLevelChangedCallbackDescriptor` metadata | Quest Start/Lock Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java default callback can start or lock quests after checking start conditions, prerequisite quests, XML start conditions, and mission min-level rules. C# does not execute these mutations here. |
| `com.aionemu.gameserver.questEngine.model.QuestState`; `com.aionemu.gameserver.questEngine.model.QuestStatus` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Model / DTO | Partial | Unit Tested | Partial Parity | Planner uses exact Java COMPLETE skip behavior; START, REWARD, LOCKED, and missing states are dispatchable. Broader QuestState mutation/defaulting behavior remains outside this unit. |
| `com.aionemu.gameserver.controllers.PlayerController.onLevelChange` | `QuestXpExecutionPlanService`; `QuestLevelChangedCallbackPlanService` | Controller Dependency | Partial | Unit Tested as metadata | Needs Verification | UOW-1048 staged the full level-change order; UOW-1052 adds a dedicated QuestEngine callback dispatch sub-plan. The sub-plan is not composed into XP execution and live XP level-change execution remains disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_DispatchesRegisteredRaceCallbacksInJavaOrderAndSkipsCompleteAndMissingHandlers` | Unit | `QuestEngine.onLevelChanged`; `QuestEngine.registerOnLevelChanged`; `AbstractQuestHandler.onLevelChangedEvent` | Race-scoped callback order, duplicate suppression, COMPLETE skip, wrong-race skip metadata, missing-handler skip, and non-live dispatch descriptors. | Source-reviewed Java dispatch loop and registration logic. | Does not load real Java/C# dynamic handlers, execute handler code, or compare Java runtime logs. |
| `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_TreatsEveryNonCompleteQuestStateAsDispatchableLikeJava` | Unit | `QuestEngine.onLevelChanged` COMPLETE guard | START, REWARD, LOCKED, and missing quest states are dispatchable; COMPLETE is skipped. | Source-reviewed Java `qs == null || qs.getStatus() != COMPLETE` condition. | Does not execute handler side effects. |
| `QuestLevelChangedCallbackPlanServiceTests.CreatePlan_RecordsNoRegisteredNoDispatchAndMissingRegistrationBranches` | Unit | `QuestEngine.onLevelChanged`; `QuestEngine.registerOnLevelChanged` | No registered callbacks, no dispatches due to race filtering, and missing registration input statuses. | Conservative C# metadata around Java race-specific registration lists. | Missing-registration input is a C# planning guard, not a Java runtime branch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The QuestEngine callback plan is not composed into `QuestXpExecutionPlan` and is not connected to quest-finish execution.
- Dynamic quest handler loading and invocation remain unported for this path.
- Java `defaultOnLevelChangedEvent` can mutate quests to START or LOCKED and depends on `QuestService.checkStartConditions`, prerequisite quests, XML start conditions, mission min-level offsets, and packet side effects; none of that runs live here.
- Java catches and logs exceptions around the entire callback loop; C# planner has no live exception surface.
- Race registration metadata is caller-supplied; no C# extractor currently populates all `registerOnLevelChanged` calls from Java handlers.
- Java level-change side effects outside QuestEngine callback dispatch remain descriptors only: nearby refresh, guide HTML, skill auto-learn, custom rewards, and starter kits.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 1 non-live QuestEngine level-change callback dispatch sub-plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/partial categories: Java runtime comparison, live XP composition, dynamic quest handler execution, callback mutation side effects, and registration extraction from Java handlers
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds one more level-change callback prerequisite without enabling live XP mutation.

## Next Work Options

### Recommended Sequential Task

- Task: Audit or stage Java `PlayerController.updateNearbyQuests`.
- Why: level-up animation, upgrade-player, NPC faction level-up, and QuestEngine level-change callbacks now have non-live prerequisites. The next Java-order level-change dependency is nearby quest refresh.
- Suggested first slice: inspect Java `updateNearbyQuests`, C# nearby quest refresh surfaces, packet ordering, known-list dependencies, and whether a non-live planner can reuse existing nearby quest refresh services.
- Likely files: read-only Java/C# inspection plus `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, progress/handoff docs. Keep live XP execution disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Nearby quest refresh audit | read-only Java/C# inspection, docs owned by orchestrator | Medium | Best next Java-order dependency. |
| B | Faction leave system-message helper | `SmSystemMessage.cs`, packet tests | Low-Medium | Isolated packet prerequisite for future live NPC faction plan execution. |
| C | QuestEngine registration extractor audit | read-only Java handler inspection | Medium | Would discover all `registerOnLevelChanged` sources. |
| D | Compose existing sub-plans into XP metadata | `QuestXpExecutionPlanService.cs`, XP tests | Medium | Sequential only; shared XP execution order. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Audit/stage nearby quest refresh | Exact selected docs/code files | Any files assigned to a sidecar agent |
| Sidecar A | Read-only level-change registration extractor audit | Read-only Java/C# inspection only | All writes |
| Sidecar B | Read-only faction leave system-message helper audit | Read-only Java/C# inspection only | All writes |

Use sidecars only when the implementation scope does not require the same files. Shared docs remain orchestrator-owned.

## Do Not Parallelize

- `QuestXpExecutionPlanService.cs`: shared XP execution order.
- `QuestLevelChangedCallbackPlanService.cs`: new sub-plan and likely future composition point.
- `docs/PHASE-6-PROGRESS.md`, audit docs, and handoff docs: orchestrator-owned.

## Context Needed By Next Session

Start with this file, `docs/QuestXpReward-Audit.md`, `docs/QuestRewardSideEffects-Audit.md`, `docs/PHASE-6-PROGRESS.md` Session 1052, `QuestLevelChangedCallbackPlanService.cs`, `QuestLevelChangedCallbackPlanServiceTests.cs`, `QuestXpExecutionPlanService.cs`, and Java `PlayerController.updateNearbyQuests`.
