# Phase 6XT Completion - UOW-1132 NPC Controller DialogService Composition

Date: May 26, 2026

## Unit Of Work

UOW-1132: `[Phase 6][UOW-1132] Compose NPC controller DialogService fallback planning`

## Summary

UOW-1132 composes the descriptor-only `NpcDialogServiceSelectPlanService` behind `NpcDialogControllerDispatchPlanService`. The controller planner now optionally creates a `DialogService` sub-plan only when Java would reach it: NPC target, in talk range, and NPC AI returned `false`.

This remains staged only. It does not call production `GameServerConnection`, live talk-range geometry, NPC AI, `DialogService`, QuestEngine, packet sends, trade/goods static-data lookup, or service side effects.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogControllerDispatchPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XT-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogControllerDispatchPlanServiceTests\|NpcDialogServiceSelectPlanServiceTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 35 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,296 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1132

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `Aion.GameServer.Services.NpcDialogControllerDispatchPlanService` | Controller Dispatch Planner | Partial | Unit Tested | Partial Parity | Now optionally composes the staged `DialogService` planner only in the Java AI-false fallback branch. Live controller/AI execution remains disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlan` composed by `NpcDialogControllerDispatchPlanService` | Service Fallback Dependency | Partial | Unit Tested | Partial Parity | `DialogService` sub-plan is reachable through controller fallback facts. It remains descriptor-only with explicit static-data/service facts. |
| `com.aionemu.gameserver.ai.NpcAI.onDialogSelect` | `NpcDialogControllerDispatchInput.NpcAiHandledDialogSelect` | AI Dispatch Dependency | Not Started | Unit Tested as explicit input | Needs Verification | AI boolean return gates `DialogService` composition. No live AI handler lookup, threading, or side effects. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange` | `NpcDialogControllerDispatchInput.IsInTalkRange` | Geometry / Guard Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Out-of-range short-circuit prevents service composition. Range math remains explicit input only. |
| `com.aionemu.gameserver.services.DialogService.handleQuestDialogueOrSendNextPage` | Composed `NpcDialogServiceSelectPlan` descriptors | Service Helper Dependency | Partial | Existing Unit Coverage | Needs Verification | Still descriptor-only; no live QuestEngine or `SM_DIALOG_WINDOW` send. |
| `com.aionemu.gameserver.dataholders.TradeListData` / `TradeListTemplate` | `NpcDialogServiceSelectFacts` | Static Data Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Trade-list facts can now be carried through controller fallback composition. No live XML/static-data lookup or legion filtering. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_ComposesDialogServicePlanOnlyWhenAiFallsBack` | AI false fallback composes `DialogService` BUY plan and preserves Java price modifier formula through service facts. | Source-reviewed Java fallback condition and BUY formula. |
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_DoesNotComposeDialogServicePlanForShortCircuitedControllerBranches` | Out-of-range and AI-handled branches do not create `DialogService` sub-plans. | Source-reviewed Java guard order. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not consume the staged branch/controller/service composition.
- Controller dispatch, NPC AI, talk-range geometry, QuestEngine, packet sends/serialization, trade/goods static-data lookup, legion filtering, and service side effects remain disabled.
- `DialogService` branch behavior remains grouped/descriptive for many actions; no Java runtime comparison has verified the composed planner chain.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live controller/service composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: production routing, live talk-range, live AI/controller execution, live QuestEngine/packets, live static-data/service side effects, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit links the staged `DialogService` fallback planner to the staged NPC controller dispatch planner.

## Next Recommended Unit Of Work

Compose `NpcDialogServiceSelectFacts` up into `QuestDialogNpcTargetBranchInputAssemblyPlanService` controller facts so the top-level NPC-target adapter can produce branch, interaction, controller, and `DialogService` fallback metadata from one non-live snapshot.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Top-level adapter service-facts composition | `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs` and tests | Medium | Recommended next; keep optional and non-live. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java `DialogAction.nameOf` behavior before loader warnings. |
| C | Trade-list static-data input adapter | New planner/tests around trade/goods facts | Medium | Needs `TradeListData` and `GoodsListData` XML review; avoid live packet routing. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- Existing NPC dialog adapter/controller planner files: assign exclusive ownership for composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
