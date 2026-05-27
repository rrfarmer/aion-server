# Phase 6AKV Completion - Kisk Known Enemy Fanout

Date: 2026-05-27
Unit of Work: UOW-1472
Status: Complete after validation.

## Scope

Cover a Java `Kisk.broadcastKiskUpdate` fanout edge where a kisk member already known to the kisk is skipped by direct member fanout, and only same-race visible players receive the second-stage broadcast.

## Completed Work

- Re-read Java `Kisk.addPlayer`, `Kisk.removePlayer`, and `Kisk.broadcastKiskUpdate`.
- Added `CreatePlanSkipsKnownDifferentRaceMemberLikeJavaBroadcastKiskUpdate`.
- The test models an unrestricted kisk with a different-race member and a same-race member that both already know the kisk.
- It verifies the different-race known member is not included in either direct member updates or visible same-race broadcast, while the same-race known member is included in visible same-race fanout.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 5 tests.

## Migration Parity Table - UOW-1472

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskUpdateFanoutService` | Service / Fanout Planner | Partial | Unit Tested | Partial Parity | Test covers `broadcastKiskUpdate` direct-member skip for known members plus same-race visible broadcast filtering. No live packet send or Java runtime packet comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player` / `WorldVisibility` | Model / Visibility Input | Partial | Unit Tested | Needs Verification | Test uses race and position inputs to model Java same-race visible broadcast filtering. Full Java known-list and `PacketSendUtility.broadcastPacket` behavior remain broader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` / `PlayerKiskUpdateFanoutPlan` | Packet Boundary / Planner | Partial | Unit Tested | Partial Parity | Fanout recipient planning is covered; actual packet serialization is covered elsewhere and Java runtime fanout output is not compared. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlanSkipsKnownDifferentRaceMemberLikeJavaBroadcastKiskUpdate` | Unit / fanout planner | `Kisk.broadcastKiskUpdate` | A different-race kisk member that already knows the kisk is skipped by direct member updates and by same-race visible broadcast, while a same-race known member is included in visible fanout. | Deterministic fanout plan regression from reviewed Java source. | No live socket packet send or Java runtime comparison. |
| Existing `PlayerKiskUpdateFanoutServiceTests` | Existing Unit | `Kisk.broadcastKiskUpdate` | Existing direct member, visible same-race, unavailable known-list, and NPC visibility known-list tests remained stable. | Focused 5-test suite passed. | Full Java `PacketSendUtility.broadcastPacket` and known-list internals remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Fanout planner tests do not execute real socket sends or compare Java packet output.
- C# uses `NpcVisibilityService.IsKnownNpc` as the known-list callback rather than Java direct `Kisk.getKnownList().knows`.
- Java duplicate `Kisk.addPlayer` branch sends `SM_KISK_UPDATE` only to the player; C# bind dialog/authorization flow still needs a dedicated comparison before changing behavior.
- Live player aggro mutation remains blocked by the missing C# player-owned aggro list.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 fanout regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live socket fanout comparison, Java known-list callback internals, duplicate add-player branch comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk add/remove member parity with the Java duplicate `Kisk.addPlayer` direct-only update branch, or move to online kisk removal packet/order fanout review.
- Preferred next kisk slice: audit C# bind dialog and response flow against Java `KiskService.onBind` / `Kisk.addPlayer` duplicate handling before changing behavior.

## Suggested Acceptance Criteria

- Determine whether the Java duplicate `Kisk.addPlayer` branch is reachable through current C# bind flow or already blocked by authorization.
- If adding coverage, make the distinction between Java direct `SM_KISK_UPDATE` and C# system-message rejection explicit.
- Preserve current kisk bind/revive workflow behavior unless a confirmed mismatch requires a production change.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java duplicate add-player audit | `Kisk.java`, `KiskService.java`, `KiskAI.java` | Low | Safe read-only sub-agent candidate. |
| B | C# bind duplicate flow regression | kisk dialog/bind question response tests | Medium | Sequential if touching shared connection fixtures. |
| C | Online removal packet/order review | runtime cleanup tests | Medium | Separate from bind duplicate flow. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate from kisk fanout. |

## Do Not Parallelize

- Shared kisk bind or revive workflow fixture changes.
- Shared active connection fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1472] Cover kisk known enemy fanout`.
- Files changed in UOW-1472:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskUpdateFanoutServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKV-Completion.md`
