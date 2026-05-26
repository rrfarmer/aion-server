# Phase 6VU Completion - UOW-1081 Dialog Auto-Reward Guard Planner

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VT-Completion.md`.

## Last Completed Unit

UOW-1081: `[Phase 6][UOW-1081] Add dialog auto-reward guard planner`

Recent commits before this unit:

- `ff899906f [Phase 6][UOW-1080] Audit quest-finish production call sites`
- `f5ca7b9c7 [Phase 6][UOW-1079] Add disabled custom reward composition regression`
- `dc13d9dd9 [Phase 6][UOW-1078] Add quest-finish custom reward session runtime adapter`

## Summary

UOW-1081 adds `QuestDialogAutoRewardGuardPlanService`, a non-live planner for Java's `CM_DIALOG_SELECT.runImpl` self/reportable quest auto-reward branch.

The planner mirrors Java's branch guards and action constants, then returns only disabled planning metadata. It does not call production quest finish, mutate quest state, mutate XP/level, execute custom rewards, allocate object ids, persist mail, or send packets.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A | Dialog auto-reward guard planner | `CM_DIALOG_SELECT`; `DialogAction`; `QuestTemplate.isCanReport`; `QuestService.finishQuest` | new service and test file | Service Port / Test Creation | No for selected unit | Medium | New production-path guard surface and docs are coupled; orchestrator-owned. |
| B | Mail list packet splitting tests | Mail list packet artifacts | packet test file only | Test Creation | Yes later | Low/Medium | Independent from quest-finish guard planning. |
| C | Additional timezone vectors | `ServerTime.ofEpochMilli` | assembler test file | Test Creation | Yes later | Low/Medium | Independent if assembler service is not edited. |
| D | Opt-in system-mail DB hardening | `MailDAO`; system-mail repository | DB integration tests | Parity Verification | Yes later | Medium | Environment-dependent. |
| E | Full account aggregate analysis | `Account`; `AccountService` | read-only | Java Analysis | Yes later | Low | Read-only support task before full account aggregate porting. |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Dialog auto-reward guard planner | `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`; `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogAutoRewardGuardPlanServiceTests.cs`; Phase 6 docs/handoff | `GameServerConnection`; live quest finish; custom reward execution; mail executor | Non-live planner, tests, docs, commit |

No sub-agents were used.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogAutoRewardGuardPlanServiceTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VU-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestDialogAutoRewardGuardPlanServiceTests" --nologo` | Passed: 15 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1925 |

