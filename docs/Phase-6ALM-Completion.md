# Phase 6ALM Completion - Player Death Emotion Fanout Planner

Date: 2026-05-27
Unit of Work: UOW-1489
Status: Complete after validation.

## Scope

Add a non-live planner for Java `CreatureController.onDie` death observer notification, `SM_EMOTION(DIE)` broadcast intent, and known-list aggro cleanup ordering.

## Completed Work

- Re-audited Java `CreatureController.onDie`, `EmotionType.DIE`, Java `SM_EMOTION.writeImpl`, C# `EmotionType`, and C# `SmEmotion`.
- Added `PlayerDeathEmotionFanoutPlanService`.
- Modeled Java's self-death target id rule: `owner.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()`.
- Recorded emotion id `18`, emotion action id `0`, `SmEmotion` opcode metadata, and `PacketSendUtility.broadcastPacketAndReceive` intent.
- Recorded known-creature `stopHating(owner)` cleanup intents without mutating live aggro lists.
- Added focused tests for ordinary attacker id, self-death target id zero, and observer/broadcast/cleanup ordering.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerDeathEmotionFanoutPlanServiceTests|FullyQualifiedName~PlayerDeathWorkflowAdapterServiceTests|FullyQualifiedName~PlayerDeathWorkflowPlanServiceTests"`.
- Result: passed 11 tests.

## Migration Parity Table - UOW-1489

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerDeathEmotionFanoutPlanService` | Controller / Fanout Planner | Partial | Unit Tested | Partial Parity | Models the post-state side-effect order from `onDie`: death observer notification, `SM_EMOTION(DIE)` broadcast intent, then known-creature aggro `stopHating(owner)` intents. Movement abort, casting clear, effect removal, actual observer callbacks, packet broadcast, and live aggro-list mutation remain unsupported. |
| `com.aionemu.gameserver.model.EmotionType` | `Aion.GameServer.Model.EmotionType` / `PlayerDeathEmotionFanoutPlanService` | Enum / Packet Metadata | Partial | Unit Tested | Partial Parity | Confirms `DIE` maps to id `18` in planner metadata. Full enum parity and parser/golden coverage remain outside this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` / `PlayerDeathEmotionFanoutPlanService` | Packet / Fanout Metadata | Partial | Unit Tested | Needs Verification | Planner records `SmEmotion.PacketOpCode`, `EmotionType.Die`, action id `0`, and Java target-object-id rule. No byte comparison or broadcast-recipient verification was performed. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.PlayerDeathEmotionFanoutPlanService` | Utility / Fanout Boundary | Not Started | Unit Tested via planner metadata | Needs Verification | Java `broadcastPacketAndReceive(owner, packet)` is represented only as intent metadata. C# live recipient selection/order and self-receive behavior are not implemented here. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `Aion.GameServer.Services.PlayerDeathEmotionFanoutPlanService` | Service Dependency / Aggro Metadata | Not Started | Unit Tested via planner metadata | Needs Verification | Known-creature `creature.getAggroList().stopHating(owner)` is represented as per-creature cleanup intents only. Live known-list iteration, type filtering, and aggro mutation remain unsupported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_OrdinaryAttackerPlansDieEmotionTargetAndKnownCreatureCleanup` | Unit / planner | `CreatureController.onDie`, `SM_EMOTION`, `EmotionType` | Ordinary attacker id is used as the `SM_EMOTION(DIE)` target; known creature cleanup intents target the dead owner. | Deterministic Java source audit and C# metadata assertion. | No live packet send or aggro mutation. |
| `CreatePlan_SelfDeathUsesZeroEmotionTargetLikeJava` | Unit / planner | `CreatureController.onDie` | Self-death uses target object id `0` for `SM_EMOTION(DIE)`. | Deterministic Java branch assertion. | No byte serialization comparison. |
| `CreatePlan_OrdersObserverBeforeBroadcastBeforeKnownListCleanup` | Unit / planner | `CreatureController.onDie` | Observer notification is ordered before death emotion broadcast, which is ordered before known-list aggro cleanup. | Deterministic Java source-order assertion. | Does not execute observer callbacks, fanout, or known-list iteration. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Planner is non-live and not composed into the broader player death workflow yet.
- `SM_EMOTION(DIE)` packet bytes and broadcast recipient ordering remain unverified.
- Known-list creature filtering and live `AggroList.stopHating(owner)` mutation are represented as metadata only.
- Observer callback ordering is modeled but callbacks are not invoked.
- Java movement abort, casting clear, and effect removal side effects around this fanout remain planned elsewhere but not live.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live death emotion fanout/aggro cleanup planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live packet fanout, live known-list/aggro-list mutation, observer callback runtime, production death wiring, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerDeathEmotionFanoutPlanService` into `PlayerDeathWorkflowPlanService` / `PlayerDeathWorkflowAdapterService` metadata.
- Workflows should expose the concrete death emotion fanout and known-list cleanup plan at the Java `CreatureController.onDie` point.
- Keep the composition non-live and avoid packet broadcast or aggro-list mutation.

## Suggested Acceptance Criteria

- Workflow plan includes a nullable or explicit death emotion fanout plan when Java reaches `CreatureController.onDie`.
- Duel opponent early-return plans do not include a death emotion fanout plan.
- Adapter exposes the composed fanout plan while still reporting `SentPackets = false` and no live aggro mutation.
- Tests cover ordinary attacker id, self-death target id zero, and absence during duel early return.
- Re-run new fanout plan tests plus death workflow planner/adapter tests.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose death emotion fanout into workflow | death workflow planner/adapter/tests | Medium | Sequential because it touches recently added shared death workflow files. |
| B | Protection packet fanout bridge analysis or planner | read-only first, then new service/tests if scoped | Low-Medium | Keep separate from death workflow files. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared death workflow/state transition/scheduler/fanout fixtures if composing into workflow.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1489] Add death emotion fanout planner`.
- Files changed in UOW-1489:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerDeathEmotionFanoutPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerDeathEmotionFanoutPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALM-Completion.md`
