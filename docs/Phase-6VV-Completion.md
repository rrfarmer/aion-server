# Phase 6VV Completion - UOW-1082 Dialog Guard Operation Composition

Date: May 26, 2026

## Current Phase

Phase 6 remains active. Java is the source of truth. This handoff continues from `docs/Phase-6VU-Completion.md`.

## Last Completed Unit

UOW-1082: `[Phase 6][UOW-1082] Add dialog guard operation composition regression`

Recent commits before this unit:

- `f49b57143 [Phase 6][UOW-1081] Add dialog auto-reward guard planner`
- `ff899906f [Phase 6][UOW-1080] Audit quest-finish production call sites`
- `f5ca7b9c7 [Phase 6][UOW-1079] Add disabled custom reward composition regression`

## Summary

UOW-1082 adds a non-live composition regression proving the new dialog auto-reward guard intent can feed existing quest-finish operation planning when explicit mock reward projection data is supplied.

The test creates a planned `QuestDialogAutoRewardGuardPlanService` intent for Java dialog action `108`, passes that dialog action into `QuestFinishOperationPlanService.CreatePlan`, and verifies all resulting operation descriptors remain non-live.

No production socket path, static-data lookup, reward mutation, XP mutation, custom reward execution, mail execution, object-id allocation, or packet send was enabled.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A | Guard intent plus explicit projection composition | `CM_DIALOG_SELECT`; `QuestService.finishQuest`; `QuestService.giveReward` | existing operation-plan test file | Test Creation | No for selected unit | Medium | Uses shared quest-finish operation-plan tests and docs. |
| B | Static quest can-report/reward projection prerequisite | `QuestTemplate`; quest XML/static data | new extractor/service/test files | Service Port / Test Creation | Maybe later | Medium | Needs careful static-data shape review before production use. |
| C | Mail list packet splitting tests | Mail list packet artifacts | packet test file only | Test Creation | Yes later | Low/Medium | Independent from quest-finish guard planning. |
| D | Additional timezone vectors | `ServerTime.ofEpochMilli` | assembler test file | Test Creation | Yes later | Low/Medium | Independent if assembler service is not edited. |
| E | Opt-in system-mail DB hardening | `MailDAO`; system-mail repository | DB integration tests | Parity Verification | Yes later | Medium | Environment-dependent. |

File ownership map:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Guard-to-operation-plan composition regression | `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishOperationPlanServiceTests.cs`; Phase 6 docs/handoff | production code; `GameServerConnection`; live quest finish; static-data loader rewrites; custom reward execution; mail executor | Test, docs, commit |

No sub-agents were used.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishOperationPlanServiceTests.cs`
- `docs/QuestFinishProductionCallSite-Audit.md`
- `docs/QuestFinishRuntimeInput-Audit.md`
- `docs/QuestXpReward-Audit.md`
- `docs/QuestRewardSideEffects-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6VV-Completion.md`

## Validation

| Command | Result |
| --- | --- |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishOperationPlanServiceTests" --nologo` | Passed: 23 |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 1926 |

## Migration Parity Table - UOW-1082

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `QuestDialogAutoRewardGuardPlanService`; `QuestFinishOperationPlanServiceTests` composition regression | Socket Guard / Test Composition | Partial | Unit Tested | Partial Parity | Test proves a planned self auto-reward guard intent can feed non-live quest-finish operation planning with explicit projections. Production `GameServerConnection` is still not wired and no packet/runtime integration is performed. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishOperationPlanService.CreatePlan` | Quest Finish Planner | Partial | Unit Tested | Partial Parity | Existing planner composes metadata and planned quest-state completion from explicit inputs. This unit verifies guard-to-planner composition remains non-live; reward mutation, work-item removal execution, packet sends, callbacks, persistence, and threading remain disabled. |
| `com.aionemu.gameserver.services.QuestService.giveReward` | `QuestFinishRewardTemplateProjection`; `QuestFinishOperationDescriptor` metadata | Reward Metadata Composition | Partial | Unit Tested | Partial Parity | Explicit mock non-item XP projection produces non-live metadata and coarse placeholder. Live reward side effects, serialization/packet ordering, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | Future static-data projection; explicit mock `QuestFinishRewardTemplateProjection` in test | Static Quest Template Dependency | Partial | Unit Tested as explicit mock | Needs Verification | Test bypasses live static-data extraction. `can_report`, reward groups, extended rewards, work items, and reward XML/JAXB defaults still need a real C# projection prerequisite before production socket use. |

## Tests Added

| Test | Validates | Java Comparison |
| --- | --- | --- |
| `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesDialogAutoRewardGuardIntentWithExplicitProjectionWithoutLiveSideEffects` | A planned auto-reward guard intent can pass its dialog action into explicit quest-finish projection planning while all descriptors remain non-live. | Source-reviewed from `CM_DIALOG_SELECT.runImpl`, `QuestService.finishQuest`, and `QuestService.giveReward`; no Java runtime comparison. |

## Remaining Risks

- Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
- C# static quest data still does not expose full Java `QuestTemplate.can_report` and reward metadata for production quest finish.
- The composition test uses explicit mock projection data and does not read live XML/static data.
- Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
- Custom reward receipt/mail execution, packet ordering, and persistence remain gated and unverified.
- Java runtime comparison remains blocked locally by Java 8 and missing Maven.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 0 new production artifacts; 1 guard-to-operation-plan composition regression
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 5 categories: production socket integration, static quest projection, live quest finish, live XP mutation, and custom reward/mail execution
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit of Work

Add a static-data projection prerequisite for Java `QuestTemplate.can_report` and reward metadata so the C# guard planner can eventually consume real quest data. Keep production quest-finish/custom reward execution disabled.

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
| Orchestrator | Static quest can-report/reward projection prerequisite | exact new extractor/service/test files selected after discovery plus docs | live quest finish; broad static-data loader rewrites unless scoped; custom reward execution; mail executor |

## Do Not Parallelize

- `GameServerConnection`, production quest-finish wiring, and static quest-data projection if the next unit moves toward live socket context.
- `QuestFinishRewardPlanService`, `QuestFinishOperationPlanService`, and dialog guard planner if composing real static quest data into operation planning.
- Phase 6 progress and handoff docs: orchestrator-owned only.

## Context For Next Session

- `docs/commit-conventions.md` is still missing; use the orchestration commit format.
- Keep Java as source of truth and leave live custom reward execution disabled.
- Newest code surfaces:
  - `QuestDialogAutoRewardGuardPlanService`
  - `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesDialogAutoRewardGuardIntentWithExplicitProjectionWithoutLiveSideEffects`
- The newest audit is `docs/QuestFinishProductionCallSite-Audit.md`, now updated with UOW-1082 state.
- A safe next implementation is a static-data projection prerequisite for `QuestTemplate.can_report` plus reward metadata, likely as a narrow extractor/projection with tests before any socket integration.