## Migration Parity Table - UOW-1081

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.QuestDialogAutoRewardGuardPlanService` | Socket Guard Planner | Partial | Unit Tested | Partial Parity | The non-live planner mirrors Java's target/template/reportable/action guard order for the self auto-reward branch and returns a disabled planning intent. It is not wired into `GameServerConnection.HandleDialogSelectAsync`, does not call `QuestService.finishQuest`, and has no packet/runtime integration evidence. |
| `com.aionemu.gameserver.model.DialogAction` | `QuestDialogAutoRewardGuardPlanService.IsAutoRewardDialogAction` | Dialog Action Constants | Partial | Unit Tested | Partial Parity | Tests cover Java action `108` and range `110..124`, plus rejection of `109`, normal reward ids, and out-of-range values. C# does not yet centralize all Java dialog action constants or serialize enum names. |
| `com.aionemu.gameserver.model.templates.QuestTemplate.isCanReport` | `QuestDialogAutoRewardGuardInput.QuestTemplateCanReport` | Quest Template Flag Dependency | Partial | Unit Tested as explicit input | Needs Verification | The planner consumes an explicit flag because C# static quest data does not yet expose a full `can_report` quest template projection for production quest finish. XML extraction, JAXB default parity, and live static-data lookup remain unverified. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestDialogAutoRewardGuardPlan`; future `QuestFinishOperationPlanService` composition | Quest Finish Invocation Boundary | Partial | Unit Tested as non-live intent | Needs Verification | Planner records the Java `QuestService.finishQuest(new QuestEnv(null, player, questId, dialogActionId))` target but intentionally does not call quest finish or build reward projections. Reward mutation, work-item removal, quest update packet, callback dispatch, persistence, and threading remain disabled. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlan_PlansNonLiveQuestFinishIntentForSelfOrPlayerTargetAutoReward` | Target id zero or player id plus reportable template and auto-reward action yields a non-live quest-finish intent. | Source-reviewed from `CM_DIALOG_SELECT.runImpl` and `DialogAction` constants. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlan_RejectsDialogActionsOutsideJavaAutoRewardSwitch` | Rejects `109`, normal selected reward ids, and out-of-range actions. | Matches the Java switch case list. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlan_RejectsNonSelfTargetBeforeQuestTemplateLookupEquivalent` | Non-self target returns before template/reportable/action checks. | Mirrors Java branch order. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlan_RejectsMissingQuestTemplateBeforeReportableAndActionChecks` | Missing template returns before reportable/action checks. | Mirrors Java null template return. |
| `QuestDialogAutoRewardGuardPlanServiceTests.CreatePlan_RejectsNonReportableQuestBeforeAutoRewardActionCheck` | Non-reportable template returns before auto-reward action switch. | Mirrors `questTemplate.isCanReport()` guard. |
| `QuestDialogAutoRewardGuardPlanServiceTests.IsAutoRewardDialogAction_MatchesJavaSwitchConstants` | Matches action `108`, rejects `109`, accepts `110..124`, rejects `125`. | Deterministic constant coverage. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- C# static quest data still does not expose full Java `QuestTemplate.can_report` and reward metadata for production quest finish.
- The guard planner does not build `QuestFinishRewardTemplateProjection`, `QuestFinishRewardSideEffectContext`, or custom reward runtime options.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution and per-player packet/persistence ordering remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 non-live guard planner and 1 dialog-action constant slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 5 categories: production socket integration, static quest `can_report`/reward projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a static-data projection prerequisite for Java `QuestTemplate.can_report` and reward metadata, or compose the new non-live dialog auto-reward guard intent with explicit mock quest-finish projections. Keep production quest-finish/custom reward execution disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Mail list packet splitting tests | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` or new packet test file | Low/Medium | Independent from quest-finish guard planning. |
| B | Additional date/time conversion vectors | `QuestFinishCustomRewardRuntimeInputAssemblerServiceTests.cs` | Low/Medium | Avoid named-zone assumptions unless cross-platform behavior is verified. |
| C | Live DB run/hardening of system-mail opt-in integration suite | no code unless failures are isolated | Medium | Requires `AION_GAMESERVER_DB_INTEGRATION=1` and MySQL. |
| D | Read-only Java account/session model analysis for full account aggregate parity | read-only | Low | Useful before porting account time/toll/warehouse state. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Agent A | Mail list packet splitting tests | packet test file only | production code; docs |
| Agent B | Read-only full account aggregate analysis | read-only | all writes |
| Orchestrator | Static quest can-report/reward projection or guard-plan composition | exact new service/test files selected after discovery plus docs | live quest finish; broad static-data loader rewrites; custom reward execution; mail executor |

## Do Not Parallelize

- `GameServerConnection`, production quest-finish wiring, and static quest-data projection if the next unit moves toward live socket context.
- `QuestFinishRewardPlanService`, `QuestFinishOperationPlanService`, and new guard planner if composing the intent into operation planning.
- Phase 6 progress and handoff docs: orchestrator-owned only.

## Context For Next Session

- `docs/commit-conventions.md` is still missing; use the orchestration commit format.
- Keep Java as source of truth and leave live custom reward execution disabled.
- New code surface: `QuestDialogAutoRewardGuardPlanService`.
- The newest audit is `docs/QuestFinishProductionCallSite-Audit.md`, now updated with UOW-1081 state.
- A safe next implementation is either a static-data projection prerequisite for `QuestTemplate.can_report` plus reward metadata, or a non-live composition test that feeds the guard planner's planned intent into existing quest-finish operation planning with explicit mock projections.
