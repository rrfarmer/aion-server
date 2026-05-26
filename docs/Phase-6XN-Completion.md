# Phase 6XN Completion - UOW-1126 NPC Dialog Target Input Adapter Planner

Date: May 26, 2026

## Unit Of Work

UOW-1126: `[Phase 6][UOW-1126] Compose NPC dialog target branch inputs`

## Summary

UOW-1126 adds a non-live input assembly planner for the Java `CM_DIALOG_SELECT` NPC-target branch. It composes the static-data pieces introduced in UOW-1124 and UOW-1125: global `NpcData.isFunctionDialog` equivalent from `NpcTemplateTable`, target-specific `NpcTemplate.supportsAction` equivalent from `NpcTemplateSummary`, and the existing branch planner.

This is staged only. It does not call production socket routing, world known-list lookup, `DialogService`, controllers, AI, audit logging, or packet sends.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XN-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests\|QuestDialogNpcTargetBranchPlanServiceTests\|NpcTemplateTableTests" --nologo` | Passed: 18 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,243 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1126

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchInputAssemblyPlanService`; `QuestDialogNpcTargetBranchPlanService` | Packet / Non-Live Input Adapter | Partial | Unit Tested | Partial Parity | Adapter composes explicit non-self branch inputs and invokes the non-live branch planner. Production `GameServerConnection.HandleDialogSelectAsync` still does not use it. |
| `com.aionemu.gameserver.dataholders.NpcData.isFunctionDialog` | `NpcTemplateTable.IsFunctionDialog` consumed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Data Dependency | Partial | Unit Tested | Partial Parity | Adapter now derives the global function-dialog flag from static NPC data. Java unknown-action warning and runtime data-load comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplate.supportsAction` | `NpcTemplateSummary.SupportsDialogAction` consumed by `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Static Template Dependency | Partial | Unit Tested | Needs Verification | Adapter derives target-specific support separately from global function-dialog recognition. Full Java JAXB/TalkInfo behavior remains unverified. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `QuestDialogNpcTargetBranchRuntimeSnapshot.InteractionAllowed` | Service Guard Dependency | Partial | Unit Tested as explicit input | Needs Verification | Adapter deliberately keeps interaction as an explicit dependency. Summon-owner, sub-dialog, siege, skill, item, abyss-rank, and ranking restrictions remain unported here. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` / `CreatureController.onDialogSelect` | `QuestDialogNpcControllerDispatchDescriptor` from composed branch plan | Controller Dispatch Intent | Partial | Existing Unit Coverage | Needs Verification | Adapter can produce the dispatch descriptor through the branch planner, but no live controller, AI, talk-range, or `DialogService` fallback is executed. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DerivesFunctionDialogFromGlobalNpcTemplateTable` | Global function-dialog true plus target support false produces unsupported function action. | Source-reviewed Java guard order. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DerivesNpcSupportFromTargetTemplate` | Target-supported global function action dispatches after guards. | Source-reviewed Java branch composition. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_KeepsInteractionAllowedAsExplicitDependency` | Interaction false still blocks a supported function dialog. | Source-reviewed Java `DialogService.isInteractionAllowed` guard. |
| `QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests.CreatePlan_DoesNotApplyNpcSupportGuardToNonNpcCreatures` | Non-NPC creatures bypass NPC-specific support/interaction guards and dispatch. | Source-reviewed Java branch nesting. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not assemble or execute this branch.
- Known-list lookup, target object typing, and interaction checks are explicit snapshot inputs rather than live world state.
- Java `DialogAction.nameOf` registry validation, unknown function-dialog warnings, audit logger side effects, packet ordering, threading/player-ordering, AI dispatch, and `DialogService.onDialogSelect` fallback remain disabled.
- No Java runtime comparison has verified the static-data and branch-adapter behavior.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live NPC dialog branch input adapter
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: production socket routing, live known-list/type resolution, interaction restrictions, controller/AI/DialogService dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit composes the static-data prerequisite into the staged NPC-target branch planner.

## Next Recommended Unit Of Work

Add a read-only `DialogService.isInteractionAllowed` parity audit/planner for the NPC dialog branch, starting with Java's summon-owner and sub-dialog restrictions. Keep it explicit and non-live until group/alliance/legion/fort/rank/item/skill dependencies have C# homes.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | DialogService interaction audit/planner | New service/tests or docs-only audit | Medium | Recommended next; keep non-live. |
| B | NpcController dispatch audit | Read-only audit doc | Medium | Map talk-range, AI, and `DialogService` fallback dependencies. |
| C | Dialog action registry warning audit | Static-data validation audit/tests | Medium | Compare Java unknown-action warning behavior before adding loader warnings. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `NpcTemplateTable.cs` and static-data loader paths: avoid concurrent edits with dialog action registry work.
- Phase 6 progress/handoff docs: orchestrator-owned.
