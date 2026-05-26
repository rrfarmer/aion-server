# Phase 6WW Completion - UOW-1109 Quest Bonus Finish Input Assembly Planner

Date: May 26, 2026

## Unit Of Work

UOW-1109: `[Phase 6][UOW-1109] Add quest bonus finish input assembly planner`

## Summary

UOW-1109 adds a disabled `QuestFinishBonusRewardInputAssemblyPlanService`.

The planner accepts explicit quest-finish inputs, runtime static data, optional handler quest states, and optional loaded handler ids, then returns either a complete `QuestBonusRewardPlanningInput` or deterministic missing-input diagnostics. It does not call `QuestBonusRewardPlanningInputAdapterService`, does not add operation-plan descriptors, and does not enable production quest-finish rewards.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishBonusRewardInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishBonusRewardInputAssemblyPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WW-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishBonusRewardInputAssemblyPlanServiceTests\|QuestBonusRewardPlanningInputAdapterServiceTests\|QuestBonusStaticDataBridgeTests" --nologo` | Passed: 11 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,194 tests. |

## Migration Parity Table - UOW-1109

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishBonusRewardInputAssemblyPlanService` | Service / Input Assembly Planner | Partial | Unit Tested | Needs Verification | C# now assembles explicit non-live bonus planning input or missing diagnostics. It does not invoke handlers, `BonusService`, RNG, or live reward mutation. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | future caller supplying `QuestFinishBonusRewardInputAssemblyPlan` | Service / Finish Flow Dependency | Partial | Unit Tested | Needs Verification | Assembly is available as a separate disabled planner. It is not composed into `QuestFinishOperationPlanService` or production `CM_DIALOG_SELECT`. |
| `com.aionemu.gameserver.services.reward.BonusService.getQuestBonus` | `QuestBonusRewardPlanningInputAdapterService` input envelope from assembly planner | Reward Service Dependency | Partial | Unit Tested | Needs Verification | Planner produces the adapter input shape but deliberately does not call the adapter or select candidate rewards. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `StaticData.ItemTemplates` passed through `QuestFinishBonusRewardInputAssemblyRequest.ItemTemplates` | Static Data Dependency | Partial | Unit Tested | Needs Verification | Runtime item templates can now be carried explicitly into the staged input envelope. Production caller still must source them. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_GROUPS_DATA` | `StaticData.QuestBonusItemGroups` passed through `QuestFinishBonusRewardInputAssemblyRequest.ItemGroups` | Static Data Dependency | Partial | Unit Tested | Needs Verification | Supported quest bonus groups can now be carried explicitly into the staged input envelope. Broad Java `ItemGroupsData` parity remains out of scope. |
| `com.aionemu.gameserver.model.gameobjects.player.Player`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Player`; `PlayerQuestState`; `QuestBonusRewardPlanningInput` | Runtime Player / Quest State Inputs | Partial | Unit Tested | Needs Verification | Planner uses `Player.Race`, current quest state, and optional handler quest states. Java mutable quest-state timing and threading/order remain unverified. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onBonusApplyEvent` | `BonusHandlerQuestStates`; `LoadedBonusHandlerQuestIds` in assembled adapter input | Dynamic Handler Dependency | Partial | Unit Tested | Needs Verification | Planner preserves explicit handler quest-state ids and loaded handler ids for later adapter use. Production handler registry/reflection and exception behavior are still missing. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishBonusRewardInputAssemblyPlanServiceTests.CreatePlan_AssemblesCompleteSupportedBonusInputWithoutInvokingAdapter` | Complete supported bonus inputs produce a non-live adapter input with player race, static data, handler states, and loaded ids. | Source-reviewed Java `QuestService.getRewardItems`; no Java runtime comparison. |
| `QuestFinishBonusRewardInputAssemblyPlanServiceTests.CreatePlan_ReportsMissingSupportedBonusStaticDataBeforeInputCreation` | Supported Java `BonusService` bonus types require item templates and item groups before assembly succeeds. | Mirrors existing adapter missing-input guard; no Java runtime comparison. |
| `QuestFinishBonusRewardInputAssemblyPlanServiceTests.CreatePlan_AllowsMovieNoOpWithoutStaticBonusData` | MOVIE no-op bonus input can be assembled without item static data. | Source-reviewed Java `BonusService` MOVIE no-op branch. |
| `QuestFinishBonusRewardInputAssemblyPlanServiceTests.CreatePlan_UsesExplicitHandlerQuestStatesWhenCurrentQuestStateIsMissing` | Explicit handler quest states can satisfy the adapter quest-state dependency without a current quest state. | Source-reviewed Java handler-state lookup behavior; no dynamic handler registry comparison. |

## Remaining Risks

- The new planner is not composed into `QuestFinishOperationPlanService`.
- Production `CM_DIALOG_SELECT` still does not route quest-finish auto-reward actions into staged finish planning.
- Production caller input sourcing for reward projection, static data, full handler states, and loaded handler ids remains unimplemented.
- Dynamic handler dispatch, reflection/registration order, handler exception handling, Java RNG/Chance selection, selected reward materialization, live inventory mutation, packet ordering, persistence, rollback, threading/player-ordering, serialization, date/time ordering, and Java runtime comparison remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 partial disabled input-assembly planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production finish planner composition, production socket wiring, production reward-projection lookup, loaded-handler registry, dynamic handler dispatch, Java RNG/Chance selection, live reward mutation/persistence, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Compose the disabled input-assembly plan into `QuestFinishOperationPlanService` as an optional non-live descriptor supplied by tests/callers. Keep the production socket path, adapter invocation, RNG selection, selected item creation, and live rewards disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Optional non-live bonus input-assembly operation descriptor | `QuestFinishOperationPlanService.cs`, tests | Medium | Recommended next unit; shared planner, so own exclusively. |
| B | Java handler exception/failure-ordering audit | Read-only Java/C# audit doc | Low | Can run in parallel only if no planner files are edited. |
| C | Production caller-input source audit for reward projections | Read-only or audit doc | Low/Medium | Useful before socket wiring. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Compose optional non-live descriptor | `QuestFinishOperationPlanService.cs`, focused tests, docs | Production socket wiring and adapter invocation |
| Explorer A | Audit Java handler exceptions | Read-only Java/C# inspection or separate audit doc | `QuestFinishOperationPlanService.cs`, assembly planner files |

## Do Not Parallelize

- `QuestFinishOperationPlanService.cs`: shared operation planner.
- `QuestFinishBonusRewardInputAssemblyPlanService.cs`: new assembly contract.
- `QuestBonusRewardPlanningInputAdapterService.cs`: adapter contract should remain stable.
- `GameServerConnection.cs`: production socket path remains disabled.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1109 added a disabled input-assembly planner and tests.
- The planner returns an adapter input, but it does not invoke the adapter.
- `QuestFinishOperationPlanService` does not yet include a bonus input-assembly descriptor.
- The next safe step is optional descriptor composition, not production wiring.
