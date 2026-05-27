# Phase 6AKF Completion - Live Revive Team Movement Fanout

Date: 2026-05-27
Unit of Work: UOW-1456
Status: Complete after validation.

## Scope

Wire live kisk revive group/alliance movement fanout now that the group and alliance revive movement planners exist. This unit keeps the change inside the kisk revive handler and workflow fixture.

## Completed Work

- Added `GameServerConnection.SendReviveMovementUpdatesAsync`.
- Called the revive movement fanout after kisk restore/target cleanup and before resurrect emotion fanout.
- Reused `PlayerGroupMovementUpdatePlanner.CreateReviveMovementUpdatePlan`.
- Reused `PlayerAllianceMovementUpdatePlanner.CreateReviveMovementUpdatePlan`.
- Added group and alliance kisk revive workflow tests that assert movement member-info packets precede revive emotion and exclude the revived subject.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests"`.
- Result: passed 9 tests.
- Ran `git diff --check`.
- Result: only CRLF conversion warnings for touched files.

## Migration Parity Table - UOW-1456

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` / `SendReviveMovementUpdatesAsync` | Service / Connection Workflow | Partial | Regression Tested | Partial Parity | Live kisk revive now sends group/alliance movement updates before resurrect emotion. Remaining Java revive gaps include aggro cleanup, soul sickness, prison/event branches, resurrection skill id messages, flying-before-death restoration, and broader teleport side effects. |
| `com.aionemu.gameserver.services.player.PlayerGroupService.updateGroup` | `PlayerGroupMovementUpdatePlanner.CreateReviveMovementUpdatePlan` / `SmGroupMemberInfo` send bridge | Service / Packet Fanout | Partial | Regression Tested | Partial Parity | Runtime bridge sends movement member-info packets to all group members except the revived subject. Full Java packet-byte comparison and offline recipient behavior remain unverified. |
| `com.aionemu.gameserver.services.player.PlayerAllianceService.updateAlliance` | `PlayerAllianceMovementUpdatePlanner.CreateReviveMovementUpdatePlan` / `SmAllianceMemberInfo` send bridge | Service / Packet Fanout | Partial | Regression Tested | Partial Parity | Runtime bridge sends movement member-info packets to all alliance members except the revived subject. The bridge reconstructs members from `PlayerAllianceRuntime`; Java runtime comparison remains blocked. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupUpdateEvent` | `PlayerGroupMemberInfoUpdatePlan` / `PlayerGroupEvent.Movement` | Event / Packet Plan | Partial | Regression Tested | Partial Parity | Existing planner packet branch is now executed by live kisk revive. Packet bytes remain covered only by deterministic C# packet tests, not Java artifact comparison. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceUpdateEvent` | `PlayerAllianceMemberInfoUpdatePlan` / `PlayerAllianceEvent.Movement` | Event / Packet Plan | Partial | Regression Tested | Partial Parity | Existing planner packet branch is now executed by live kisk revive. Alliance slot/group details remain as current C# planner metadata; Java byte comparison remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_GroupKiskReviveSendsMovementUpdateBeforeReviveEmotion` | Regression / connection workflow | `PlayerReviveService.revive`, `PlayerGroupService.updateGroup`, `PlayerGroupUpdateEvent` | Group kisk revive sends one `SmGroupMemberInfo` movement packet to the other group member before revive emotion and excludes the revived subject. | Deterministic C# workflow assertion from reviewed Java order. | Does not compare Java packet bytes or prove every Java recipient predicate nuance. |
| `HandleReviveAsync_AllianceKiskReviveSendsMovementUpdateBeforeReviveEmotion` | Regression / connection workflow | `PlayerReviveService.revive`, `PlayerAllianceService.updateAlliance`, `PlayerAllianceUpdateEvent` | Alliance kisk revive sends one `SmAllianceMemberInfo` movement packet to the other alliance member before revive emotion and excludes the revived subject. | Deterministic C# workflow assertion from reviewed Java order. | Does not compare Java packet bytes or prove every Java alliance group/slot nuance. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Existing Regression | Kisk revive, target cleanup, kisk depletion cleanup, teleport fanout | Kisk charge, restore, target cleanup, teleport delete, depleted-kisk cleanup, and object-id release remained stable. | 9-test focused suite passed. | Broader revive and teleport side effects remain partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Group/alliance revive movement fanout is now live, but exact Java packet bytes and recipient predicates remain unverified.
- Java aggro cleanup, soul sickness, prison/event-mode pre-teleports, skill-id resurrection message, flying-before-death restoration, protection tasks, and full world despawn/spawn ownership remain incomplete.
- Date/time handling was not changed in this unit; no new precision/rounding behavior was introduced.
- No new reflection, threading, or serialization behavior was introduced beyond existing async registry packet sends.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 live revive movement fanout bridge
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java packet byte comparisons, broader revive side effects
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk/revive with a read-only aggro cleanup audit.
- If a C# aggro model already exists, add the smallest service/test seam for Java `PlayerReviveService.revive -> player.getAggroList().clear()`.
- If aggro surfaces are not ready, switch to the toy-pet lifetime schedule observability design note before adding any scheduling hook.

## Suggested Acceptance Criteria

- Identify the Java aggro cleanup source and any C# equivalent before editing production code.
- Do not invent a broad combat aggro system for this slice.
- If adding code, keep it isolated from teleport packet ordering unless a test explicitly proves the order.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Aggro cleanup audit | Java revive service, C# model/services search | Low | Read-only first; decide whether a real C# surface exists. |
| B | Toy-pet lifetime schedule observability design | docs or fixture design | Medium | Good fallback if aggro has no local model. |
| C | Kisk revive socket-order hardening | kisk workflow tests | Medium | Sequential only if touching shared fixture again. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` revive changes.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1456] Wire revive team movement fanout`.
- C# files changed in UOW-1456:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKF-Completion.md`
