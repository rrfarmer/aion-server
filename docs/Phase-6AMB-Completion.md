# Phase 6AMB Completion - Protection Sighted Recipient Socket Executor

Date: 2026-05-27
Unit of Work: UOW-1504
Status: Complete after validation.

## Scope

Add a disabled-by-default protection sighted-recipient socket executor for projected `broadcastToSightedPlayers(..., true)` recipients.

## Completed Work

- Added `PlayerProtectionActiveTaskSightedRecipientSocketExecutorService`.
- Executor consumes `PlayerProtectionActiveTaskSightedRecipientTrace` plus a concrete packet.
- Executor can send through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled.
- Disabled mode records all projected recipients as not attempted and does not call the registry.
- Enabled test-only mode sends source first, then filtered known-list recipients.
- Source self-send failure stops known-list traversal.
- Known-list recipient failure is recorded and traversal continues.
- Production packet fanout remains disabled; no production caller enables this executor.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 50 tests.

## Migration Parity Table - UOW-1504

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.PlayerProtectionActiveTaskSightedRecipientSocketExecutorService` | Utility / Socket Executor Boundary | Partial | Unit Tested | Partial Parity | Executor models `broadcastToSightedPlayers(..., true)` socket send order over an already-projected recipient trace: source first, then filtered known-list recipients. It is disabled by default and not wired into production. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionActiveTaskSightedRecipientTraceService` / socket executor recipient results | Visibility Dependency | Partial | Unit Tested | Needs Verification | Executor trusts non-live trace recipients. Production known-list and `sees(source)` fact generation remains unproven. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerProtectionActiveTaskSightedRecipient` / socket recipient result metadata | Visibility DTO / Cached State | Partial | Unit Tested | Needs Verification | Recipient visibility is already projected before executor execution. No live Java/C# runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `SmPlayerState` passed to executor in tests | Packet / Socket Payload | Partial | Unit Tested | Needs Verification | Executor can send a concrete `SmPlayerState` in enabled tests. No Java byte comparison or real-client capture in this unit. |
| `com.aionemu.gameserver.controllers.PlayerController` | Protection planner/adapter/trace/executor services | Controller / Fanout Boundary | Partial | Unit Tested | Partial Parity | Protection fanout now has a disabled-by-default send boundary, but production `startProtectionActiveTask` / `stopProtectionActiveTask` live packet fanout remains disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ExecuteAsync_DisabledExecutorDoesNotCallRegistryAndRecordsRecipients` | Unit / executor | `PacketSendUtility.broadcastToSightedPlayers` | Disabled executor records projected recipients as not attempted and does not call registry. | Deterministic disabled-boundary assertion. | No live send. |
| `ExecuteAsync_EnabledSendsSourceFirstThenSightedRecipients` | Unit / executor | `broadcastPacket(..., true, filter)` | Enabled test-only executor sends source first then sighted known-list recipients. | Deterministic Java send-order assertion. | Test uses fake registry, not live client. |
| `ExecuteAsync_EnabledContinuesAfterKnownListRecipientException` | Unit / executor | `KnownList.forEachPlayer` / `CollectionUtil.forEach` | Known-recipient send exception is recorded and traversal continues. | Deterministic modeled Java traversal behavior. | Java runtime exception/log behavior not compared. |
| `ExecuteAsync_EnabledStopsBeforeKnownListWhenSourceSendThrows` | Unit / executor | `broadcastPacket(..., true, filter)` source send before traversal | Source self-send failure prevents known-list traversal. | Deterministic source-first ordering assertion. | Java exception propagation/log behavior not runtime-compared. |
| `ExecuteAsync_NoBroadcastOrNoPacketDoesNotCallRegistry` | Unit / executor | skipped protection fanout branches | No-broadcast/no-packet paths do not call registry. | Deterministic branch assertion. | No production caller wiring. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Executor is disabled by default and not wired into production protection start/stop flow.
- Production source/recipient known-list snapshots and `sees(source)` update paths remain unproven.
- Java exception/logging details for `sendPacket` failures have not been runtime-compared.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled-by-default protection sighted-recipient socket executor and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, recipient-side `sees(source)` production facts, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add an execution-plan/result metadata bridge for protection fanout.
- Compose:
  - adapter result;
  - sighted-recipient trace;
  - concrete `SmPlayerState`;
  - disabled socket executor result.
- Keep production sends disabled and make the boundary observable end-to-end.

## Suggested Acceptance Criteria

- Bridge creates `SmPlayerState` from the post-mutation player state for broadcast branches.
- Bridge invokes the socket executor disabled by default and records `SentPackets=false`.
- No-broadcast branches produce no packet/executor sends.
- Tests cover start broadcast, spawned stop broadcast, and skipped branches.
- No production caller enables the executor.
- Re-run protection planner/adapter/fanout/report/trace/executor tests plus `PlayerStateTests`.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection fanout execution bridge | new bridge service/tests | Medium | New files; composes existing protection metadata. |
| B | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |
| C | Death workflow production-readiness audit | read-only death workflow files/docs | Low | Independent if kept read-only. |

## Do Not Parallelize

- Shared protection adapter/fanout/report/trace/executor files if changing result shapes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1504] Add protection sighted socket executor`.
- Files changed in UOW-1504:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskSightedRecipientSocketExecutorService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskSightedRecipientSocketExecutorServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMB-Completion.md`
