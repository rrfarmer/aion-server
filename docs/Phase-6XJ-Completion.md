# Phase 6XJ Completion - UOW-1122 Quest Finish Guarded Operation Composition

Date: May 26, 2026

## Unit Of Work

UOW-1122: `[Phase 6][UOW-1122] Compose guarded quest finish operation planning`

## Summary

UOW-1122 adds a non-live boundary planner that composes the guarded quest-finish socket input assembly layer with the existing socket operation composition layer. The new service first runs the Java-shaped `CM_DIALOG_SELECT` target/template/reportable/action guard, then assembles socket inputs, then composes the non-live operation descriptors only when all earlier layers are ready.

Guard rejection prevents operation composition, and diagnostic input plans remain `InputNotReady`. Production `GameServerConnection.HandleDialogSelectAsync` remains disconnected from quest-finish execution.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedOperationCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishSocketGuardedOperationCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XJ-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishSocketGuardedOperationCompositionPlanServiceTests\|QuestFinishSocketGuardedInputAssemblyPlanServiceTests\|QuestFinishSocketOperationCompositionPlanServiceTests\|QuestDialogAutoRewardGuardPlanServiceTests" --nologo` | Passed: 30 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,225 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1122

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestFinishSocketGuardedOperationCompositionPlanService`; `QuestFinishSocketGuardedInputAssemblyPlanService`; `QuestFinishSocketOperationCompositionPlanService` | Packet / Boundary Planner | Partial | Unit Tested | Partial Parity | The staged boundary preserves Java guard order before non-live operation composition. It is still not wired into `GameServerConnection.HandleDialogSelectAsync`, does not perform player-thread packet ordering, and does not dispatch NPC controller dialog branches. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishSocketGuardedOperationCompositionPlanService`; `QuestFinishOperationPlanService` | Finish Service / Operation Planner | Partial | Unit Tested | Needs Verification | Ready guarded input can produce non-live reward and quest-state operation descriptors. Live finish execution, packet sends, quest mutation, callback execution, persistence, rollback, and threading remain disabled. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; guarded operation composition input | Static Template DTO | Partial | Existing Unit Coverage | Partial Parity | `CanReport` and reward metadata gate staged operation composition. Full Java template/JAXB/script behavior remains unverified. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardProjectionLookupPlanService`; `QuestFinishOperationPlanService` | Reward Selection / Projection | Partial | Unit Tested | Needs Verification | Tests prove item and Kinah projection descriptors flow through the guarded staged boundary. Java `ItemService.addItem`, selected bonus RNG, dynamic bonus handlers, and live reward application remain unsupported. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `PlayerQuestState`; `QuestFinishStateMutationService` | Quest State DTO / Mutation Planner | Partial | Existing Unit Coverage | Needs Verification | The operation plan contains planned completion descriptors only. Completion date/time uses supplied `DateTimeOffset`; Java runtime timing and persistence are not compared. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardTemplateProjection`; `QuestFinishOperationDescriptor` | Static Reward DTO / Operation Descriptor | Partial | Unit Tested | Needs Verification | Synthetic reward XML composes through guarded operation planning. Serialization/default/JAXB differences remain unverified. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItemProjectionDescriptor`; future inventory mutation adapter | Reward Item DTO / Mutation | Partial | Unit Tested | Needs Verification | Item descriptors are present in operation plans but no object IDs, stack merge, inventory capacity, special inventory, or DB writes execute. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection`; future bonus runtime inputs | Bonus DTO / Dynamic Dependency | Partial | Existing Unit Coverage | Needs Verification | Diagnostic input does not compose to operation planning. Java reflection and dynamic bonus handler execution remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishSocketGuardedOperationCompositionPlanServiceTests.CreatePlan_ComposesGuardedSelfAutoRewardInputIntoNonLiveOperationPlan` | Valid self auto-reward request passes guard, assembles input, and produces non-live item, Kinah, and quest-state operation descriptors. | Source-reviewed Java `CM_DIALOG_SELECT.runImpl` -> `QuestService.finishQuest`; no Java runtime comparison. |
| `QuestFinishSocketGuardedOperationCompositionPlanServiceTests.CreatePlan_DoesNotComposeWhenJavaGuardRejectsNpcTarget` | Non-self/NPC target stops at guard rejection and does not create operation composition. | Source-reviewed Java target branch split. |
| `QuestFinishSocketGuardedOperationCompositionPlanServiceTests.CreatePlan_DoesNotComposeWhenInputAssemblyHasDiagnostics` | Guard-planned but diagnostic input, such as missing player class for class-selectable rewards, remains `InputNotReady`. | Conservative C# behavior pending Java runtime comparison and dynamic handler support. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` remains intentionally disabled for quest finish.
- NPC controller dialog dispatch, known-list/function checks, unsupported-action audits, and interaction-distance validation are not modeled by this boundary.
- Live item/non-item reward mutation, dynamic bonus handler dispatch, selected bonus RNG, packet ordering, persistence, rollback, threading/player-ordering, serialization, and completion date/time behavior remain disabled.
- The staged composition is deterministic C# evidence only; Java runtime comparison is still absent.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live guarded socket-to-operation composition planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production socket routing, NPC dispatch/known-list checks, live reward mutation, dynamic bonus handler execution, selected bonus RNG, persistence/rollback, packet-order validation, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit links the staged guarded boundary through operation descriptors without enabling live execution.

## Next Recommended Unit Of Work

Add a disabled production socket boundary regression proving `GameServerConnection.HandleDialogSelectAsync` still does not invoke the new guarded operation composition planner. Document the exact future call-site dependencies: static reward lookup, target NPC template resolution, callback/persistence plans, side-effect context, and packet ordering.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Disabled production boundary regression | Existing socket boundary test file or new test file | Medium | Recommended next; do not modify live routing. |
| B | NPC-target dialog branch audit | Read-only audit doc/test notes | Medium | Analyze Java unsupported-action and interaction guards. |
| C | Dynamic bonus handler registry audit | Read-only audit doc | Medium | Needed before live bonus execution. |

## Do Not Parallelize

- `GameServerConnection.cs`: production quest finish remains intentionally disabled.
- Quest finish planner contract files: shared staged surfaces; use exclusive ownership.
- Phase 6 progress/handoff docs: orchestrator-owned.
