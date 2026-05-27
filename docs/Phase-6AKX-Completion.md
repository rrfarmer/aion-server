# Phase 6AKX Completion - Kisk Creator Member Removal Cleanup

Date: 2026-05-27
Unit of Work: UOW-1474
Status: Complete after validation.

## Scope

Cover Java `KiskService.removeKisk` behavior when the removed kisk creator is also a current kisk member.

## Completed Work

- Re-read Java `KiskService.removeKisk` and the C# kisk removal cleanup planner/runtime cleanup services.
- Added `CreatePlanIncludesCreatorMemberBindPointResetLikeJavaRemoveKisk`.
- Verified the creator-as-member receives both final creator update intent and member bind-point reset / bound-state clear intents.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskRemovalCleanupServiceTests"`.
- Result: passed 3 tests.

## Migration Parity Table - UOW-1474

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskRemovalCleanupService` / `PlayerKiskRemovalRuntimeCleanupService` | Service / Cleanup Planner | Partial | Unit Tested | Partial Parity | Test covers `removeKisk` creator-as-member cleanup intent: final creator update plus current-member bind-point reset/clear intent. Runtime send order is implemented but not newly asserted here. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `PlayerKiskDespawnResult` | Runtime State / World Object | Partial | Unit Tested | Partial Parity | Test uses `CurrentMemberIds` captured into despawn result, matching Java `kisk.getCurrentMemberList()` iteration source. Java synchronized set/list behavior and live world object state remain broader. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player.BoundKiskObjectId` / `PendingKiskBindRequest` | Model / Bind State | Partial | Unit Tested | Needs Verification | Test asserts creator member bound state is included in clear/bind-point-reset intents. Java clears direct `Player.kisk` references; C# clears object ids during runtime cleanup. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `SmBindPointInfo` creation inside `PlayerKiskRemovalRuntimeCleanupService` | Service / Packet Adapter | Partial | Unit Tested | Needs Verification | Planner coverage proves the reset recipient; actual bind-point packet payload/order and Java runtime output are not compared in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` | Packet | Partial | Regression Tested | Needs Verification | Creator update intent is covered by planner field; no new serialization or live socket send assertion. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlanIncludesCreatorMemberBindPointResetLikeJavaRemoveKisk` | Unit / cleanup planner | `KiskService.removeKisk` | A removed-kisk creator who is also a current member gets creator update, bound-kisk clear, and bind-point reset cleanup intents. | Deterministic planner regression from reviewed Java source. | Does not execute socket sends or compare Java packet output/order. |
| Existing `PlayerKiskRemovalCleanupServiceTests` | Existing Unit | `KiskService.removeKisk` | Existing creator update, member bind-point reset, dead-member revive refresh, pending-request clear, and no-removed-kisk tests remained stable. | Focused 3-test suite passed. | C# includes defensive bound-object-id cleanup for online players not in Java's current member list. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Runtime cleanup packet order should still be asserted at the fake connection registry level: final creator `SmKiskUpdate` before creator/member bind-point reset and `SmDie` refresh.
- C# defensive cleanup includes players bound by object id even if they are absent from removed kisk member ids; this is a deliberate safety behavior but remains an intentional difference candidate until live state is verified.
- Java clears direct `Player.kisk` references; C# clears `BoundKiskObjectId` and pending request state.
- Live socket packet ordering for offline login restore and duplicate bind remains broader.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 cleanup planner regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, live cleanup socket order comparison, direct Java object-reference semantics, defensive C# bound-id cleanup difference
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add runtime cleanup packet-order coverage for kisk removal.
- Preferred next slice: use a fake connection registry to assert final creator `SmKiskUpdate` happens before bind-point reset and dead-member `SmDie` refresh during `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync`.

## Suggested Acceptance Criteria

- Keep production cleanup code unchanged unless the order test exposes a mismatch.
- Assert recipient-level packet ordering for creator/member cleanup where the fake registry can observe it.
- Keep visibility refresh and zone-counter cleanup out of scope unless already required by the fixture.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime cleanup fake-registry order test | kisk removal runtime cleanup tests | Medium | Sequential if creating shared fake registry helpers. |
| B | Offline login restore packet-order audit | enter-world connection tests | Medium | Separate login path. |
| C | Duplicate bind socket response audit | bind response tests | Medium | Separate from removal cleanup. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate blocked workstream. |

## Do Not Parallelize

- Shared fake connection registry helpers.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1474] Cover kisk creator member removal cleanup`.
- Files changed in UOW-1474:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskRemovalCleanupServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKX-Completion.md`
