# Phase 6AKW Completion - Kisk Duplicate Authorization Guard

Date: 2026-05-27
Unit of Work: UOW-1473
Status: Complete after validation.

## Scope

Cover the duplicate-first kisk bind guard that prevents full-kisk responses from masking an already registered player, and document how the C# interactive bind path differs from Java's lower-level `Kisk.addPlayer` duplicate packet branch.

## Completed Work

- Re-audited Java `KiskAI.handleDialogStart`, `KiskService.onBind`, and `Kisk.addPlayer` behavior from the current Phase 6 handoff.
- Re-audited C# `PlayerKiskDialogService`, `PlayerKiskAuthorizationService`, `PlayerKiskBindService`, and `PlayerKiskRegistry.RestoreOfflineBinding`.
- Added `ValidateBindPrioritizesAlreadyRegisteredBeforeCapacityLikeJavaDialogGuard`.
- Verified duplicate players return `AlreadyRegistered` before full-kisk rejection, including C#'s defensive member-id-only duplicate case.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskAuthorizationServiceTests"`.
- Result: passed 4 tests.

## Migration Parity Table - UOW-1473

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.handler.KiskAI` | `PlayerKiskDialogService` / `PlayerKiskAuthorizationService` | AI Handler / Dialog Authorization | Partial | Unit Tested | Partial Parity | Test covers duplicate-first guard ordering before full-kisk rejection. Java checks `player.getKisk() == getOwner()` in dialog; C# accepts both bound object-id and runtime member-id duplicates as already registered. |
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskBindService` / `PlayerKiskRegistry.RestoreOfflineBinding` | Service | Partial | Unit Tested | Needs Verification | Interactive C# bind flow rejects duplicates before mutation; Java direct `Kisk.addPlayer` duplicate branch sends `SM_KISK_UPDATE` and sets the direct player kisk reference if called. Offline restore covers existing-member pointer restoration separately. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` | Runtime State / World Object | Partial | Unit Tested | Needs Verification | Runtime member ids participate in C# duplicate authorization and offline restore. Java uses synchronized member sets and direct `Kisk` object references; threading and direct reference semantics remain different and need live verification. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` | Packet | Partial | Regression Tested | Needs Verification | No new packet serialization test in this unit. The duplicate direct-only update branch remains unverified at live socket level because normal dialog authorization blocks duplicates first. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ValidateBindPrioritizesAlreadyRegisteredBeforeCapacityLikeJavaDialogGuard` | Unit / authorization | `KiskAI.handleDialogStart`, `Kisk.canBind`, `Kisk.addPlayer` audit | Already-registered players return `AlreadyRegistered` before a full-kisk response, including C#'s defensive member-id-only duplicate case. | Deterministic unit regression and Java source audit of duplicate-before-canBind/full order. | Does not execute Java runtime, socket sends, or the direct Java `Kisk.addPlayer` duplicate packet branch. |
| Existing `PlayerKiskAuthorizationServiceTests` | Existing Unit | `Kisk.canBind` use-mask logic | Existing race, legion, solo, unrestricted, group, and alliance use-mask tests remained stable. | Focused 4-test suite passed. | Group/alliance live resolver parity depends on runtime team state wiring. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Normal C# dialog/authorization flow rejects duplicate kisk binds before `PlayerKiskBindService.Bind`, while Java `Kisk.addPlayer` still contains a direct duplicate packet branch; reachability remains Needs Verification.
- C# runtime member-id duplicate detection is a defensive extension beyond Java's dialog `player.getKisk() == getOwner()` check.
- Java synchronized member sets and direct object references differ from C# concurrent dictionaries and object-id lookups.
- Live socket packet ordering for duplicate bind, offline login restore, and kisk removal remains broader.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 authorization regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, direct duplicate `SM_KISK_UPDATE` reachability, live socket duplicate/removal fanout, direct Java object-reference semantics
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: online kisk removal packet/order fanout review.
- Preferred next slice: compare Java `KiskService.removeKisk`, `Kisk.removeAll`, `TeleportService.sendKiskBindPoint`, and death-option refresh ordering against C# runtime cleanup and connection tests.

## Suggested Acceptance Criteria

- Identify the current C# packet/order coverage for kisk removal.
- Add one focused regression for any missing creator/member update ordering or bind-point reset behavior.
- Keep live aggro mutation out of scope unless the missing player-owned aggro list is introduced intentionally.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java removal packet/order audit | `KiskService.java`, `Kisk.java`, teleport/death-option callers | Low | Read-only candidate. |
| B | C# runtime cleanup coverage | kisk removal cleanup / workflow tests | Medium | Sequential if touching shared fixtures. |
| C | Offline login restore packet audit | enter-world connection tests | Medium | Separate from removal if socket fixture work grows. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate blocked workstream. |

## Do Not Parallelize

- Shared connection fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1473] Cover kisk duplicate authorization guard`.
- Files changed in UOW-1473:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskAuthorizationServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKW-Completion.md`
