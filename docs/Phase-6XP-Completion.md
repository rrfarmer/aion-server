# Phase 6XP Completion - UOW-1128 NPC Dialog Interaction Composition

Date: May 26, 2026

## Unit Of Work

UOW-1128: `[Phase 6][UOW-1128] Compose NPC dialog interaction planning`

## Summary

UOW-1128 composes the non-live `NpcDialogInteractionAllowedPlanService` into `QuestDialogNpcTargetBranchInputAssemblyPlanService`. The adapter now optionally accepts explicit interaction facts, runs the staged Java `DialogService.isInteractionAllowed` planner, and feeds its result into the existing NPC-target branch planner.

This remains staged only. It does not call production `GameServerConnection`, live known-list lookup, player/world membership resolvers, controller dispatch, audit logging, or packet sends.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XP-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests\|QuestDialogNpcTargetBranchPlanServiceTests\|NpcDialogInteractionAllowedPlanServiceTests" --nologo` | Passed: 40 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,267 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1128

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService`; `QuestDialogNpcTargetBranchPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Adapter now composes global function-dialog, target support, and optional interaction-plan result before invoking the non-live branch planner. Production socket routing remains disabled. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `NpcDialogInteractionAllowedPlanService`; optional `QuestDialogNpcTargetBranchRuntimeSnapshot.InteractionInput` | Service Guard Dependency | Partial | Unit Tested | Partial Parity | Interaction facts can now feed the NPC branch adapter. The facts are still explicit inputs; no live player/NPC/world state is resolved. |
| `com.aionemu.gameserver.services.DialogService.isSubDialogRestricted` | `NpcDialogInteractionAllowedPlan` consumed by NPC target branch adapter | Service Guard Dependency | Partial | Unit Tested | Needs Verification | Sub-dialog decisions can block the branch planner through `InteractionAllowed=false`. Live fort, skill, item, ranking, and legion dominion dependencies remain absent. |
| `com.aionemu.gameserver.dataholders.NpcData.isFunctionDialog` | `NpcTemplateTable.IsFunctionDialog` consumed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Data Dependency | Partial | Existing Unit Coverage | Needs Verification | Unchanged in this unit; still provides the global function-dialog flag used before interaction checks. Java unknown-action warning behavior remains unverified. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplate.supportsAction` | `NpcTemplateSummary.SupportsDialogAction` consumed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Template Dependency | Partial | Existing Unit Coverage | Needs Verification | Unchanged in this unit; still provides target-specific support before interaction checks. Full JAXB/TalkInfo behavior remains unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_UsesInteractionPlanWhenProvided` | Optional interaction facts override the fallback flag and block the branch planner with Java-style illegal-action audit metadata. | Source-reviewed Java `CM_DIALOG_SELECT` and `DialogService.isInteractionAllowed` branch order. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_AllowsWhenInteractionPlanAllows` | An allowing interaction plan feeds `InteractionAllowed=true` and permits controller dispatch descriptor creation. | Source-reviewed Java branch order. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not use this adapter.
- The interaction planner consumes explicit facts instead of resolving live group/alliance/legion, fort-zone, SiegeService, skill, inventory, abyss ranking, legion dominion, or player-level state.
- Known-list lookup, target object typing, Java `DialogAction.nameOf`, audit logger side effects, packet ordering, threading/player-ordering, AI dispatch, and `DialogService.onDialogSelect` fallback remain disabled.
- No Java runtime comparison has verified the composed adapter behavior.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live adapter composition
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: production socket routing, live known-list/type resolution, live interaction dependency resolution, controller/AI/DialogService dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit wires staged interaction planning into the staged NPC-target dialog branch.

## Next Recommended Unit Of Work

Add a read-only `NpcController.onDialogSelect` dispatch audit/planner for Java talk-range check, AI `onDialogSelect`, and `DialogService.onDialogSelect` fallback inputs. Keep the result non-live and do not route production packets yet.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | NpcController dispatch planner | New service/tests near NPC dialog planning | Medium | Recommended next; keep non-live. |
| B | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java unknown-action warning behavior before adding loader warnings. |
| C | `DialogService.onDialogSelect` read-only branch audit | Docs-only or new audit notes | Medium | Map quest/dialog page/service packets before live fallback. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`: avoid concurrent edits with future branch composition work.
- Phase 6 progress/handoff docs: orchestrator-owned.
