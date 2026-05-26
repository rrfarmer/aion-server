# Phase 6XL Completion - UOW-1124 NPC Dialog Non-Self Branch Planner

Date: May 26, 2026

## Unit Of Work

UOW-1124: `[Phase 6][UOW-1124] Model NPC dialog non-self branch planning`

## Summary

UOW-1124 adds a non-live planner for the Java `CM_DIALOG_SELECT` non-self target branch. The planner models the branch after self/reportable quest handling: unknown dialog action rejection, self-target handoff, known-list miss, non-creature target no-op, NPC function-action support audit, NPC interaction-allowed audit, and final controller dispatch intent.

This is staged only. It does not call AI, `DialogService`, `NpcController`, packet sends, audit logging, or production `GameServerConnection` routing.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogNpcTargetBranchPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XL-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogNpcTargetBranchPlanServiceTests\|NpcDialogTargetingServiceTests\|NpcDialogRequestServiceTests" --nologo` | Passed: 20 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,237 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1124

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogNpcTargetBranchPlanService` | Packet / Non-Live Branch Planner | Partial | Unit Tested | Partial Parity | Planner models Java non-self branch guard order and controller dispatch intent. It is not wired into `GameServerConnection.HandleDialogSelectAsync`, does not read the live known list, and does not execute controller dispatch. |
| `com.aionemu.gameserver.model.DialogAction` | `QuestDialogNpcTargetBranchPlanService`; explicit `DialogActionKnown` / `Select1` boundary | Dialog Action Constants | Partial | Unit Tested | Needs Verification | Planner uses Java `SELECT1 = 1011` threshold and an explicit known-action input because C# does not have full `DialogAction.nameOf` parity. Full dialog action registry/name logging remains incomplete. |
| `com.aionemu.gameserver.dataholders.NpcData` | `QuestDialogNpcTargetBranchInput.IsFunctionDialog` | Static Data Repository Dependency | Partial | Unit Tested as explicit input | Needs Verification | Java `DataManager.NPC_DATA.isFunctionDialog` is represented as an input flag. Production `NpcTemplateTable` does not expose a global function-dialog set for this branch yet. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplate` | `NpcTemplateSummary.SupportsDialogAction`; `QuestDialogNpcTargetBranchInput.NpcSupportsAction` | Static Template DTO | Partial | Unit Tested as explicit input | Needs Verification | Unsupported function actions are rejected before interaction checks. Template support comes from explicit input in this planner; production lookup/wiring remains missing. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `QuestDialogNpcTargetBranchInput.InteractionAllowed` | Service Guard Dependency | Partial | Unit Tested as explicit input | Needs Verification | Planner preserves Java guard condition `(isFunctionDialog || dialogActionId < SELECT1) && !isInteractionAllowed`. Summon-owner, sub-dialog, siege, item, skill, abyss-rank, and other restriction logic remains unported here. |
| `com.aionemu.gameserver.controllers.CreatureController.onDialogSelect` | `QuestDialogNpcControllerDispatchDescriptor` | Controller Dispatch Intent | Partial | Unit Tested | Needs Verification | Known non-NPC creatures dispatch without NPC guards. Live controller execution is not implemented. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `QuestDialogNpcControllerDispatchDescriptor`; future NPC controller bridge | Controller Dispatch Intent | Partial | Unit Tested | Needs Verification | Known NPCs dispatch after function/action and interaction guards. Java talk-range check, AI `onDialogSelect`, and `DialogService.onDialogSelect` fallback remain disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | Future dialog service bridge | Dialog Service | Not Started | No Tests | Unknown | Discovered dependency for the next layers. Quest dialog routing, dialog-window pages, AI fallback, and packet sends are not ported by this unit. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_ReturnsSelfTargetBranchBeforeKnownObjectLookup` | Target `0` or player object id is not handled by the non-self planner. | Source-reviewed Java branch split. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_RejectsUnknownDialogActionBeforeTargetBranching` | Unknown dialog action rejects before target branch logic. | Source-reviewed Java `DialogAction.nameOf` null guard. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_ReturnsUnknownTargetWhenKnownListLookupMisses` | Known-list miss produces no dispatch. | Source-reviewed Java `player.getKnownList().getObject` branch. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_ReturnsTargetNotCreatureForKnownNonCreatureObjects` | Known non-creature targets produce no dispatch. | Source-reviewed `instanceof Creature` check. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_RejectsUnsupportedFunctionActionBeforeInteractionCheck` | Function dialog unsupported by NPC template rejects before interaction check. | Source-reviewed Java audit guard order. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_RejectsInteractionOnlyForFunctionOrPreSelectNpcActions` | Interaction guard applies for function actions and pre-`SELECT1` actions. | Source-reviewed Java condition. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_AllowsSelectPageNpcActionsWithoutInteractionGuard` | `SELECT1` page actions that are not function dialogs bypass the interaction guard and dispatch. | Source-reviewed Java condition. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_DispatchesKnownNpcTargetAfterJavaGuards` | Known NPC dispatch descriptor carries target, action, last page, quest id, and extended reward index. | Source-reviewed controller call signature. |
| `QuestDialogNpcTargetBranchPlanServiceTests.CreatePlan_DispatchesKnownNonNpcCreatureWithoutNpcGuards` | Known non-NPC creatures dispatch without NPC-specific guards. | Source-reviewed branch nesting. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` does not use this planner.
- `DialogAction.nameOf`, `DataManager.NPC_DATA.isFunctionDialog`, known-list lookup, creature typing, and NPC template support are explicit inputs rather than live runtime lookups.
- `DialogService.isInteractionAllowed` is represented as an input; its summon owner, sub-dialog, siege, skill, item, abyss-rank, and ranking restrictions remain unported here.
- `NpcController.onDialogSelect` talk-range check, AI dispatch, `DialogService.onDialogSelect` fallback, quest dialog routing, packet sends, and audit logging remain disabled.
- Threading/player-ordering and Java runtime comparison remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live NPC dialog non-self branch planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 7 blocked/partial categories: production socket routing, live known-list lookup, function-dialog registry, interaction restrictions, NPC controller/AI dispatch, dialog service fallback, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit adds a staged model for the non-self dialog branch without enabling live NPC dispatch.

## Next Recommended Unit Of Work

Add a static-data bridge for the global function-dialog set needed by the non-self branch: materialize `NpcData.isFunctionDialog` equivalents from NPC template function dialog IDs, expose it through static data or a small lookup, and test it against synthetic/real static data. Keep `GameServerConnection` routing disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Function-dialog static-data lookup | `NpcTemplateTable`/new lookup tests | Medium | Recommended next; may touch shared static data. |
| B | DialogService interaction audit | Read-only audit doc | Medium | Map summon/sub-dialog restrictions before live interaction guard. |
| C | NpcController dialog dispatch audit | Read-only audit doc | Medium | Map talk-range, AI, and `DialogService` fallback dependencies. |

## Do Not Parallelize

- `GameServerConnection.cs`: production dialog routing remains high-risk.
- `StaticData.cs` and NPC static data loaders: use exclusive ownership if touched.
- Phase 6 progress/handoff docs: orchestrator-owned.
