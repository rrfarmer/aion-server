# Phase 6ANE Completion - Protection Action Stop Trigger Audit

Date: 2026-05-27
Unit of Work: UOW-1533
Status: Complete after validation.

## Scope

Add non-live action-packet stop-trigger ordering audit coverage for `CM_ATTACK` and `CM_CASTSPELL`, capturing Java guard order before `stopProtectionActiveTask` while keeping production packet handlers disabled.

## Completed Work

- Updated `PlayerProtectionActiveTaskFirstActionStopTriggerAuditService`.
- Added opt-in detailed `CM_ATTACK` evaluation.
- Added opt-in detailed `CM_CASTSPELL` evaluation.
- Preserved default pending-caller catalog behavior when action-packet details are not requested.
- Kept remaining action packet callers as pending class-specific audits.
- No production C# packet handler wiring, combat/cast runtime change, scheduler callback execution, controller task-map owner, socket fanout, known-list mutation, AI move notification, inbound-damage guard, or protection bridge execution was wired.

## Java Source Notes

`CM_ATTACK.runImpl`:

```java
if (player.isDead())
	return;

if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();

VisibleObject obj = player.getKnownList().getObject(targetObjectId);
if (obj instanceof Creature) {
	player.getController().attackTarget((Creature) obj, time, false);
}
```

`CM_CASTSPELL.runImpl` protection-stop ordering:

```java
if (player.isDead()) {
	sendPacket(SM_SYSTEM_MESSAGE.STR_SKILL_CANT_CAST(ChatUtil.l10n(1400059)));
	return;
}

if (spellid == 0) {
	player.getController().cancelCurrentSkill(null);
	return;
}
if (DataManager.PET_SKILL_DATA.isPetOrderSkill(spellid) && (player.getSummon() == null || !player.getSummon().isPet())) {
	sendPacket(SM_SYSTEM_MESSAGE.STR_SKILL_NOT_NEED_PET());
	return;
}

SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(spellid);
if (template == null || template.isPassive())
	return;

if (player.isProtectionActive())
	player.getController().stopProtectionActiveTask();
player.getController().cancelUseItem();
```

Important parity notes:
- `CM_ATTACK` stop happens before known-list lookup and before `attackTarget`.
- `CM_CASTSPELL` stop happens after dead, zero-spell, invalid pet-order, and template/passive guards.
- `CM_CASTSPELL` stop happens before `cancelUseItem`.
- Future live C# packet handlers must preserve these guards and side-effect order.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests`.
- Result: passed 24 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 124 tests.

