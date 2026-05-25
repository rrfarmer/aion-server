# Phase 6TG Completion - UOW-1015 Staged Quest Finish Operation Plan

## Scope

UOW-1015 adds a staged quest-finish operation plan that composes the existing quest-state mutation helper and NPC faction completion helper into Java-ordered descriptors.

The plan remains non-live: it does not grant rewards, remove work items, send packets, dispatch quest callbacks, trigger live nearby refresh, or write DAO state.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Staged quest-finish operation plan | `QuestService.finishQuest`, `SM_QUEST_ACTION`, `QuestEngine.onQuestCompleted`, `NpcFactions.completeQuest` | `QuestFinishOperationPlanService.cs`, focused tests, docs | Service Port | Selected sequential | Medium | Composes shared quest-state and NPC faction helpers; one owner needed. |
| B | Quest action packet port | `SM_QUEST_ACTION` | future packet/tests | Packet Port | No with A | Medium | Depends on operation-plan descriptors and extra-category suppression policy. |
| C | Quest/faction persistence contracts | `PlayerQuestListDAO`, `PlayerNpcFactionsDAO` | future repositories/tests | Repository Port | No with A | High | Needs operation plan first. |
| D | Reward mutation audit/plan | reward helpers in `QuestService` | docs/read-only or future service | Java Analysis | Yes read-only | Medium | Useful but lower priority than composing already staged state helpers. |

Selected batch: local-only A. No sub-agent was spawned because implementation touched shared quest/faction operation boundaries.

## Java Breadcrumbs

- `QuestService.finishQuest` computes rewards and removes work items before quest-state completion.
- After state mutation, Java sends `SM_QUEST_ACTION(ActionType.UPDATE, qs)`.
- `QuestEngine.onQuestCompleted` runs after the update packet.
- `NpcFactions.completeQuest` runs after quest-completed callbacks when `template.getNpcFactionId() != 0`.
- `PlayerController.updateNearbyQuests` runs after NPC faction completion.
- Quest and NPC faction persistence are deferred to `PlayerService.storePlayer`.

## Implementation

- Added `QuestFinishOperationPlanService.CreatePlan`.
- Added `QuestFinishOperationAction`, `QuestFinishOperationDescriptor`, and `QuestFinishOperationPlan`.
- The plan composes:
  - `QuestFinishStateMutationService.ApplyRewardCompletion`
  - optional `PlayerNpcFactionsSnapshot.CompleteActiveQuest`
  - ordered descriptors for future packet, callback, nearby refresh, and deferred persistence work
