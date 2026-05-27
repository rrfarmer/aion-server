# Phase 6AMA Completion - Protection Adapter Sighted Trace Exposure

Date: 2026-05-27
Unit of Work: UOW-1503
Status: Complete after validation.

## Scope

Compose `PlayerProtectionActiveTaskSightedRecipientTrace` into protection adapter metadata.

## Completed Work

- Extended `PlayerProtectionActiveTaskAdapterRequest` with optional source known-list snapshot and recipient visibility facts.
- Extended `PlayerProtectionActiveTaskAdapterResult` with `PlayerProtectionActiveTaskSightedRecipientTrace`.
- Adapter results now carry:
  - task plan;
  - fanout plan;
  - sighted-recipient trace;
  - Java-order report metadata.
- Existing callers without membership facts remain deterministic and conservative:
  - broadcast branches project the source self-send only;
  - skipped branches project no recipients.
- Runtime behavior remains unchanged: packet sends, scheduler, cast cancellation, target removal, and AI notification remain disabled; `SentPackets=false`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 45 tests.

## Migration Parity Table - UOW-1503

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskAdapterService` | Controller / Adapter Boundary | Partial | Unit Tested | Partial Parity | Adapter now exposes sighted-recipient trace metadata for protection start/stop fanout while only opt-in visual-state mutation can run live. Scheduler/cast/target/packet/AI side effects remain disabled. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `PlayerProtectionActiveTaskAdapterService` / `PlayerProtectionActiveTaskSightedRecipientTraceService` | Utility / Fanout Metadata | Partial | Unit Tested | Partial Parity | Adapter request can accept source known-list and recipient-side visibility facts, and result exposes projected Java `broadcastToSightedPlayers(..., true)` recipients. No live send. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerKnownListMembershipSnapshot` / `PlayerProtectionActiveTaskRecipientVisibilityFact` consumed by adapter | Visibility Dependency | Partial | Unit Tested | Needs Verification | Adapter can consume non-live known-list/visibility facts. Production update path for these facts remains unproven. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` / adapter trace metadata | Visibility DTO / Cached State | Partial | Unit Tested | Needs Verification | Adapter trace uses supplied visibility facts to model Java `KnownObject.isVisible`. No Java runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `SmPlayerState` / adapter fanout and trace metadata | Packet / Fanout Payload | Partial | Unit Tested | Needs Verification | Adapter still reports `SentPackets=false`; packet serialization exists elsewhere but live recipient sends are disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_DisabledExposesStartPlanWithoutVisualMutation` | Unit / adapter | `PlayerController.startProtectionActiveTask`, `PacketSendUtility.broadcastToSightedPlayers` | Existing no-facts disabled start path now exposes source-only sighted trace and no packet sends. | Conservative adapter trace assertion. | No known-list facts supplied, no live send. |
| `Apply_LiveStartSetsBlinkingButLeavesSchedulerAndPacketsPlanned` | Unit / adapter | `PlayerController.startProtectionActiveTask` | Live-start path exposes source-only sighted trace while setting BLINKING only. | Deterministic C# mutation plus metadata assertion. | No live cast/target cleanup, scheduler, or packet send. |
| `Apply_LiveStartAlreadyProtectedDoesNotMutate` | Unit / adapter | `PlayerController.startProtectionActiveTask` | Already-protected start exposes no-broadcast trace. | Deterministic Java branch assertion. | No concurrency coverage. |
| `Apply_LiveStopClearsBlinkingButLeavesSchedulerAndPacketsPlanned` | Unit / adapter | `PlayerController.stopProtectionActiveTask` | Live spawned-stop path exposes source-only sighted trace while clearing BLINKING only. | Deterministic C# mutation plus metadata assertion. | No live scheduler, packet send, or AI notification. |
| `Apply_LiveStopUnspawnedDoesNotClearBlinking` | Unit / adapter | `PlayerController.stopProtectionActiveTask` | Unspawned stop exposes no-broadcast trace. | Deterministic Java branch assertion. | C# lacks live `Player.isSpawned()` model. |
| `Apply_DisabledStartProjectsSuppliedSightedRecipientsWithoutSendingPackets` | Unit / adapter | `PacketSendUtility.broadcastToSightedPlayers`, `KnownList.sees` | Adapter consumes supplied source known-list and recipient visibility facts to expose source-first filtered recipients with `SentPackets=false`. | Deterministic Java predicate assertion through adapter metadata. | Supplied facts are not live production known-list state. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Sighted-recipient trace exposure is metadata only; no packet sends were enabled.
- Production source/recipient known-list snapshots and `sees(source)` update paths remain unproven.
- Socket executor semantics for protection fanout still need a disabled-by-default bridge before live send can be considered.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: adapter sighted-recipient trace exposure plus focused adapter test updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, recipient-side `sees(source)` production facts, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a disabled-by-default protection sighted-recipient socket executor or execution-plan metadata bridge.
- Model it after the bind-point known-list executor:
  - source self-send first;
  - filtered known-list recipients after source;
  - source failure stops traversal;
  - known-list recipient failure continues traversal;
  - disabled mode reports no sends.
- Keep production packet sends disabled.

## Suggested Acceptance Criteria

- Executor/plan consumes `PlayerProtectionActiveTaskSightedRecipientTrace` and the packet from the fanout plan.
- Disabled mode reports all recipients as not attempted and `SentPackets=false`.
- Enabled mode can send source first then filtered known-list recipients through `SendPacketToPlayerAsync` in tests only.
- Source failure prevents known-list traversal; known-list recipient failure continues.
- No production caller enables the executor.
- Re-run protection planner/adapter/fanout/report/trace/executor tests plus `PlayerStateTests`.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection sighted-recipient socket executor | new executor service/tests | Medium | New files, but touches packet-send boundary semantics. |
| B | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |
| C | Death workflow production-readiness audit | read-only death workflow files/docs | Low | Independent if kept read-only. |

## Do Not Parallelize

- Shared protection adapter/fanout/report/trace files if composing executor into adapter immediately.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1503] Expose protection sighted trace from adapter`.
- Files changed in UOW-1503:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskAdapterServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AMA-Completion.md`
