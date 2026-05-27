# Phase 6ALU Completion - Protection State Fanout Planner

Date: 2026-05-27
Unit of Work: UOW-1497
Status: Complete after validation.

## Scope

Add a non-live recipient/order planner for Java protection active task `SM_PLAYER_STATE` broadcasts.

## Completed Work

- Re-audited Java `PlayerController.startProtectionActiveTask`, `PlayerController.stopProtectionActiveTask`, `SM_PLAYER_STATE`, `PacketSendUtility.broadcastToSightedPlayers`, `PacketSendUtility.broadcastPacket(..., true, filter)`, and `KnownList.sees`.
- Added `PlayerProtectionActiveTaskFanoutPlanService`.
- Planner records:
  - start broadcast occurs after `setVisualState(BLINKING)`;
  - stop broadcast occurs after `unsetVisualState(BLINKING)`;
  - `SM_PLAYER_STATE` packet construction is after the visual mutation and therefore captures post-mutation visual state;
  - `toSelf=true` sends the source player first;
  - sighted-player recipients are Java known-list players filtered by `other.getKnownList().sees(source)`;
  - all fanout remains non-live with `SentPackets=false`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 36 tests.

## Migration Parity Table - UOW-1497

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFanoutPlanService` / `PlayerProtectionActiveTaskPlanService` | Controller / Planner Service | Partial | Unit Tested | Partial Parity | Fanout planner models start/stop protection broadcast ordering. Live scheduling, cast cancellation, target removal, packet send, and AI notification remain unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerState` / `PlayerProtectionActiveTaskFanoutPlanService` | Packet / Fanout Metadata | Partial | Unit Tested | Needs Verification | Planner records packet type and opcode `68`, and asserts construction after BLINKING mutation. No Java byte comparison or live client capture in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.PlayerProtectionActiveTaskFanoutPlanService` / existing `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` metadata | Utility / Broadcast Planner | Partial | Unit Tested | Needs Verification | Planner records `broadcastToSightedPlayers(..., true)` semantics: source-player send first, then known-list recipients filtered by `other.getKnownList().sees(source)`. No live registry send or socket-order capture. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionActiveTaskFanoutPlanService` metadata | Visibility Dependency | Not Started | Unit Tested via metadata | Needs Verification | Recipient selection is documented as a live known-list dependency. C# currently has world-position visibility helpers, not a 1:1 known-list recipient snapshot for this flow. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | `Aion.GameServer.Model.GameObjects.PlayerVisualStates` | Enum / State Dependency | Partial | Regression Tested | Needs Verification | Planner records packet construction after set/unset BLINKING so packet metadata reflects post-mutation visual state. No Java runtime comparison was run. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_StartBroadcastsSmPlayerStateAfterBlinkingState` | Unit / fanout planner | `PlayerController.startProtectionActiveTask`, `SM_PLAYER_STATE`, `PacketSendUtility.broadcastToSightedPlayers` | Start fanout records BLINKING mutation before broadcast, packet type/opcode, source inclusion, source-first send, known-list visibility filter, and non-live status. | Deterministic Java source-order assertion from planner metadata. | No live send, no Java byte comparison, no concrete recipient list. |
| `Create_StartAlreadyProtectedSkipsFanout` | Unit / fanout planner | `PlayerController.startProtectionActiveTask` | Already protected start branch produces no packet/fanout metadata. | Deterministic Java branch assertion. | No concurrency coverage. |
| `Create_StopSpawnedBroadcastsAfterBlinkingClear` | Unit / fanout planner | `PlayerController.stopProtectionActiveTask`, `SM_PLAYER_STATE`, `PacketSendUtility.broadcastToSightedPlayers` | Spawned stop fanout records unset-BLINKING before broadcast and known-list recipient metadata. | Deterministic Java source-order assertion from planner metadata. | No live send, no Java byte comparison, no concrete recipient list. |
| `Create_StopUnspawnedSkipsFanout` | Unit / fanout planner | `PlayerController.stopProtectionActiveTask` | Unspawned stop branch records that Java skips `SM_PLAYER_STATE` fanout. | Deterministic Java branch assertion. | C# still lacks a live `Player.isSpawned()` model for this flow. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Fanout planner is metadata only and is not wired into production packet sends.
- C# visible-player broadcast currently uses world-position visibility, while Java protection fanout depends on known-list iteration and `other.getKnownList().sees(source)`.
- Packet serialization/runtime ordering still needs Java-generated packet artifacts or live socket capture before parity can be verified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection fanout planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, packet/runtime socket-order verification, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose the new protection fanout metadata into `PlayerProtectionActiveTaskAdapterService` results.
- Disabled and live-visual-only adapter calls should expose the fanout plan alongside the existing protection task plan.
- Keep `SentPackets=false` and production packet fanout disabled.

## Suggested Acceptance Criteria

- Adapter result includes a `PlayerProtectionActiveTaskFanoutPlan`.
- Disabled start/stop calls expose fanout metadata without mutating visual state, scheduler, or packets.
- Live start call mutates BLINKING and exposes start fanout metadata with `SentPackets=false`.
- Live spawned stop call clears BLINKING and exposes stop fanout metadata with `SentPackets=false`.
- Already-protected start and unspawned stop expose skipped fanout statuses.
- Re-run protection active task planner/adapter/fanout tests plus `PlayerStateTests`.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose fanout plan into protection adapter | adapter service/tests | Medium | Shared adapter result shape; best done by orchestrator. |
| B | Death workflow production-readiness audit | read-only death files/docs | Low | Independent if kept read-only. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection adapter files.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1497] Add protection state fanout planner`.
- Files changed in UOW-1497:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskFanoutPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskFanoutPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALU-Completion.md`
