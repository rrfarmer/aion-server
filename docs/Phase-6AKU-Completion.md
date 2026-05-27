# Phase 6AKU Completion - Kisk Offline Bind Cleanup

Date: 2026-05-27
Unit of Work: UOW-1471
Status: Complete after validation.

## Scope

Cover Java `KiskService.removeKisk` offline bind cleanup for current kisk members.

## Completed Work

- Audited Java `KiskService.removeKisk`.
- Added `TryRemoveKiskRemovesOfflineBindingsForCurrentMembersOnlyLikeJavaRemoveKisk`.
- The test registers offline bindings for two current kisk members and one stale nonmember.
- It removes the kisk and verifies current-member offline bindings are gone, while the stale nonmember binding expires on restore because the kisk no longer resolves.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskRegistryTests"`.
- Result: passed 6 tests.

## Migration Parity Table - UOW-1471

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskRegistry.TryRemoveKisk` | Service / Registry | Partial | Unit Tested | Partial Parity | Test covers `removeKisk` offline-bind cleanup for current member ids. C# stale nonmember offline bindings expire on later restore because the removed kisk no longer resolves; Java's map only removes entries for current members. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` | Runtime State / World Object | Partial | Unit Tested | Partial Parity | Test uses current member ids as the source of offline-bind cleanup, matching Java `kisk.getCurrentMemberIds()`. Other kisk behaviors such as broadcast update, use-mask binding, and live packets remain broader. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player.BoundKiskObjectId` / `PlayerKiskOfflineBindingRestoreResult` | Model / Bind State | Partial | Unit Tested | Needs Verification | Offline restore state is modeled by object ids rather than Java direct `Kisk` references. Live login/logout persistence and packet fanout remain separate. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryRemoveKiskRemovesOfflineBindingsForCurrentMembersOnlyLikeJavaRemoveKisk` | Unit / registry | `KiskService.removeKisk`, `Kisk.getCurrentMemberIds` | Removing a kisk clears offline bindings for current member ids and leaves a stale nonmember binding to expire when restored. | Deterministic registry state regression based on Java source audit. | Does not execute online packet fanout or Java runtime comparison. |
| Existing `PlayerKiskRegistryTests` | Existing Unit | `KiskService.regKisk`, `haveKisk`, `onLogin`, `onLogout`, `Kisk.resurrectionUsed` slices | Existing owner lookup, runtime state, offline restore, and removed-kisk restore tests remained stable. | Focused 6-test suite passed. | Full Java `Kisk` object reference behavior remains broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Online member bind-point reset and death-option packet fanout are covered elsewhere but not by this registry test.
- C# offline binding uses kisk object ids instead of Java direct `Kisk` references.
- Kisk `addPlayer` / `removePlayer` broadcast update nuances remain broader.
- Live player aggro mutation remains blocked by the missing C# player-owned aggro list.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 registry regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full live kisk packet fanout, direct Java object-reference semantics, live player aggro mutation
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue kisk bind/member cleanup with online member packet/order fanout review, or move to kisk add/remove member update fanout.
- Preferred next kisk slice: audit Java `Kisk.addPlayer`, `Kisk.removePlayer`, and `Kisk.broadcastKiskUpdate` against C# `PlayerKiskUpdateFanoutService` and existing kisk dialog/bind tests.

## Suggested Acceptance Criteria

- Verify direct member update fanout versus visible same-race update fanout.
- Preserve current kisk revive/death workflow fixture behavior.
- Keep production changes narrow; add tests first unless a mismatch is exposed.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java kisk add/remove fanout audit | `Kisk.java`, `KiskService.java` | Low | Safe read-only sub-agent candidate. |
| B | C# update fanout service regression | `PlayerKiskUpdateFanoutServiceTests.cs` or related test file | Low-Medium | Keep separate from revive workflow fixture. |
| C | Online removal packet/order review | runtime cleanup tests | Medium | Sequential if touching shared workflow tests. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate from kisk bind/member cleanup. |

## Do Not Parallelize

- Shared kisk runtime cleanup or revive workflow fixture changes.
- Shared active connection fixture changes.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1471] Cover kisk offline bind cleanup`.
- Files changed in UOW-1471:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskRegistryTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKU-Completion.md`
