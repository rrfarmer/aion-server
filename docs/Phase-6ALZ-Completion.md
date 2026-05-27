# Phase 6ALZ Completion - Protection Sighted Recipient Trace

Date: 2026-05-27
Unit of Work: UOW-1502
Status: Complete after validation.

## Scope

Add a non-live protection sighted-recipient trace planner for Java `PacketSendUtility.broadcastToSightedPlayers(..., true)`.

## Completed Work

- Added `PlayerProtectionActiveTaskSightedRecipientTraceService`.
- Planner inputs:
  - existing protection fanout plan;
  - source player's known-list membership snapshot;
  - recipient-side visibility facts equivalent to `other.getKnownList().sees(source)`.
- Planner records:
  - source-first ordering for `toSelf=true`;
  - source known-list traversal;
  - recipient-side `KnownList.sees(source)` filtering;
  - duplicate known-object-id collapse;
  - Java known-list ordering as unspecified;
  - skipped/no-broadcast branches.
- Production packet fanout remains disabled.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskFanoutPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskAdapterServiceTests|FullyQualifiedName~PlayerStateTests"`.
- Result: passed 44 tests.

## Migration Parity Table - UOW-1502

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.PlayerProtectionActiveTaskSightedRecipientTraceService` | Utility / Fanout Trace Planner | Partial | Unit Tested | Partial Parity | Trace planner models `broadcastToSightedPlayers(..., true)` recipient projection: source first, then source known-list players that pass recipient-side visibility facts. It does not send packets. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerProtectionActiveTaskSightedRecipientTraceService` / `PlayerKnownListMembershipService` | Visibility Dependency | Partial | Unit Tested | Needs Verification | Planner consumes source known-list snapshot and separate recipient-side `sees(source)` facts. Production known-list update path remains unproven for this flow. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` / `PlayerProtectionActiveTaskRecipientVisibilityFact` | Visibility DTO / Cached State | Partial | Unit Tested | Needs Verification | Planner uses C# membership entries plus recipient visibility facts to model Java `KnownObject.isVisible`. No live Java/C# runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `SmPlayerState` / protection fanout plan feeding sighted trace | Packet / Fanout Dependency | Partial | Unit Tested | Needs Verification | Trace planner depends on existing fanout plan that identifies `SM_PLAYER_STATE`; it still does not serialize or send packets. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskFanoutPlanService` / `PlayerProtectionActiveTaskSightedRecipientTraceService` | Controller / Fanout Boundary | Partial | Unit Tested | Partial Parity | Protection start/stop fanout can now project recipient intent after task/fanout planning. Live scheduler/cast/target/packet/AI side effects remain disabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateTrace_ProjectsSourceFirstThenRecipientsThatSeeSource` | Unit / trace planner | `PacketSendUtility.broadcastToSightedPlayers`, `KnownList.sees` | Source is first; known-list candidates are included only when recipient-side `sees(source)` is true, independent of source-to-recipient visible flag. | Deterministic Java source-order and predicate assertion from planner metadata. | No live send or Java runtime comparison. |
| `CreateTrace_DeduplicatesSourceKnownListCandidates` | Unit / trace planner | `KnownList.knownObjects` object-id map | Duplicate known-player candidates collapse to one recipient and ordering is marked unspecified. | Deterministic known-list object-id behavior assertion. | C# snapshot order is not a Java runtime ordering proof. |
| `CreateTrace_MissingRecipientVisibilityFactSkipsKnownPlayer` | Unit / trace planner | `KnownList.sees` | Known-list candidate without recipient-side visibility proof is skipped conservatively. | Conservative parity assertion. | Runtime source of recipient facts still missing. |
| `CreateTrace_SkippedFanoutPlanHasNoRecipients` | Unit / trace planner | `PlayerController.startProtectionActiveTask` already-protected branch | No-broadcast fanout plan produces no recipients. | Deterministic Java branch assertion. | No live branch wiring. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Sighted-recipient trace is non-live and is not yet composed into adapter/report results.
- Production source/recipient known-list snapshots and `sees(source)` update paths remain unproven.
- Existing C# `BroadcastToVisiblePlayersAsync` remains an approximation and is not used by this planner.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live protection sighted-recipient trace planner and 1 focused test suite
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, recipient-side `sees(source)` production facts, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: compose `PlayerProtectionActiveTaskSightedRecipientTrace` into protection report or adapter metadata.
- Prefer report composition if it can stay additive; expose recipient-projection rows without changing live behavior.
- Keep production packet sends disabled.

## Suggested Acceptance Criteria

- Report or adapter result can carry a sighted-recipient trace when caller supplies source known-list and recipient visibility facts.
- Broadcast branches expose source-first and filtered known-list recipients.
- Skipped/no-broadcast branches expose empty recipient trace.
- Existing adapter calls without membership facts remain deterministic and conservative.
- Tests cover start broadcast trace exposure, invisible-recipient filtering, and skipped branches.
- Re-run protection planner/adapter/fanout/report/trace tests plus `PlayerStateTests`.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose sighted trace into report/adapter metadata | protection adapter/report service/tests | Medium | Shared result shape; best done sequentially. |
| B | Protection socket executor analysis | bind-point socket executor and connection registry | Low if read-only | Useful before live send bridge. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared protection adapter/fanout/report/trace files if composing metadata.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1502] Add protection sighted recipient trace`.
- Files changed in UOW-1502:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskSightedRecipientTraceService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskSightedRecipientTraceServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALZ-Completion.md`
