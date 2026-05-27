# Phase 6AKE Completion - Alliance Revive Movement Metadata

Date: 2026-05-27
Unit of Work: UOW-1455
Status: Complete after validation.

## Scope

Add an explicit staged alliance movement planner entry point for Java `PlayerReviveService.revive`. This unit adds a small service wrapper and focused unit coverage, but does not wire live kisk revive fanout.

## Completed Work

- Added `PlayerAllianceMovementUpdatePlanner.CreateReviveMovementUpdatePlan`, an explicit Java-breadcrumbed wrapper for `PlayerReviveService.revive -> PlayerAllianceService.updateAlliance(..., MOVEMENT)`.
- Added `CreateReviveMovementUpdatePlan_UsesJavaPlayerReviveMovementEvent`, asserting the revive wrapper emits `PlayerAllianceEvent.Movement` intents to all alliance members except the revived subject.
- Kept live `GameServerConnection.HandleReviveAsync` group/alliance fanout unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests"`.
- Result: passed 34 tests.

## Migration Parity Table - UOW-1455

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `PlayerAllianceMovementUpdatePlanner.CreateReviveMovementUpdatePlan` | Service / Planner | Partial | Unit Tested | Partial Parity | Staged wrapper records Java revive movement-update intent for alliance members. Live `GameServerConnection.HandleReviveAsync` still does not emit group/alliance movement updates. |
| `com.aionemu.gameserver.services.player.PlayerAllianceService.updateAlliance` | `PlayerAllianceMovementUpdatePlanner.CreateMovementUpdatePlan` / `CreateReviveMovementUpdatePlan` | Service / Planner | Partial | Unit Tested | Partial Parity | Test asserts all-except-subject movement intents for the revive caller. Runtime alliance membership/fanout execution remains outside this unit. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceUpdateEvent` | `PlayerAllianceMemberInfoUpdatePlan` / `PlayerAllianceEvent.Movement` | Event / Packet Plan | Partial | Unit Tested | Partial Parity | Event id and movement packet-plan surface already existed; this unit adds explicit revive caller coverage. Full packet bytes remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateReviveMovementUpdatePlan_UsesJavaPlayerReviveMovementEvent` | Unit / planner | `PlayerReviveService.revive`, `PlayerAllianceService.updateAlliance`, `PlayerAllianceUpdateEvent` | Revive movement wrapper emits `PlayerAllianceEvent.Movement` intents to all other alliance members. | Deterministic C# planner assertions from reviewed Java branch. | Does not execute live connection fanout or compare packet bytes. |
| Existing `PlayerAllianceMemberInfoTests` | Existing Unit | Alliance member-info packet and movement planner Java artifacts | Alliance member-info and movement planner suite remained stable. | 34-test focused suite passed. | Broader runtime alliance service remains partial. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Live kisk revive still does not call group/alliance movement-update planners.
- Runtime alliance fanout and socket ordering remain unverified.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 1 staged planner method
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live revive movement fanout, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: decide whether to wire live kisk revive group/alliance movement fanout now.
- If that is too broad, switch to a read-only toy-pet lifetime scheduling design note or another existing service-level revive side-effect planner.

## Suggested Acceptance Criteria

- If wiring live fanout, keep socket order explicit around `SmKiskUpdate`, revive emotion, teleport delete, channel/spawn/info/stats/motion, and future group/alliance packets.
- Use existing `PlayerGroupMovementUpdatePlanner.CreateReviveMovementUpdatePlan` and `PlayerAllianceMovementUpdatePlanner.CreateReviveMovementUpdatePlan` rather than inventing a new planner shape.
- If not wiring live fanout, document the exact blocker and next safe seam.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Live revive movement fanout design | connection handler + workflow tests | Medium-High | Sequential only if attempted. |
| B | Toy-pet lifetime schedule observability design | `ToyPetSpawnAction`, `Kisk`, connection fixture | Medium | Current fixture lacks schedule metadata; read-only first. |
| C | Existing revive side-effect planner audit | revive/resource/group/alliance planner tests | Low | Keep to service-level tests if live wiring is too broad. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` revive changes.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1455] Add alliance revive movement planner`.
- C# files changed in UOW-1455:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceMovementUpdatePlanner.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceMemberInfoTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKE-Completion.md`
