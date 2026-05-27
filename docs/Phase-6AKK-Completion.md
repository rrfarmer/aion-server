# Phase 6AKK Completion - Known Kisk Visibility Delete Candidate

Date: 2026-05-27
Unit of Work: UOW-1461
Status: Complete after validation.

## Scope

Anchor the known-list service behavior that lets kisk removal refreshes produce viewer-specific `SM_DELETE` candidates.

## Completed Work

- Audited `GameClientSocketServer.RefreshNpcVisibilityAsync`, which maps `NpcVisibilityService` disappeared NPC ids to `SmDelete`.
- Added a focused `NpcVisibilityService` regression for a removed kisk/NPC snapshot.
- Verified only the viewer that previously knew the kisk receives a disappeared object id after the current NPC snapshot no longer contains that object.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~NpcVisibilityServiceTests|FullyQualifiedName~GameClientSocketServerNpcVisibilityTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1461

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList` | `NpcVisibilityService` | Service / Known List | Partial | Unit Tested | Partial Parity | New test verifies per-viewer known-NPC state: a removed kisk snapshot produces a disappeared object id only for a viewer that previously saw the kisk. Java known-list locking, object templates, and update cadence remain broader. |
| `com.aionemu.gameserver.controllers.PlayerController` | `GameClientSocketServer.RefreshNpcVisibilityAsync` | Controller / Packet Fanout | Partial | Existing Unit Coverage | Partial Parity | Socket server maps disappeared NPC ids to `SmDelete`, but this unit only tests the service delta precondition; full active-connection packet send order remains unverified. |
| `com.aionemu.gameserver.controllers.VisibleObjectController` | `PlayerKiskRemovalRuntimeCleanupService` / `NpcVisibilityService` | Controller / World Removal | Partial | Unit Tested | Partial Parity | The service test supports the removal path where the current world NPC snapshot no longer contains the kisk. C# still lacks Java's full controller deletion hierarchy. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `WorldNpc` | Runtime State / World Object | Partial | Unit Tested | Partial Parity | The test uses a generic NPC object as the known-list subject because `NpcVisibilityService` tracks NPC object ids independent of kisk runtime metadata. Kisk-specific creature-type packet bytes are covered elsewhere. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `UpdateKnownNpcs_RemovedKiskSnapshotOnlyDeletesForViewerThatKnewIt` | Unit / service | `KnownList.updateKnownList`, `PlayerController.notSee`, `VisibleObjectController.delete` | A removed kisk snapshot emits the kisk object id in `DisappearedObjectIds` for the near viewer that previously knew it and emits no delete candidate for a distant viewer that never saw it. | Deterministic C# known-list delta assertion from reviewed socket-server `SM_DELETE` mapping. | Does not instantiate `GameClientSocketServer` active connections or byte-compare `SM_DELETE`. |
| Existing `GameClientSocketServerNpcVisibilityTests` | Existing Unit | `PlayerController.see`, kisk creature-type projection | Viewer-specific kisk `SmNpcInfo` packet construction remained stable. | Focused suite passed. | Full refresh send ordering remains broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full socket-level `SM_DELETE` delivery from active `GameClientSocketServer.RefreshNpcVisibilityAsync` remains unverified.
- The regression uses generic NPC visibility state; kisk runtime registry deletion and exact `SM_DELETE` serialization are covered only indirectly.
- C# known-list service is simpler than Java's full known-list implementation and does not model all object categories or controller callbacks.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 known-list regression slice added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, active socket-server `SM_DELETE` integration, full Java known-list/controller lifecycle, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: build a focused active-connection NPC visibility refresh integration test for `SM_NPC_INFO`/`SM_DELETE` ordering.
- Alternative: switch to the dedicated player-owned aggro model design UOW to unblock revive aggro cleanup.

## Suggested Acceptance Criteria

- If continuing socket-server visibility, create the smallest active-player connection fixture possible and prove refresh sends `SmNpcInfo` for appeared NPCs and `SmDelete` for disappeared NPCs in the current C# order.
- Keep production changes narrow unless the integration test exposes a mismatch.
- If designing player aggro, keep it separate from revive live wiring.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Active socket-server NPC refresh integration | socket-server visibility tests | Medium | Sequential if touching live connection fixture code. |
| B | Player aggro model design | docs/model design, future service skeleton | High | Do not combine with revive live wiring. |
| C | Read-only Java known-list/controller audit | Java known-list/controller files only | Low | Safe read-only sub-agent candidate if tools are available. |

## Do Not Parallelize

- Shared `GameClientSocketServer` connection registration or active-player test fixture changes.
- Shared kisk workflow or flight-zone fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1461] Cover known kisk visibility deletion`.
- Files changed in UOW-1461:
  - `dotnetConversion/tests/Aion.GameServer.Tests/NpcVisibilityServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKK-Completion.md`