## Migration Parity Table - UOW-1533

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Attack Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models dead-player guard and protection stop before known-list target lookup/`attackTarget`. Production attack packet handler and combat runtime remain unwired. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFirstActionStopTriggerAuditService` | Packet Handler / Cast Stop Trigger Audit | Partial | Unit Tested Metadata | Partial Parity | Audit models dead, zero-spell, pet-order, template-missing, passive-template, and protection-active branches before `cancelUseItem`. Production cast packet handler and skill runtime remain unwired. |
| `com.aionemu.gameserver.controllers.PlayerController` | first-action stop trigger audit action-packet rows | Controller / Stop Protection Boundary | Partial | Unit Tested Metadata | Needs Verification | Audit records future calls into `stopProtectionActiveTask`, `attackTarget`, `cancelCurrentSkill`, `cancelUseItem`, and `useSkill` ordering only. Production controller task-map cancellation, visual/socket/AI side effects, threading, and scheduler interactions remain unwired. |
| `com.aionemu.gameserver.dataholders.DataManager` | first-action stop trigger audit cast precondition metadata | Static Data / Skill Lookup Boundary | Not Started | Unit Tested Metadata | Needs Verification | Audit documents `PET_SKILL_DATA` and `SKILL_DATA` guard positions. No C# skill data lookup or runtime template behavior was implemented. |
| `com.aionemu.gameserver.skillengine.model.SkillTemplate` | first-action stop trigger audit passive/missing-template metadata | Skill Template Boundary | Not Started | Unit Tested Metadata | Needs Verification | Audit documents missing/passive template skips before protection stop. No skill-template parity is claimed. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_CmAttackStopsAfterDeadGuardBeforeTargetLookup` | Unit / audit metadata | `CM_ATTACK.runImpl` | Active protection stops after dead guard and before known-list lookup. | Deterministic Java source-derived ordering assertion. | No live attack packet handler or Java runtime comparison. |
| `Create_CmAttackSkippedBranchesDoNotStop` | Unit / audit metadata | `CM_ATTACK.runImpl` | Dead-player and inactive-protection branches do not stop protection. | Deterministic Java source-derived branch assertion. | No live combat runtime. |
| `Create_CmCastSpellStopsAfterPreconditionGuardsBeforeCancelUseItem` | Unit / audit metadata | `CM_CASTSPELL.runImpl` | Active protection stops after reviewed preconditions and before `cancelUseItem`. | Deterministic Java source-derived ordering assertion. | No live cast packet handler or Java runtime comparison. |
| `Create_CmCastSpellSkippedBranchesDoNotStop` | Unit / audit metadata | `CM_CASTSPELL.runImpl` | Dead-player, zero-spell, invalid pet order, missing/passive template, and inactive-protection branches do not stop protection. | Deterministic Java source-derived branch assertion. | No live skill runtime. |
| Existing movement and pending-caller audit tests | Unit / regression metadata | `CM_MOVE`, `CM_MOVE_IN_AIR`, direct caller search | Existing movement/air movement and pending action caller behavior remains stable. | Regression coverage in 24 focused tests and 124 protection-slice tests. | Production wiring remains disabled. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- First-action stop trigger audit is metadata-only and is not consumed by production code.
- `CM_ATTACK` and `CM_CASTSPELL` parity is source-derived and unit-tested in C# metadata only; neither is verified against Java runtime packet traces.
- No production C# packet handler stop hook, attack runtime, cast runtime, skill template lookup, known-list lookup, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, or material-skill suppression integration exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, action-packet stop-trigger runtime ordering, and `ConcurrentHashMap` race behavior remain unverified by runtime comparison.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live first-action stop-trigger audit enhancement and focused test coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production packet handler stop hooks, production attack/cast runtime, production skill data/template runtime, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, inbound-damage guard, aggro/target/skill suppression, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/action-packet runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue the non-live action-packet stop-trigger audit with `CM_USE_ITEM`, `CM_SHOW_DIALOG`, and `CM_DIALOG_SELECT`.
- Capture Java guard ordering before `stopProtectionActiveTask`.
- Keep production packet handlers disabled.
- Preserve existing `CM_MOVE`, `CM_MOVE_IN_AIR`, `CM_ATTACK`, and `CM_CASTSPELL` detailed audit behavior.

## Suggested Acceptance Criteria

- Audit distinguishes `CM_USE_ITEM` from generic pending action rows:
  - active protection stop before item lookup/restriction checks, as verified from Java source.
- Audit distinguishes `CM_SHOW_DIALOG` from generic pending action rows:
  - active protection stop before trading/NPC validation, as verified from Java source.
- Audit distinguishes `CM_DIALOG_SELECT` from generic pending action rows:
  - active protection stop before trading/dialog validation, as verified from Java source.
- Tests cover active-protection stop and inactive-protection skip for each of the three packet classes.
- Remaining `CM_COMPOSITE_STONES` and `CM_EMOTION` callers stay listed as pending.
- Existing first-action audit, wiring intent, closure, delayed callback preview, scheduler callback, owner prototype, readiness aggregate, lifecycle cleanup, scheduled-handle, task-map simulation, adapter, audit, readiness, summary, task-operation, planner, side-effect, bridge, fanout, trace, executor, report, plan, and player-state tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `CM_USE_ITEM`/dialog action stop audit | first-action audit service/tests | Medium | Keep metadata-only and avoid production packet handlers. |
| B | Read-only Java ordering for `CM_COMPOSITE_STONES` and `CM_EMOTION` | Java packet sources | Low | Can run in parallel if docs remain orchestrator-owned. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add `CM_USE_ITEM`/`CM_SHOW_DIALOG`/`CM_DIALOG_SELECT` stop ordering audit and tests | first-action audit service/tests, progress/handoff docs | production packet handlers, inventory/dialog runtime, scheduler implementation |
| Explorer | Read-only remaining action caller ordering analysis | read-only Java source inspection only | all writes, shared docs, C# files |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same audit service/test pair.

## Do Not Parallelize

- Production packet handler wiring.
- Inventory/dialog runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1533] Add protection action stop audit`.
- Files changed in UOW-1533:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskFirstActionStopTriggerAuditService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANE-Completion.md`
- Latest prior commits:
  - `abadcda57 [Phase 6][UOW-1532] Deepen protection air-move stop audit`
  - `b1b58e464 [Phase 6][UOW-1531] Add protection first-action stop trigger audit`
  - `2d87c25fa [Phase 6][UOW-1530] Add protection wiring intent report`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