- Reward and work-item behavior are represented as explicit non-live placeholders.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestFinishOperationPlanServiceTests` | Passed, 6 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1746 tests |

## Migration Parity Table - UOW-1015

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService.CreatePlan` | Service / Operation Plan | Partial | Unit Tested | Partial Parity | Stages Java ordering around completion state, packet descriptor, callback descriptor, optional NPC faction completion, nearby-refresh descriptor, and deferred persistence descriptors. Rewards, work-item removal, live sends, callback runtime, DAO writes, and production refresh remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` | `QuestFinishOperationAction.QuestUpdatePacket` descriptor | Packet Dependency | Not Started | Unit Tested as descriptor order only | Needs Verification | Descriptor preserves Java position after state mutation and before callbacks. No packet bytes or extra-category suppression are implemented in this unit. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onQuestCompleted` | `QuestFinishOperationAction.QuestCompletedCallback` descriptor | Callback Dependency | Not Started | Unit Tested as descriptor order only | Needs Verification | Descriptor preserves Java position after update packet and before NPC faction completion. No quest handler runtime exists yet. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions.completeQuest` | `PlayerNpcFactionsSnapshot.CompleteActiveQuest` via `QuestFinishOperationPlanService` | Player State / Faction Completion Helper | Partial | Unit Tested | Partial Parity | Operation plan invokes staged faction completion only when template NPC faction id is nonzero. Mentor title side effects, daily assignment, persistence flags, and DAO writes remain missing. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.store` | `QuestFinishOperationAction.DeferredQuestPersistence` descriptor | Repository Dependency | Not Started | Unit Tested as descriptor order only | Needs Verification | Descriptor documents deferred persistence but does not write `player_quests`. Java delete/insert/update commit behavior remains unported. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO.storeNpcFactions` | `QuestFinishOperationAction.DeferredNpcFactionPersistence` descriptor | Repository Dependency | Not Started | Unit Tested as descriptor order only | Needs Verification | Descriptor is emitted only for NPC faction quests. No C# faction write path or persistent-state flag behavior exists. |

## Tests Added

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesJavaQuestFinishOrderingWithoutLiveSideEffects` | Unit | Non-faction completion orders placeholders, state mutation, update packet descriptor, callback descriptor, nearby refresh, and deferred quest persistence; all descriptors are non-live. | Source-reviewed Java `QuestService.finishQuest` ordering. |
| `QuestFinishOperationPlanServiceTests.CreatePlan_ComposesNpcFactionCompletionAfterCallbackAndBeforeNearbyRefresh` | Unit | NPC faction completion descriptor and state update occur after callback descriptor and before nearby refresh. | Source-reviewed Java `QuestService.finishQuest` and `NpcFactions.completeQuest`. |
| `QuestFinishOperationPlanServiceTests.CreatePlan_KeepsNpcFactionNoOpDescriptorWhenJavaWouldReturnFromMissingActiveSlot` | Unit | NPC faction quest still records the completion step when the active slot is missing, while the staged faction helper no-ops like Java. | Source-reviewed Java null active-slot return. |
| `QuestFinishOperationPlanServiceTests.CreatePlan_ReturnsNoDescriptorsWhenQuestFinishGuardFails` | Unit | Non-`REWARD` quest states produce no operation descriptors. | Source-reviewed Java guard before side effects. |
| `QuestFinishOperationPlanServiceTests.CreatePlan_ReturnsNoDescriptorsWhenQuestStateIsMissing` | Unit | Missing quest state produces no operation descriptors. | Source-reviewed Java null guard before side effects. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Reward calculation, inventory mutation, and work-item removal are placeholders only.
- `SM_QUEST_ACTION` packet bytes and extra-category suppression are not implemented.
- Quest callback dispatch has no C# runtime equivalent yet.
- Nearby refresh is a descriptor only and does not send `SM_NEARBY_QUESTS`.
- Quest and NPC faction persistence remain deferred descriptors with no write repository.
- C# immutable state snapshots differ from Java in-place mutation and persistent-state flags.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 staged operation-plan artifact in this unit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Port a non-sending `SM_QUEST_ACTION` update packet serializer or add a quest-finish reward/work-item audit. The packet slice should verify the Java update packet body shape and extra-category suppression without wiring live sends.

## Next Unit Handoff

Start with this file, `docs/QuestFinishOrdering-Audit.md`, and `docs/PHASE-6-PROGRESS.md` Session 1015.

Recommended next slice:

1. Inspect existing C# game-server packet serializer patterns.
2. Port a narrow `SM_QUEST_ACTION(ActionType.UPDATE, qs)` serializer for operation-plan use.
3. Cover Java packet fields: action id, quest id, status value, zero byte, packed vars/flags, trailing zero short.
4. Decide how to represent Java extra-category suppression before live sends.
5. Keep live packet sending, callbacks, rewards, DAO writes, and nearby refresh disabled.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Packet serializer slice | future quest action packet + packet tests | Medium | Sequential owner should avoid packet utility conflicts. |
| B | Reward/work-item Java audit | docs-only | Low | Can run in parallel with packet work if no docs overlap. |
| C | Persistence contract analysis | docs-only | Medium | Read-only analysis of DAO write semantics; avoid repository code until packet/operation plan stabilizes. |

## Do Not Parallelize

- `QuestFinishOperationPlanService.cs`: shared operation boundary.
- Quest packet base classes: shared serialization infrastructure.
- `PHASE-6-PROGRESS.md` and completion docs: Orchestrator-owned docs only.
