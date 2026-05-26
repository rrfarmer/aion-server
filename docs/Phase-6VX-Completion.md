# Phase 6VX Completion - UOW-1084 Quest Guard Static Summary Adapter

Date: May 26, 2026

## Unit Of Work

UOW-1084: `[Phase 6][UOW-1084] Feed quest summary into auto reward guard`

## Starting Point

- Continue from `docs/Phase-6VW-Completion.md`.
- UOW-1083 added static summary fields for Java `QuestTemplate.can_report`, `reward_repeat_count`, and direct reward/work child presence.
- Production quest-finish, reward mutation, XP mutation, custom reward execution, mail execution, persistence, and packet sends remain disabled.

## Summary

UOW-1084 adds a non-live adapter from `NearbyQuestTemplateSummary?` into `QuestDialogAutoRewardGuardPlanService`.

The adapter derives template existence and reportability from the real C# static summary projection, carries coarse reward/work metadata on the guard plan, and preserves Java `CM_DIALOG_SELECT` guard order. It does not wire production socket handling or build a full quest-finish reward projection.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogAutoRewardGuardPlanServiceTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VX-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogAutoRewardGuardPlanServiceTests" --nologo` | Passed: 18 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1,929 tests. |

## Migration Parity Table - UOW-1084

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.QuestDialogAutoRewardGuardPlanService.CreatePlanFromTemplateSummary` | Socket Guard Adapter | Partial | Unit Tested | Partial Parity | Adapter derives the self/reportable auto-reward guard inputs from real C# static summary data and preserves Java guard order. It is not wired into `GameServerConnection.HandleDialogSelectAsync`, does not dispatch NPC controller logic, and does not send packets. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary` consumed by `QuestDialogAutoRewardGuardTemplateInput` | Static Quest Template Dependency | Partial | Unit Tested | Partial Parity | `CanReport` now feeds the guard path and coarse reward/work metadata can travel on the plan. Full Java `QuestTemplate` reward contents, target NPC context, JAXB defaults, serialization, and production loader behavior remain incomplete. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestDialogAutoRewardGuardStaticMetadata` | Reward Template Dependency | Partial | Unit Tested as presence only | Needs Verification | Metadata carries only `RewardRepeatCount` plus direct child presence for rewards/extended/bonus. It does not parse XP, kinah, AP/DP/GP, title, cube, warehouse, item reward lists, selectable rewards, precision/rounding, or reward groups. |
| `com.aionemu.gameserver.model.templates.quest.QuestWorkItems` | `QuestDialogAutoRewardGuardStaticMetadata.HasQuestWorkItems` | Work Item Dependency | Partial | Unit Tested as presence only | Needs Verification | Only presence is carried. Work item ids/counts, inventory removal, failure ordering, persistence, and packet behavior remain unported. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | Future operation planner input from `QuestDialogAutoRewardGuardPlan` | Quest Finish Invocation Boundary | Partial | Unit Tested as non-live intent | Needs Verification | Guard plan remains an intent only. It does not build `QuestFinishRewardTemplateProjection`, call `QuestFinishOperationPlanService`, mutate quest state, invoke callbacks, persist, or run on a player-thread/connection path. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlanFromTemplateSummary_UsesRealCanReportAndStaticRewardMetadataWithoutLiveSideEffects` | Reportable summary data produces a non-live plan carrying coarse reward/work metadata. | Source-reviewed from `CM_DIALOG_SELECT.runImpl`, `QuestTemplate.isCanReport`, and current Java XML shape; no Java runtime comparison. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlanFromTemplateSummary_RejectsMissingAndNonReportableTemplatesInJavaOrder` | Missing template and non-reportable template return before action checks. | Source-reviewed Java guard order plus deterministic C# assertions. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlanFromTemplateSummary_RejectsNonSelfTargetBeforeUsingStaticMetadata` | Non-self target returns before static metadata is attached. | Source-reviewed Java branch order. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- Full `QuestFinishRewardTemplateProjection` construction from Java static quest rewards remains missing.
- Reward/work metadata is presence-only and cannot drive live execution.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution, packet ordering, and persistence remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 partial non-live guard adapter and 1 coarse metadata carrier
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/partial categories: production socket integration, full static reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Begin full `QuestFinishRewardTemplateProjection` extraction from Java static quest rewards, starting with non-item reward fields only and keeping operation-planner composition non-live.

Keep production quest-finish/custom reward execution disabled.
