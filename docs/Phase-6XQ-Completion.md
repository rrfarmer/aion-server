# Phase 6XQ Completion - UOW-1129 NPC Controller Dialog Dispatch Planning

Date: May 26, 2026

## Unit Of Work

UOW-1129: `[Phase 6][UOW-1129] Model NPC controller dialog dispatch planning`

## Summary

UOW-1129 adds a non-live `NpcDialogControllerDispatchPlanService` for the Java controller layer reached by the staged `CM_DIALOG_SELECT` NPC-target branch. The planner models the Java `CreatureController.onDialogSelect` no-op, `NpcController.onDialogSelect` talk-range guard, NPC AI boolean dispatch, and `DialogService.onDialogSelect` fallback descriptor.

This remains staged only. It does not call production `GameServerConnection`, live range geometry, NPC AI, `DialogService`, audit logging, or packet sends.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogControllerDispatchPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XQ-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogControllerDispatchPlanServiceTests\|QuestDialogNpcTargetBranchPlanServiceTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 23 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,272 tests after one initial 2-minute tool timeout before result. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1129

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `Aion.GameServer.Services.NpcDialogControllerDispatchPlanService` | Controller Dispatch Planner | Partial | Unit Tested | Partial Parity | Models talk-range return, AI boolean handling, and `DialogService` fallback from explicit inputs. Does not execute live controller, AI, packets, or service logic. |
| `com.aionemu.gameserver.controllers.CreatureController.onDialogSelect` | `NpcDialogControllerDispatchPlanService` non-NPC branch | Controller Base Method | Partial | Unit Tested | Partial Parity | Models the empty base implementation for known non-NPC creatures. No live creature controller dispatch is executed. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange` | `NpcDialogControllerDispatchInput.IsInTalkRange` | Geometry / Guard Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Talk range is an explicit fact only. C# does not calculate Java 3D/2D range, target/player radii, heading, map, or geo behavior in this unit. |
| `com.aionemu.gameserver.ai.NpcAI.onDialogSelect` | `NpcDialogControllerDispatchInput.NpcAiHandledDialogSelect` and `CallsNpcAi` metadata | AI Dispatch Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Java AI boolean return is represented as input. No live AI event dispatch, handler lookup, threading, or side effects are ported here. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceFallbackDescriptor` | Service Fallback Descriptor | Partial | Unit Tested | Needs Verification | Fallback descriptor preserves action, target, quest, and extended reward index. Dialog action switch, quest branch behavior, packets, inventory/storage/warehouse side effects, and serialization are not implemented in this unit. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_ReturnsNoOpForKnownNonNpcCreatureController` | Known non-NPC creature target produces no AI or fallback dispatch. | Source-reviewed Java `CreatureController.onDialogSelect` empty method body. |
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_ReturnsBeforeAiWhenNpcIsOutsideTalkRange` | Out-of-range NPC select returns before AI and fallback. | Source-reviewed Java `NpcController.onDialogSelect` guard order. |
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_StopsAfterAiWhenNpcAiHandlesDialogSelect` | AI-handled dialog select does not call `DialogService`. | Source-reviewed Java NPC AI boolean branch. |
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_FallsBackToDialogServiceWhenNpcAiDoesNotHandleDialogSelect` | AI false plans `DialogService` fallback and preserves action/quest/extended reward inputs. | Source-reviewed Java fallback call. |
| `NpcDialogControllerDispatchPlanServiceTests.CreatePlan_PreservesOriginalControllerDispatchDescriptor` | Original dispatch descriptor, including Java `prevDialogId`/C# `LastPage`, remains available while fallback omits it like Java. | Source-reviewed Java method signature and fallback call. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call this planner.
- Talk range is not calculated from live player/NPC geometry, radii, map state, or Java `PositionUtil`.
- NPC AI dispatch is not executed; handler lookup, event ordering, threading, and AI side effects remain unported.
- `DialogService.onDialogSelect` is represented only as a fallback descriptor; its large switch, quest integration, packets, inventory/storage/warehouse, precision, serialization, and date/time side effects remain unmodeled.
- No Java runtime comparison has verified the controller dispatch planner.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live controller dispatch planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: production socket routing, live talk-range geometry, live AI dispatch, live `DialogService` fallback, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds the staged NPC controller dispatch decision layer needed after the NPC-target branch planner.

## Next Recommended Unit Of Work

Compose `NpcDialogControllerDispatchPlanService` into `QuestDialogNpcTargetBranchInputAssemblyPlanService` as an optional non-live controller-dispatch plan, so the staged NPC-target branch can produce branch, interaction, and controller-dispatch metadata without live routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Controller-dispatch composition | `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs` and tests | Medium | Recommended next; keep non-live and descriptor-only. |
| B | `DialogService.onDialogSelect` branch audit | New service/docs/tests near dialog services | Medium | Start mapping action switch and quest/service packet dependencies. |
| C | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java unknown-action warning behavior before adding loader warnings. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`: avoid concurrent edits if the next unit composes controller dispatch.
- Phase 6 progress/handoff docs: orchestrator-owned.
