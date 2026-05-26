# Phase 6XR Completion - UOW-1130 NPC Dialog Controller Composition

Date: May 26, 2026

## Unit Of Work

UOW-1130: `[Phase 6][UOW-1130] Compose NPC dialog controller dispatch planning`

## Summary

UOW-1130 composes the non-live `NpcDialogControllerDispatchPlanService` into `QuestDialogNpcTargetBranchInputAssemblyPlanService`. The adapter can now produce staged branch, interaction, and controller-dispatch metadata in Java order when explicit controller facts are provided.

This remains staged only. It does not call production `GameServerConnection`, live known-list lookup, live talk-range geometry, NPC AI, `DialogService`, audit logging, or packet sends.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XR-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests\|NpcDialogControllerDispatchPlanServiceTests\|QuestDialogNpcTargetBranchPlanServiceTests\|NpcDialogInteractionAllowedPlanServiceTests" --nologo` | Passed: 49 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,276 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1130

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService`; `QuestDialogNpcTargetBranchPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Adapter now can produce branch, interaction, and optional controller-dispatch metadata in Java order. Production packet routing remains disabled. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `NpcDialogControllerDispatchPlanService` composed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Controller Dispatch Dependency | Partial | Unit Tested | Partial Parity | Controller facts are composed only after branch dispatch. Talk-range and AI handled state remain explicit facts; no live controller call. |
| `com.aionemu.gameserver.controllers.CreatureController.onDialogSelect` | `NpcDialogControllerDispatchPlanService` composed non-NPC branch | Controller Base Method | Partial | Unit Tested | Partial Parity | Non-NPC creature dispatch can be represented as a no-op controller sub-plan. No live creature controller execution. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `NpcDialogInteractionAllowedPlanService` consumed before controller dispatch composition | Service Guard Dependency | Partial | Existing Unit Coverage | Partial Parity | Interaction plan result still gates branch dispatch before controller composition. Facts are explicit; no live world/player dependency resolution. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceFallbackDescriptor` reachable through composed controller plan | Service Fallback Descriptor | Partial | Unit Tested | Needs Verification | Fallback is descriptor-only and only appears when the branch dispatches and NPC AI is modeled as not handling the select. Full `DialogService` switch remains unported here. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange` | `QuestDialogNpcControllerDispatchFacts.IsInTalkRange` | Geometry / Guard Dependency | Not Started | Unit Tested as explicit input | Needs Verification | Still explicit input only; no Java range/radius/geo comparison. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_ComposesControllerDispatchPlanWhenBranchDispatches` | A dispatching NPC branch can compose controller metadata and `DialogService` fallback descriptor. | Source-reviewed Java `CM_DIALOG_SELECT` branch and `NpcController.onDialogSelect` call order. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_UsesControllerDispatchFactsAfterBranchDispatch` | Out-of-range controller facts return before AI even if AI handled input is true. | Source-reviewed Java talk-range guard order. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DoesNotComposeControllerDispatchWhenBranchIsBlocked` | Interaction-blocked branches do not create controller sub-plans. | Source-reviewed Java branch guards. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_KeepsControllerDispatchOptional` | Existing adapter users still receive branch plans without controller facts. | Deterministic C# regression coverage. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call this composed adapter.
- Known-list lookup, target typing, static `DialogAction.nameOf` warning behavior, audit logger side effects, live talk-range geometry, live NPC AI dispatch, live `DialogService` fallback, packet ordering, and threading/player-ordering remain disabled.
- `DialogService.onDialogSelect` remains descriptor-only; quest/dialog page behavior, packets, inventory/storage/warehouse, serialization, precision/rounding, and date/time side effects are not ported in this unit.
- No Java runtime comparison has verified the composed adapter/controller behavior.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live adapter/controller composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: production socket routing, live known-list/type resolution, live interaction dependency resolution, live talk-range geometry, live AI/DialogService dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit composes the staged controller-dispatch decision layer into the staged NPC-target dialog branch.

## Next Recommended Unit Of Work

Start a read-only `DialogService.onDialogSelect` branch audit/planner for the fallback path, beginning with `questId == 0` service/action branches and explicit packet/side-effect descriptors. Keep it non-live and do not route production packets.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `DialogService.onDialogSelect` branch audit | New service/tests near dialog services | Medium | Recommended next; start with descriptor-only `questId == 0` branches. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java unknown-action warning behavior before adding loader warnings. |
| C | Talk-range geometry audit | New non-live range planner/tests | Medium | Requires careful Java `PositionUtil` review and C# world object geometry mapping. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`: avoid concurrent edits with future adapter wiring.
- Phase 6 progress/handoff docs: orchestrator-owned.
