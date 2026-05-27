# Phase 6AKY Completion - Kisk Removal Runtime Packet Order

Date: 2026-05-27
Unit of Work: UOW-1475
Status: Complete after validation.

## Scope

Strengthen runtime kisk removal coverage by asserting the packet order emitted during online member cleanup.

## Completed Work

- Re-used `HandleDeathAsync_RemovesRuntimeKiskAndRunsMemberCleanup`.
- Added captured packet-order assertions for runtime removal cleanup.
- Verified C# sends creator `SmKiskUpdate` first, then creator and member `SmBindPointInfo`, then dead-member `SmDie`.
- Left production code unchanged.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~WorldNpcDeathDropWorkflowServiceTests.HandleDeathAsync_RemovesRuntimeKiskAndRunsMemberCleanup"`.
- Result: passed 1 test.

## Migration Parity Table - UOW-1475

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskRemovalRuntimeCleanupService` / `WorldNpcDeathDropWorkflowService` | Service / Workflow | Partial | Regression Tested | Partial Parity | Runtime workflow test now asserts final creator `SmKiskUpdate` precedes member bind-point resets and dead-member revive refresh, matching Java `removeKisk` source order. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `SmBindPointInfo` send path in `PlayerKiskRemovalRuntimeCleanupService` | Service / Packet Adapter | Partial | Regression Tested | Partial Parity | Test verifies bind-point reset packet recipients and order after creator update. Packet payload remains C# static-data based and not Java-runtime compared. |
| `com.aionemu.gameserver.controllers.PlayerController` | `SmDie` refresh in `PlayerKiskRemovalRuntimeCleanupService` | Controller / Packet Adapter | Partial | Regression Tested | Partial Parity | Test verifies dead member receives `SmDie` after bind-point reset, matching Java `member.getController().showResurrectionOptions()` after `member.setKisk(null)`. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `PlayerKiskDespawnResult` | Runtime State / World Object | Partial | Regression Tested | Partial Parity | Runtime test uses removed kisk member ids and owner id to drive cleanup. Java synchronized collection semantics and live object references remain broader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` | Packet | Partial | Regression Tested | Needs Verification | Runtime send order is covered, but packet bytes were not compared to Java runtime output in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIE` | `SmDie` | Packet | Partial | Regression Tested | Needs Verification | Packet type and order covered; full resurrection-option payload parity remains broader. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDeathAsync_RemovesRuntimeKiskAndRunsMemberCleanup` | Regression / workflow | `KiskService.removeKisk`, `TeleportService.sendKiskBindPoint`, `PlayerController.showResurrectionOptions` | Runtime kisk removal sends creator update first, then creator/member bind-point resets, then dead-member `SmDie`; existing state cleanup counts remain covered. | Deterministic packet-order assertion against the C# runtime workflow from Java source audit. | Does not compare Java-generated packet bytes or real socket timing. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Packet byte parity for `SmKiskUpdate`, `SmBindPointInfo`, and `SmDie` was not compared to Java output in this unit.
- C# defensive cleanup still includes players bound by object id even if absent from removed kisk member ids.
- Java direct `Player.kisk` object references differ from C# `BoundKiskObjectId` cleanup.
- Offline login restore and duplicate bind socket behavior remain separate kisk lifecycle slices.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit; 1 workflow packet-order regression strengthened
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, packet byte comparison, direct Java object-reference semantics, offline restore/duplicate bind socket slices
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: offline login restore packet-order audit.
- Preferred next slice: compare Java `KiskService.onLogin`, `PlayerEnterWorldService`, and `TeleportService.sendKiskBindPoint` ordering against C# enter-world restored-kisk sends.

## Suggested Acceptance Criteria

- Identify whether an existing enter-world fixture can capture `SmKiskUpdate` and `SmBindPointInfo` order without broad setup.
- Add one focused regression if fixture cost stays low.
- Document whether C# sends bind-point info before or after restored kisk update and how that maps to Java `onLogin` plus enter-world ordering.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Enter-world restored-kisk packet audit | connection enter-world tests | Medium | Fixture-heavy; keep sequential. |
| B | Duplicate bind socket response audit | bind response tests | Medium | Separate interactive bind path. |
| C | Kisk packet byte comparison notes | packet tests / docs | Low | Blocked from Java runtime verification locally. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate blocked workstream. |

## Do Not Parallelize

- Shared enter-world connection fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1475] Cover kisk removal packet order`.
- Files changed in UOW-1475:
  - `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcDeathDropWorkflowServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKY-Completion.md`
