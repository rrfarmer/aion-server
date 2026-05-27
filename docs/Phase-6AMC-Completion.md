# Phase 6AMC Completion - Protection Execution Metadata Bridge

Date: 2026-05-27
Unit of Work: UOW-1505
Status: Complete after validation.

## Scope

Add a non-live protection execution metadata bridge that composes the existing protection adapter, sighted-recipient trace, concrete `SmPlayerState`, and disabled socket executor result without enabling production sends.

## Completed Work

- Added `PlayerProtectionActiveTaskExecutionBridgeService`.
- Bridge calls `PlayerProtectionActiveTaskAdapterService.Apply`.
- Bridge constructs `SmPlayerState` for broadcast branches after the adapter has applied any opt-in visual mutation.
- Bridge invokes `PlayerProtectionActiveTaskSightedRecipientSocketExecutorService` disabled by default.
- Disabled executor metadata remains observable end-to-end while avoiding `IGameClientConnectionRegistry` calls.
- Skipped Java branches create no packet and return no-packet executor results.
- Production packet fanout remains disabled; no production caller enables this bridge or socket executor.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskExecutionBridgeServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 53 tests.

## Migration Parity Table - UOW-1505

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskExecutionBridgeService` | Controller / Execution Bridge | Partial | Unit Tested | Partial Parity | Bridge composes visual mutation metadata, concrete `SM_PLAYER_STATE` construction, sighted-recipient trace, and disabled socket executor result. Scheduler, cast cancellation, target removal, AI move notification, and production packet sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerState` | Packet / Broadcast Payload | Partial | Unit Tested | Needs Verification | Bridge constructs concrete `SmPlayerState` for broadcast branches after opt-in visual mutation. Existing packet tests cover deterministic C# bytes, but this unit did not compare against generated Java runtime artifacts. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `PlayerProtectionActiveTaskExecutionBridgeService` / `PlayerProtectionActiveTaskSightedRecipientSocketExecutorService` | Utility / Fanout Boundary | Partial | Unit Tested | Partial Parity | Bridge invokes the socket executor disabled by default so `broadcastToSightedPlayers(..., true)` send intent is observable end-to-end without live sends. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionActiveTaskSightedRecipientTraceService` consumed by execution bridge | Visibility Dependency | Partial | Unit Tested | Needs Verification | Bridge consumes existing non-live source known-list and recipient-side `sees(source)` facts through the adapter trace. Live production fact generation remains unproven. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` / execution bridge recipient metadata | Visibility DTO / Cached State | Partial | Unit Tested | Needs Verification | Known-object visibility is still pre-projected before bridge execution. No live Java/C# runtime comparison. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_LiveStartBuildsPlayerStatePacketAndDisabledExecutorDoesNotSend` | Unit / execution bridge | `PlayerController.startProtectionActiveTask`, `PacketSendUtility.broadcastToSightedPlayers` | Opt-in live visual start constructs `SmPlayerState`, projects source plus sighted recipient, and disabled executor avoids registry calls. | Deterministic Java order and no-send boundary assertion. | No Java byte/runtime comparison; no production caller wiring. |
| `ExecuteAsync_LiveSpawnedStopBuildsPlayerStatePacketAfterClearingBlinking` | Unit / execution bridge | `PlayerController.stopProtectionActiveTask` spawned branch | Opt-in spawned stop clears BLINKING, constructs `SmPlayerState`, and records disabled executor metadata. | Deterministic branch and post-mutation packet construction assertion. | No live AI move notification or packet send. |
| `ExecuteAsync_SkippedBranchesDoNotConstructPacketOrSend` | Unit / execution bridge | already-protected start and unspawned stop branches | Skipped Java fanout branches create no packet and produce no-packet executor results. | Deterministic skipped-branch assertion. | No concurrency or runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Bridge does not wire production callers and does not enable packet sends by default.
- Production source/recipient known-list snapshots and recipient-side `sees(source)` update paths remain unproven.
- Scheduler, cast interruption, target cleanup, and AI notification remain staged out of live protection execution.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 protection execution metadata bridge and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, recipient-side `sees(source)` production facts, scheduler/cast/target/AI side-effect execution, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a protection scheduler/cast/target/AI side-effect audit or non-live operation-plan bridge.
- Cover remaining Java side effects:
  - `ThreadPoolManager.getInstance().schedule(this::stopProtectionActiveTask, 60000)`;
  - `getCast().cancelCast()`;
  - `setTarget(null)`;
  - `getAi().notifyMove()`.
- Keep scheduler execution, cast cancellation, target mutation, AI notification, and packet sends disabled until supporting runtime gates exist.

## Suggested Acceptance Criteria

- Plan or audit explicitly orders start-side visual mutation, packet fanout, and delayed stop scheduling.
- Plan or audit explicitly orders stop-side task cancellation, spawned guard, visual mutation, packet fanout, cast cancellation, target clear, and AI move notification.
- Tests cover already-protected start, normal start, spawned stop, and unspawned stop.
- Migration parity table includes all newly touched Java artifacts, especially scheduler/cast/AI dependencies.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection side-effect operation-plan bridge | new service/tests | Medium | New files preferred; avoid changing adapter shape unless necessary. |
| B | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |
| C | Death workflow production-readiness audit | read-only death workflow files/docs | Low | Independent if kept read-only. |

## Do Not Parallelize

- Shared protection adapter/fanout/report/trace/executor files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1505] Add protection execution bridge`.
- Files changed in UOW-1505:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskExecutionBridgeService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskExecutionBridgeServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMC-Completion.md`
