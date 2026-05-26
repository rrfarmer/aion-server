# Phase 6XA Completion - UOW-1113 Quest Bonus Handler Outcome Policy Coverage

Date: May 26, 2026

## Unit Of Work

UOW-1113: `[Phase 6][UOW-1113] Cover quest bonus handler outcome policy`

## Summary

UOW-1113 adds focused non-live coverage for the Java handler ordering documented in UOW-1112.

The C# handler outcome planner now has explicit coverage for first loaded `UNKNOWN` stopping later handlers, and a non-live `CreateHandlerExceptionPlan` helper to represent Java `QuestEngine.onBonusApplyEvent` exception handling as `FAILED`. Dynamic handler execution remains disabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestBonusHandlerOutcomePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestBonusHandlerOutcomePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XA-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusHandlerOutcomePlanServiceTests\|QuestBonusRewardPlanningReportServiceTests\|QuestBonusRewardPlanningInputAdapterServiceTests" --nologo` | Passed: 24 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,198 tests. |

## Migration Parity Table - UOW-1113

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent` | `Aion.GameServer.Services.QuestBonusHandlerOutcomePlanService` | Dynamic Handler Dispatch Planner | Partial | Unit Tested | Partial Parity | Tests now cover first loaded `UNKNOWN` stopping later handlers and exception-as-`FAILED` representation. C# still does not execute dynamic handlers or source real script load order. |
| `com.aionemu.gameserver.questEngine.handlers.HandlerResult` | `Aion.GameServer.Services.QuestBonusHandlerResult`; `QuestBonusHandlerOutcomeStatus` | Enum / Handler Result Policy | Partial | Unit Tested | Partial Parity | `UNKNOWN`, `SUCCESS`, `FAILED`, and exception-to-failed policy are represented for non-live planning. Runtime Java enum execution is not compared. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestBonusRewardPlanningReportService` | Reward Service / Handler Suppression Policy | Partial | Existing Unit Coverage | Partial Parity | Existing report tests cover `FAILED` suppressing BonusService and `UNKNOWN` allowing it. Production finish still does not invoke adapter/report generation. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.onBonusApplyEvent` | custom unknown handler registration in `QuestBonusHandlerOutcomePlanServiceTests` | Handler Base / Test Fixture | Partial | Unit Tested | Needs Verification | Test fixture represents a handler returning `UNKNOWN`, proving first loaded unknown stops later handlers in the C# non-live planner. No dynamic override execution yet. |
| `quest.event_quests._80016EventSockHop`; `quest.event_quests._80018EventSockItToEm`; LUNAR/RIFT event handlers | `QuestBonusHandlerOutcomePlanService` audited registrations | Quest Handler Models | Partial | Unit Tested | Partial Parity | Existing and new tests cover representative MOVIE/gate behavior and ordering. Movie side effects, Java RNG, exact script ordering, and direct mutation remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestBonusHandlerOutcomePlanServiceTests.CreatePlan_FirstLoadedUnknownHandlerStopsLaterHandlersLikeJavaQuestEngine` | First loaded handler returning `UNKNOWN` is returned immediately and later successful handlers are not evaluated. | Source-reviewed Java `QuestEngine.onBonusApplyEvent`; no Java runtime comparison. |
| `QuestBonusHandlerOutcomePlanServiceTests.CreateHandlerExceptionPlan_RepresentsJavaCatchAsFailedOutcome` | Java exception catch behavior can be represented as `FAILED` with `HandlerException` status. | Source-reviewed Java catch block; no dynamic execution. |

## Remaining Risks

- C# still does not execute dynamic quest handlers for bonus events.
- Real Java script load order and reload behavior are not runtime-compared.
- Exception behavior is represented by a non-live helper, not by an execution boundary around actual handlers.
- Movie side effects, direct reward item mutation, Java RNG, packet sends, persistence, rollback, threading/player-ordering, serialization, and Java runtime comparison remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live handler exception/failure policy surface
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 7 blocked/partial categories: dynamic handler execution, script load order, runtime exception boundary, movie side effects, direct reward mutation, Java RNG, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a production reward-projection source audit for quest finish: trace how `QuestFinishRewardTemplateProjection` can be sourced from real quest templates at the future `CM_DIALOG_SELECT`/finish boundary, and document missing lookup/index contracts before any socket wiring.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Production reward-projection source audit | Read-only or audit doc | Low/Medium | Recommended next unit before socket wiring. |
| B | Disabled selected-bonus envelope audit | Existing bonus selection services/tests | Medium | Keep Java RNG/live selection disabled. |
| C | Dynamic handler registry source audit | Read-only Java/C# audit doc | Medium | Needed before live handler execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Audit reward-projection source | Read-only source inspection plus new audit doc/docs | Production code unless explicitly selected |
| Explorer A | Audit dynamic handler registry source | Read-only inspection or separate audit doc | Reward-projection audit docs if Orchestrator owns them |

## Do Not Parallelize

- `QuestBonusHandlerOutcomePlanService.cs`: just changed.
- `QuestFinishOperationPlanService.cs`: recently changed shared planner.
- `GameServerConnection.cs`: production socket path remains disabled.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1113 adds tests and a non-live exception-policy helper for bonus handler outcomes.
- Dynamic handler execution remains disabled.
- Next safest unit is a reward-projection source audit before production finish/socket wiring.
