# Phase 6ALY Completion - Protection Live Fanout Readiness Audit

Date: 2026-05-27
Unit of Work: UOW-1501
Status: Complete after read-only audit.

## Scope

Audit whether existing C# visible-player broadcast infrastructure is ready to send live protection `SM_PLAYER_STATE` packets with Java `PacketSendUtility.broadcastToSightedPlayers` parity.

## Completed Work

- Re-audited Java:
  - `PacketSendUtility.broadcastToSightedPlayers(object, packet, true)`
  - `PacketSendUtility.broadcastPacket(object, packet, true, filter)`
  - `KnownList.sees`
  - `KnownList.forEachPlayer`
- Re-audited C#:
  - `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync`
  - `WorldVisibility.IsVisibleTo`
  - bind-point known-list fanout trace/socket executor scaffolding
  - `PlayerKnownListMembershipService`
- Finding: existing `BroadcastToVisiblePlayersAsync` is not a 1:1 substitute for protection `SM_PLAYER_STATE` fanout because it computes recipients from online players and world-position visibility, while Java uses source known-list membership and filters by each recipient's cached `KnownList.sees(source)` state.
- Finding: bind-point known-list fanout scaffolding provides useful source-first/socket-executor patterns, but it models `broadcastPacket(player, packet, true)` membership traversal, not the extra `other.getKnownList().sees(source)` predicate used by `broadcastToSightedPlayers`.
- No code was changed.

## Validation

- No test run required for docs-only audit.
- Last relevant code validation remains UOW-1500: protection planner/adapter/fanout/report tests passed 40 tests.

## Migration Parity Table - UOW-1501

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` / planned protection fanout bridge | Utility / Broadcast Boundary | Partial | Manual Only | Needs Verification | Java `broadcastToSightedPlayers(..., true)` sends source first, then traverses source known-list players filtered by recipient `KnownList.sees(source)`. Current C# registry uses world-position visibility and is not 1:1 for protection fanout. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `PlayerKnownListMembershipService` / planned protection sighted-recipient projection | Visibility Dependency | Partial | Manual Only | Needs Verification | Existing C# membership service can store owner-known-player entries and visible flags, but protection needs a trace that combines source known-list membership with recipient-side `sees(source)` state. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` | Visibility DTO / Cached State | Partial | Manual Only | Needs Verification | Java `KnownObject.isVisible()` is used by `KnownList.sees`. C# entry has `IsVisibleToOwner`, but no production-proven update path for this protection flow. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | `SmPlayerState` / protection adapter report metadata | Packet / Fanout Payload | Partial | Unit Tested elsewhere | Needs Verification | Packet construction is available and existing protection metadata marks post-visual-state construction. Live recipient selection and socket send remain blocked. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskAdapterService` / planned live fanout boundary | Controller / Adapter Boundary | Partial | Unit Tested | Partial Parity | Protection task adapter exposes plans and report metadata, but live packet fanout should not be enabled until a known-list sighted-recipient projection is in place. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1501 | Manual / documentation audit | `PacketSendUtility`, `KnownList`, C# visibility helpers | Read-only readiness decision for live protection fanout. | Static Java/C# source audit. | Needs a focused sighted-recipient trace planner and tests before code wiring. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Existing C# `BroadcastToVisiblePlayersAsync` remains an approximation for Java known-list fanout and should not be used directly for protection `SM_PLAYER_STATE` parity.
- Existing bind-point known-list socket executor may be reusable later, but its current trace type does not model `KnownList.sees(source)`.
- A production update path for `PlayerKnownListMembershipService` visibility entries is still not proven for protection fanout.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts in this read-only audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, production protection packet fanout, live known-list recipient snapshots, recipient-side `sees(source)` projection, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a non-live protection sighted-recipient trace planner.
- The planner should project Java `broadcastToSightedPlayers(..., true)` recipients from:
  - the source player's known-list snapshot;
  - recipient-side visibility facts equivalent to `other.getKnownList().sees(source)`;
  - the existing protection fanout plan.

## Suggested Acceptance Criteria

- Planner records source-first ordering when `IncludeSourcePlayer=true`.
- Planner filters known-list candidates by recipient-side `sees(source)` visibility, not source-to-recipient distance.
- Planner collapses duplicate known-player object ids conservatively.
- Planner records Java known-list ordering as unspecified/weakly consistent.
- Skipped/no-packet branches produce no recipients.
- Tests cover broadcast branch, invisible-to-recipient filtering, duplicate collapse, and skipped branches.
- Keep production packet sends disabled.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Protection sighted-recipient trace planner | new service/tests | Low-Medium | New non-live metadata files. |
| B | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |
| C | Death workflow production-readiness audit | read-only death workflow files/docs | Low | Independent if kept read-only. |

## Do Not Parallelize

- Shared protection adapter/fanout/report files if composing trace immediately.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1501] Audit protection fanout readiness`.
- Files changed in UOW-1501:
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALY-Completion.md`
