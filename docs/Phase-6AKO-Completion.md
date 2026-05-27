# Phase 6AKO Completion - Multi-Viewer Kisk Delete Fanout

Date: 2026-05-27
Unit of Work: UOW-1465
Status: Complete after validation.

## Scope

Cover active socket-server kisk/NPC visibility deletion fanout across multiple registered viewers.

## Completed Work

- Expanded the `GameClientSocketServerNpcVisibilityTests` loopback fixture to create multiple active server-side connections.
- Preserved the existing single-connection `fixture.Connection` path for earlier regressions.
- Added connection-indexed packet observation so tests can assert which active viewer received each packet.
- Added `RefreshNpcVisibilityAsync_DeletesRemovedKiskOnlyForViewerThatKnewIt`.
- The test primes a near viewer and a far viewer with one kisk NPC snapshot, verifies only the near viewer sees `SmNpcInfo`, refreshes with an empty snapshot, and verifies only that same near viewer receives `SmDelete`.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameClientSocketServerNpcVisibilityTests"`.
- Result: passed 8 tests.

## Migration Parity Table - UOW-1465

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList` | `NpcVisibilityService` / `GameClientSocketServer.RefreshNpcVisibilityAsync` | Service / Known List | Partial | Integration Tested | Partial Parity | Active socket regression proves disappeared kisk/NPC delete fanout is viewer-specific: only a player that previously knew the object receives `SmDelete`. Java still includes bidirectional known-list state and `notifyNotKnow` callbacks that are not modeled here. |
| `com.aionemu.gameserver.controllers.PlayerController` | `GameClientSocketServer.RefreshNpcVisibilityAsync` | Controller / Packet Fanout | Partial | Integration Tested | Partial Parity | Test covers per-player `see`/`notSee` packet fanout through active registered connections. Java spawned-state, teleporting, and controller callback conditions remain broader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO` | `SmNpcInfo` | Packet | Partial | Integration Tested | Partial Parity | Test asserts only the near active viewer receives appeared NPC info. It does not compare generated Java packet bytes. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE` | `SmDelete` | Packet | Partial | Integration Tested | Partial Parity | Test serializes the emitted delete and validates object id plus default fade-out animation. Java runtime byte comparison remains blocked. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `WorldNpc` / `PlayerKiskRuntimeState` | Runtime State / World Object | Partial | Integration Tested | Partial Parity | Kisk is represented as a kisk NPC id in the visibility refresh. Bind/member cleanup and full `KiskService.removeKisk` side effects are outside this unit. |
| `com.aionemu.gameserver.world.World` / visibility range dependencies | `WorldVisibility` | Utility / Visibility Predicate | Partial | Integration Tested | Needs Verification | Test depends on near/far visibility filtering. Exact Java map-region visibility, lock ordering, and region-boundary behavior are not fully modeled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RefreshNpcVisibilityAsync_DeletesRemovedKiskOnlyForViewerThatKnewIt` | Integration / socket-server | `KnownList.clear`, `KnownList.del`, `PlayerController.see`, `PlayerController.notSee` | Two active viewers are refreshed against one kisk; only the near viewer receives `SmNpcInfo`, and after removal only that near viewer receives `SmDelete`. | Deterministic C# loopback packet capture with per-connection observation and serialized delete payload inspection. | No Java runtime output comparison; exact region locks, spawned/teleport state, and bidirectional known-list callbacks remain unverified. |
| Existing `GameClientSocketServerNpcVisibilityTests` | Existing Unit / Integration | Kisk `SM_NPC_INFO`, `SM_DELETE`, known-list deltas | Existing kisk creature-type, ordering, removal, and no-active-player regressions remained stable. | Focused 8-test suite passed. | Full Java lifecycle and runtime packet artifacts remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# delete fanout comes from snapshot/delta state, while Java clears retained object known-lists during world despawn.
- Java `KnownList` bidirectional cleanup and `notifyNotKnow` callbacks remain broader than the C# NPC object-id tracker.
- Exact Java map-region visibility and lock ordering remain unverified.
- Player-owned aggro cleanup remains blocked by the missing C# `PlayerAggroList` equivalent.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 multi-viewer active socket regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full Java known-list callback lifecycle, exact map-region visibility, player-owned aggro model
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk visibility/deletion parity with objects-known-but-not-visible behavior, or switch to the dedicated player-owned aggro model design UOW.
- Preferred next visibility slice: prove that a viewer with an NPC tracked as known but not currently visible does not receive a delete packet when the NPC disappears, matching Java `KnownList.del` only calling `notSee` when the known object was visible.

## Suggested Acceptance Criteria

- If continuing visibility deletion, add a focused service or active socket regression for known-but-not-visible removal behavior.
- Keep shared active connection fixture changes sequential.
- Avoid production changes unless a mismatch is exposed.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Known-but-not-visible NPC removal regression | `NpcVisibilityServiceTests.cs` or `GameClientSocketServerNpcVisibilityTests.cs` | Low-Medium | Service test is lower risk; active socket test gives stronger end-to-end evidence. |
| B | Read-only Java `KnownList` visibility-state audit | Java known-list/controller files only | Low | Safe sub-agent candidate if exact visible-vs-known state needs confirmation. |
| C | Player-owned aggro model design | future docs/model/service files | High | Separate blocker for revive aggro cleanup; do not mix with socket fixture edits. |
| D | Kisk bind/member cleanup review | kisk service tests/docs | Medium | Keep separate from active visibility fixture work. |

## Do Not Parallelize

- Shared active connection fixture changes.
- Shared kisk workflow or flight-zone fixtures.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1465] Cover multi-viewer kisk delete fanout`.
- Files changed in UOW-1465:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameClientSocketServerNpcVisibilityTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKO-Completion.md`
