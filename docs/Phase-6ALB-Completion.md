# Phase 6ALB Completion - Restored Kisk Added-Member Fanout

Date: 2026-05-27
Unit of Work: UOW-1478
Status: Complete after validation.

## Scope

Cover C# restored-added kisk fanout recipient planning after the restored player has already received a direct login restore update.

## Completed Work

- Re-audited Java `Kisk.addPlayer` and `Kisk.broadcastKiskUpdate`.
- Added `CreatePlanForRestoredAddedMemberExcludesRestoredPlayerButUpdatesOtherRecipients`.
- Verified the restored player can be excluded from follow-up broadcast while other unknown current members and same-race visible players still receive update intents.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1478

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskUpdateFanoutService` / `PlayerKiskLoginRestorePacketPlanService` | Runtime State / Fanout Planner | Partial | Unit Tested | Partial Parity | Test covers restored-added split: C# excludes the restored player from follow-up broadcast because the login restore planner already sends that player a direct update, while other unknown members and visible same-race recipients remain included. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player` / `WorldVisibility` | Model / Visibility Input | Partial | Unit Tested | Needs Verification | Test uses object id, race, and position to model Java member and same-race visible filtering. Full known-list runtime behavior remains broader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` / `PlayerKiskUpdateFanoutPlan` | Packet Boundary / Planner | Partial | Unit Tested | Needs Verification | Recipient planning is covered; packet byte output and Java runtime fanout output were not compared. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlanForRestoredAddedMemberExcludesRestoredPlayerButUpdatesOtherRecipients` | Unit / fanout planner | `Kisk.addPlayer`, `Kisk.broadcastKiskUpdate` | Restored player excluded from follow-up broadcast does not prevent updates to other unknown members or same-race visible players. | Deterministic fanout plan regression from Java source audit and C# login restore split. | Does not execute full login socket flow or Java runtime packet output. |
| Existing `PlayerKiskUpdateFanoutServiceTests` | Existing Unit | `Kisk.broadcastKiskUpdate` | Existing direct member, known-list, visible same-race, and known different-race tests remained stable. | Focused 6-test suite passed. | Java known-list internals remain approximated through callbacks. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite currently has two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- The restored-added flow is split between direct login restore packets and later fanout planning in C#; Java does this inside `Kisk.addPlayer` / `broadcastKiskUpdate`.
- Packet byte parity for `SmKiskUpdate` remains unverified against Java runtime output.
- Direct Java `Player.kisk` object references differ from C# `BoundKiskObjectId` / runtime registry references.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 fanout regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, packet byte comparison, Java known-list internals, direct Java object-reference semantics, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: move from kisk lifecycle back to the broader Phase 6 blocker list, preferably player-owned aggro list design to unblock live kisk revive aggro cleanup.
- Alternative small kisk task: kisk packet byte comparison notes, but Java runtime artifact generation is still blocked locally.

## Suggested Acceptance Criteria

- For aggro design, inspect Java player aggro list ownership and revive cleanup callers before adding C# types.
- Keep live mutation disabled unless the required player-owned aggro state is modeled.
- If staying on kisk packets, document byte-comparison blockers without claiming runtime parity.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player-owned aggro list Java analysis | read-only Java/C# aggro/revive files | Low | Analysis-only can be parallelized. |
| B | Kisk packet byte comparison notes | packet tests/docs | Low | Blocked from runtime verification locally. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate from kisk; current full-suite blocker. |

## Do Not Parallelize

- Shared revive workflow fixtures.
- Shared player model or aggro service files once implementation starts.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1478] Cover restored kisk added-member fanout`.
- Files changed in UOW-1478:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskUpdateFanoutServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALB-Completion.md`
