# Phase 6XI Completion - UOW-1121 Quest Finish Guarded Socket Input Assembly

Date: May 26, 2026

## Unit Of Work

UOW-1121: `[Phase 6][UOW-1121] Gate quest finish socket input assembly`

## Summary

UOW-1121 adds a non-live guarded socket input assembly planner for quest-finish auto rewards. The new planner runs the existing Java-shaped `CM_DIALOG_SELECT` self-target, quest-template, `can_report`, and auto-reward action guard before allowing `QuestFinishSocketInputAssemblyPlanService` to assemble reward projection inputs.

This closes the gap where the newer socket input assembly planner could be called directly without the Java branch split. The production `GameServerConnection.HandleDialogSelectAsync` path remains disabled for quest finish.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishSocketGuardedInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XI-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishSocketGuardedInputAssemblyPlanServiceTests\|QuestDialogAutoRewardGuardPlanServiceTests\|QuestFinishSocketInputAssemblyPlanServiceTests\|QuestFinishSocketOperationCompositionPlanServiceTests" --nologo` | Passed: 33 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,222 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1121

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestFinishSocketGuardedInputAssemblyPlanService`; `QuestDialogAutoRewardGuardPlanService`; `QuestFinishSocketInputAssemblyPlanService` | Packet / Guarded Planner Adapter | Partial | Unit Tested | Partial Parity | The guarded planner preserves Java branch order for target `0` or player object id, missing template, non-reportable template, and auto-reward action before input assembly. It is not wired into `GameServerConnection.HandleDialogSelectAsync`, does not invoke NPC controller dispatch, and performs no packet/runtime integration. |
| `com.aionemu.gameserver.model.DialogAction` | `QuestDialogAutoRewardGuardPlanService.IsAutoRewardDialogAction`; guarded planner tests | Dialog Action Constants | Partial | Unit Tested | Partial Parity | Tests cover auto-reward `108` and reject normal reward action `8` through the guarded boundary. C# still does not centralize all Java dialog actions or enum-name logging. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `QuestFinishRewardProjectionLookupTable`; `QuestFinishSocketGuardedInputAssemblyPlanService` | Static Data Repository / Lookup | Partial | Unit Tested | Needs Verification | Guarded planner uses the staged lookup table to resolve the template summary before input assembly. Java JAXB runtime identity, memory profile, and production socket lookup behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestDialogAutoRewardGuardTemplateInput` | Static Template DTO | Partial | Unit Tested | Partial Parity | `CanReport` gates input assembly in Java order. Full Java template fields, script hooks, JAXB defaults, serialization, and target NPC behavior remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishSocketGuardedInputAssemblyPlanService`; future guarded socket-to-operation composition | Finish Invocation Boundary | Partial | Unit Tested | Needs Verification | The planner only permits non-live input assembly after Java guards. It does not call finish, compose operation plans by itself, mutate quests, dispatch callbacks, persist, or send packets. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishSocketInputAssemblyPlanService`; `QuestFinishRewardProjectionLookupPlanService` | Reward Selection Planner | Partial | Existing Unit Coverage | Needs Verification | Ready guarded input flows to existing reward projection assembly, but live item/non-item mutation, bonus handlers, RNG/Chance selection, and `ItemService.addItem` remain disabled. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardTemplateProjection`; `QuestDialogAutoRewardGuardStaticMetadata` | Static Reward DTO | Partial | Unit Tested | Needs Verification | Guard metadata and projection lookup are exercised with synthetic XML. Serialization/default differences and full reward contents remain unverified against Java runtime. |
| `com.aionemu.gameserver.model.gameobjects.player.Player`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Player`; `PlayerQuestState` | Player State DTO | Partial | Unit Tested | Needs Verification | Player object id and quest state are read only. Threading/player-ordering, quest completion mutation, date/time completion fields, persistence, and serialization remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishSocketGuardedInputAssemblyPlanServiceTests.CreatePlan_AssemblesInputOnlyAfterJavaSelfTargetReportableGuard` | Target `0` and player-object target with reportable template and auto-reward action can assemble non-live input. | Source-reviewed Java `CM_DIALOG_SELECT.runImpl`; deterministic C# assertions. |
| `QuestFinishSocketGuardedInputAssemblyPlanServiceTests.CreatePlan_RejectsNpcTargetBeforeRewardProjectionLikeJavaBranchSplit` | Non-self target rejects before static metadata/input assembly, matching Java branch split toward NPC controller dispatch. | Source-reviewed Java target branch. |
| `QuestFinishSocketGuardedInputAssemblyPlanServiceTests.CreatePlan_RejectsMissingQuestTemplateBeforeInputAssembly` | Missing quest template rejects before input assembly. | Source-reviewed `DataManager.QUEST_DATA.getQuestById` null return branch. |
| `QuestFinishSocketGuardedInputAssemblyPlanServiceTests.CreatePlan_RejectsNonReportableQuestBeforeInputAssembly` | Non-reportable template rejects even with reward metadata. | Source-reviewed `QuestTemplate.isCanReport` guard. |
| `QuestFinishSocketGuardedInputAssemblyPlanServiceTests.CreatePlan_RejectsNonAutoRewardActionBeforeInputAssembly` | Normal reward dialog action does not assemble finish inputs through the guarded path. | Source-reviewed Java auto-reward switch. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` remains intentionally disabled for quest finish.
- NPC controller dialog dispatch, known-list/function checks, unsupported-action audits, and interaction-distance validation are not modeled by this guarded planner.
- Live item/non-item reward mutation, dynamic bonus handler dispatch, selected bonus RNG, packet ordering, persistence, rollback, threading/player-ordering, serialization, and completion date/time behavior remain disabled.
- Java JAXB/runtime comparison is still absent; tests are deterministic C# source-review parity checks.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live guarded socket input assembly planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production socket routing, NPC dispatch/known-list checks, target NPC/template resolution, live reward mutation, dynamic bonus handler execution, selected bonus RNG, persistence/rollback, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit tightens the staged quest-finish socket guard without enabling live execution.

## Next Recommended Unit Of Work

Compose the guarded socket input planner with `QuestFinishSocketOperationCompositionPlanService` in one non-live boundary planner/test. The next slice should prove Java guard rejection prevents operation composition, while planned guarded input can produce the existing non-live operation descriptors. Keep `GameServerConnection` disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Guarded input-to-operation composition | New service/tests | Medium | Recommended next; sequential if touching shared planner contracts. |
| B | NPC-target dialog branch audit | Read-only audit doc/test notes | Medium | Analyze unsupported action and interaction guards before production routing. |
| C | Dynamic bonus handler registry audit | Read-only audit doc | Medium | Needed before any live bonus execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add guarded input-to-operation composition | New service/test plus progress/handoff docs | `GameServerConnection.cs` |
| Agent A | NPC-target branch read-only audit | Read-only Java/C# inspection | All writes |

## Do Not Parallelize

- `GameServerConnection.cs`: production quest finish remains intentionally disabled.
- Quest finish planner contract files: shared staged surfaces; give one agent exclusive ownership.
- Phase 6 progress/handoff docs: orchestrator-owned.
