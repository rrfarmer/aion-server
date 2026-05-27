# Phase 6ALC Completion - Player-Owned Aggro List Boundary

Date: 2026-05-27
Unit of Work: UOW-1479
Status: Complete after validation.

## Scope

Introduce an executable C# player-owned aggro list boundary so future kisk revive cleanup can clear live player aggro instead of only exposing disabled plans.

## Completed Work

- Re-audited Java `AggroList`, `PlayerAggroList`, `AggroInfo`, `Player.createAggroList`, and `PlayerReviveService.revive`.
- Added `PlayerOwnedAggroList` with known-list-only awareness, damage/hate snapshots, all-entry clear, and represented hate-reduction cancellation state.
- Exposed the list from `Player.AggroList`.
- Updated revive cleanup planning to preserve live/non-live metadata.
- Updated `PlayerReviveCleanupAdapterService` so live mutation stays blocked unless a concrete `PlayerOwnedAggroList` is supplied, then clears that list in the Java revive cleanup boundary.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerOwnedAggroListTests|FullyQualifiedName~PlayerAggroCleanupPlanServiceTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests|FullyQualifiedName~PlayerReviveCleanupPlanServiceTests"`.
- Result: passed 11 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 10 tests.

## Migration Parity Table - UOW-1479

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `Aion.GameServer.Model.GameObjects.PlayerOwnedAggroList` | Model / Aggro List | Partial | Unit Tested | Partial Parity | C# now owns executable player aggro entries with Java known-list-only awareness, deterministic snapshots, and clear. Missing Java live `Creature` references, known-list object lookup, target selection, stream valid target filtering, random target selection, geo visibility, and full thread scheduling. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `Aion.GameServer.Model.GameObjects.PlayerOwnedAggroList` / `PlayerAggroCleanupPlanService` | Base Aggro List | Partial | Unit Tested | Needs Verification | C# models clear-all plus hate-reduction-task cancellation state for player-owned aggro. Ordinary creature awareness, damage/hate formulas, master damage transfer, final damage list, `ConcurrentHashMap` semantics, and scheduled hate decay remain unsupported. |
| `com.aionemu.gameserver.controllers.attack.AggroInfo` | `PlayerAggroEntrySnapshot` / `PlayerOwnedAggroList` | DTO / Entry State | Partial | Unit Tested | Partial Parity | C# accumulates positive damage and clamps hate to at least 1 for known attackers. Java `lastInteractionTime`, `hateReduceCount`, and timed hate reduction remain represented only as cancellation state. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model | Partial | Unit Tested | Needs Verification | `Player.AggroList` now mirrors Java `Player.createAggroList()` ownership. Full Java `KnownList`, `Creature`, effect controller, and concurrent lifecycle semantics remain broader. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerReviveCleanupAdapterService` / `PlayerReviveCleanupPlanService` | Service / Adapter | Partial | Unit Tested / Regression Tested | Partial Parity | Adapter can now execute `player.getAggroList().clear()` against a supplied C# list, and still stays disabled unless explicitly requested. Production kisk revive has not yet been wired to live aggro mutation. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryAddKnownAttacker_UsesPlayerKnownListOnlyAwareness` | Unit / model | `PlayerAggroList.isAware` | Known attackers are accepted and unknown attackers are rejected using the player known-list-only rule. | Deterministic assertion from Java source audit. | Uses a boolean known-list input, not live Java `KnownList`. |
| `TryAddKnownAttacker_AccumulatesDamageAndClampsHateLikeAggroInfo` | Unit / model | `AggroInfo.addDamage`, `AggroInfo.addHate` | Positive damage accumulates and hate is clamped to at least 1 per add. | Deterministic assertion from Java source audit. | Does not model `lastInteractionTime` or scheduled hate decay. |
| `Clear_ReturnsEntriesClearsAllAndCancelsHateReductionTask` | Unit / model | `AggroList.clear` | Clear returns previous entries, empties the list, and cancels represented hate-reduction state. | Deterministic assertion from Java source audit. | No real Java `Future` cancellation or `ConcurrentHashMap` race coverage. |
| `PlayerOwnsExecutableAggroList` | Unit / model | `Player.createAggroList` | C# `Player` now owns an executable aggro list. | Deterministic model ownership assertion. | Full player known-list integration remains pending. |
| `Apply_LiveAggroMutationClearsSuppliedPlayerOwnedAggroList` | Unit / adapter | `PlayerReviveService.revive` | Opt-in revive cleanup clears supplied player aggro, reports live mutation, and exposes live plan metadata. | Deterministic adapter regression. | Production kisk revive still does not pass the live aggro list. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- `PlayerOwnedAggroList` uses object-id snapshots and explicit known-list booleans; it does not yet query a live Java-style `KnownList`.
- Ordinary `AggroList` behavior for NPCs/creatures, summons/master transfer, final damage list, target selection, geo visibility, random target choice, `ConcurrentHashMap` race semantics, and hate-reduction scheduling remains unported.
- Production kisk revive still does not execute live aggro mutation.
- Threading differs from Java: C# records hate-reduction cancellation state but does not schedule or cancel a real periodic task.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 executable player-owned aggro boundary plus 1 opt-in revive cleanup adapter mutation path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full known-list integration, creature/NPC aggro behavior, scheduled hate decay, production kisk revive live wiring, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: wire `GameServerConnection.HandleReviveAsync` kisk revive cleanup to pass `player.AggroList` into `PlayerReviveCleanupAdapterService`.
- Add a workflow regression proving live player aggro entries are cleared in Java `PlayerReviveService.revive` order without changing existing movement/teleport/emotion packet order.

## Suggested Acceptance Criteria

- Use the new `Player.AggroList` rather than ad hoc snapshots for the live path.
- Preserve existing behavior when no live mutation is requested.
- Assert aggro entries are cleared before the existing post-restore spawn/movement/emotion boundary as far as current C# observability allows.
- Re-run `PlayerOwnedAggroListTests`, `PlayerReviveCleanupAdapterServiceTests`, and `GameServerConnectionKiskReviveWorkflowTests`.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Production kisk revive aggro clear wiring | `GameServerConnection.cs`, kisk revive workflow tests | Medium | Sequential because it touches shared connection flow. |
| B | Player known-list bridge design | player/world visibility model files | Medium | Can be analysis-only first; implementation may touch shared player/world surfaces. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared revive workflow fixtures.
- Shared `Player` model or revive cleanup adapter files once implementation starts.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1479] Add player owned aggro boundary`.
- Files changed in UOW-1479:
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerOwnedAggroList.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAggroCleanupPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveCleanupPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerReviveCleanupAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerOwnedAggroListTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerReviveCleanupAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALC-Completion.md`
