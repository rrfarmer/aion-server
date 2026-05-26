# Phase 6XG Completion - UOW-1119 Quest Finish Socket Input Assembly Planner

Date: May 26, 2026

## Unit Of Work

UOW-1119: `[Phase 6][UOW-1119] Add quest finish socket input assembly planner`

## Summary

UOW-1119 adds a pure, non-live socket input assembly planner for future quest-finish reward routing. The planner accepts the parsed `CmDialogSelect`, active `Player`, `QuestFinishRewardProjectionLookupTable`, and optional target NPC template summary, then returns either a ready reward projection bundle or an explicit disabled-path diagnostic.

The planner mirrors the Java `CM_DIALOG_SELECT` auto-reward gate for action `108` and actions `110` through `124`, finds the player's quest state, enforces the `REWARD` status guard, applies existing reward-group correction, and looks up the reward projection with player class and target NPC context. It does not mutate player state, send packets, call `GameServerConnection`, persist data, invoke bonus handlers, or execute live reward side effects.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishSocketInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XG-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishSocketInputAssemblyPlanServiceTests\|QuestFinishRewardProjectionLookupPlanServiceTests\|GameServerConnectionQuestFinishDialogBoundaryTests" --nologo` | Passed: 11 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,213 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1119

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect`; `QuestFinishSocketInputAssemblyPlanService` | Packet / Input Adapter | Partial | Unit Tested | Needs Verification | C# planner recognizes Java auto-reward actions `SELECTED_QUEST_AUTO_REWARD = 108` and `SELECTED_QUEST_AUTO_REWARD1..15 = 110..124`, but production `GameServerConnection.HandleDialogSelectAsync` still does not call it. Java target object resolution, NPC known-list validation, and reportable-template checks remain outside this unit. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishSocketInputAssemblyPlanService`; `QuestFinishOperationPlanService` | Finish Service / Planner | Partial | Unit Tested | Needs Verification | This unit assembles future finish inputs and preserves disabled execution. Live reward mutation, quest completion mutation, packet sends, callbacks, persistence, rollback, and threading/player-order side effects remain unsupported. |
| `com.aionemu.gameserver.services.QuestService.validateAndFixRewardGroup` | `QuestFinishRewardPlanService.CorrectRewardGroup`; `QuestFinishSocketInputAssemblyPlanService` | Reward Group Guard | Partial | Existing Unit Coverage | Needs Verification | Planner passes the current quest state and discovered reward-group count into the existing correction service. Missing projection leaves null reward-group state unchanged because no reward count is available; Java runtime comparison is still absent. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardProjectionLookupPlanService`; `QuestFinishSocketInputAssemblyPlanService` | Reward Selection Planner | Partial | Unit Tested | Needs Verification | Planner forwards dialog action, extended reward index, complete count, corrected reward group, player class, and target NPC template context into projection lookup. Selected bonus RNG, bonus handler dispatch, and Java `ItemService.addItem` remain disabled. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `QuestFinishRewardProjectionLookupTable`; `StaticData.QuestFinishRewardProjections` | Static Data Repository | Partial | Existing Unit Coverage | Needs Verification | Planner consumes the staged lookup table but does not verify Java JAXB runtime identity. Missing quest templates are surfaced as non-live diagnostics. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardProjectionLookupEntry` | Static Template DTO | Partial | Existing Unit Coverage | Needs Verification | Reportable and reward-repeat metadata can reach the input bundle through lookup projections, but full Java template behavior, script hooks, and unsupported template fields remain unverified. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardTemplateProjection`; `QuestFinishRewardGroupProjection` | Static Reward DTO | Partial | Unit Tested | Needs Verification | Ready-path tests prove synthetic reward metadata reaches the assembled projection. Serialization/default differences from Java JAXB remain unverified. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection`; future bonus runtime inputs | Bonus DTO / Dynamic Dependency | Partial | Existing Unit Coverage | Needs Verification | Bonus metadata may be carried by projection, but dynamic handler dispatch, Java reflection behavior, and RNG/Chance selection remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.player.Player`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.Player`; `PlayerQuestState` | Player State DTO | Partial | Unit Tested | Needs Verification | Planner reads player class and quest state without mutation. Live quest state updates, completion timestamps/date handling, complete-count mutation, persistence, and serialization remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_PreparesRewardProjectionForReportableAutoRewardWithoutExecutingFinish` | Ready-path assembly for action `108`: reward group correction, projection lookup, dialog action, player class, and kinah metadata are present without live execution. | Source-reviewed Java `CM_DIALOG_SELECT.runImpl` and `QuestService.finishQuest`; no Java runtime comparison. |
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_RejectsNonAutoRewardDialogActionsBeforeProjectionLookup` | Non-auto-reward dialog actions do not assemble finish inputs. | Mirrors Java's action gate for reportable auto rewards; no runtime comparison. |
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_ReturnsMissingQuestStateBeforeRewardProjection` | Missing player quest state stops before reward projection. | Mirrors Java finish guard intent; no runtime comparison. |
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_ReturnsQuestStateNotRewardLikeJavaFinishGuard` | Non-`REWARD` quest state returns a diagnostic and does not project rewards. | Mirrors Java `QuestStatus.REWARD` finish guard; no runtime comparison. |
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_ReturnsMissingRewardProjectionWhenStaticLookupHasNoQuest` | Missing static lookup produces an explicit projection diagnostic and preserves non-mutating correction behavior. | Documents current C# staged behavior; Java runtime comparison pending. |
| `QuestFinishSocketInputAssemblyPlanServiceTests.CreatePlan_ReturnsProjectionDiagnosticsForMissingPlayerClass` | Class-selectable reward metadata with missing player class returns projection diagnostics without live execution. | Source-reviewed Java class-specific rewards; no runtime comparison. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not invoke this planner or execute quest finish.
- Java target-object resolution, reportable quest validation, target NPC template lookup, and NPC known-list/function checks remain missing from the production socket path.
- Live item/non-item reward mutation, dynamic bonus handler dispatch, Java RNG/Chance selection, packet ordering, persistence, rollback, threading/player-ordering, serialization, and date/time completion behavior remain disabled.
- Java reflection and script-handler behavior for quest bonuses is not ported.
- Static XML projection behavior is covered by deterministic tests but not by Java JAXB runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live socket input assembly planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production socket routing, target NPC/template resolution, live reward mutation, dynamic bonus handler execution, selected bonus RNG, persistence/rollback, packet-order validation, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit improves the staged quest-finish path without enabling live execution.

## Next Recommended Unit Of Work

Compose the non-live socket input assembly planner with `QuestFinishOperationPlanService` in a separate disabled service/test. The next slice should prove that a ready assembled input can become a complete non-live operation plan while `GameServerConnection` remains disabled and no packets, inventory mutations, quest state writes, persistence, callbacks, or bonus handlers execute.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live socket-to-operation composition | New service/tests | Medium | Recommended next; continue avoiding production socket invocation. |
| B | Target NPC/template resolution audit | Read-only Java/C# audit doc | Medium | Needed before production routing because Java behavior differs for player target, NPC target, and reportable quests. |
| C | Dynamic quest bonus handler registry audit | Read-only audit doc/tests | Medium | Needed before selected bonus/live bonus execution. |

## Do Not Parallelize

- `GameServerConnection.cs`: production quest finish remains intentionally disabled.
- `QuestFinishRewardPlanService.cs`: shared reward planning surface; avoid concurrent edits during composition work.
- `QuestFinishRewardProjectionLookupPlanService.cs`: recently changed and shared by this planner.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1119 adds `QuestFinishSocketInputAssemblyPlanService` only; it is not wired into production socket handling.
- Java auto-reward action constants are implemented inside the planner for actions `108` and `110..124`.
- Focused tests passed 11 tests before full validation.
- The next safest unit is disabled socket-to-operation composition, not live quest finish routing.
