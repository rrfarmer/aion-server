# Phase 6AKZ Completion - Kisk Login Restore Packet Order

Date: 2026-05-27
Unit of Work: UOW-1476
Status: Complete after validation.

## Scope

Align C# offline kisk login restore direct packet order with Java `PlayerEnterWorldService` and `KiskService.onLogin`.

## Completed Work

- Re-read Java `PlayerEnterWorldService` ordering around `KiskService.onLogin`, `TeleportService.sendObeliskBindPoint`, and `TeleportService.sendKiskBindPoint`.
- Re-read Java `TeleportService.sendKiskBindPoint` and `SM_BIND_POINT_INFO`.
- Added `PlayerKiskLoginRestorePacketPlanService`.
- Wired `GameServerConnection` restored-kisk enter-world handling through the planner.
- Adjusted restored-kisk direct packets to emit `SmKiskUpdate` before obelisk and kisk `SmBindPointInfo` packets.
- Added planner tests for restored-existing, restored-added, and no-restored-kisk cases.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~PlayerKiskLoginRestorePacketPlanServiceTests"`.
- Result: passed 3 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~PlayerKiskRegistryTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests"`.
- Result: passed 11 tests.
- Ran full `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore`.
- Result: failed 2 unrelated inventory/item-use cleanup-seal tests after 3,038 tests passed; both failures reproduced when isolated.

## Migration Parity Table - UOW-1476

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` | `GameServerConnection` / `PlayerKiskLoginRestorePacketPlanService` | Service / Connection Flow | Partial | Unit Tested | Partial Parity | C# restored-kisk direct packet order now follows Java call order: `KiskService.onLogin`-driven update before obelisk/kisk bind-point packets. Full enter-world packet sequence remains broader. |
| `com.aionemu.gameserver.services.KiskService` | `PlayerKiskRegistry.RestoreOfflineBinding` / `PlayerKiskLoginRestorePacketPlanService` | Service | Partial | Unit Tested | Partial Parity | Planner covers restored existing and restored added member direct packet order. Java duplicate branch sends `SM_KISK_UPDATE`; C# also sends direct `SmKiskUpdate`, but Java runtime packet bytes and full broadcast recipient behavior remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.Kisk` | `PlayerKiskRuntimeState` / `PlayerKiskOfflineBindingRestoreResult` | Runtime State / World Object | Partial | Unit Tested | Needs Verification | Restore result captures existing-vs-added member state by object id. Java uses direct `Kisk` references and synchronized member collection semantics. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `PlayerKiskLoginRestorePacketPlanService` / `SmBindPointInfo` | Service / Packet Planner | Partial | Unit Tested | Partial Parity | Planner emits obelisk bind-point before kisk bind-point, matching Java `sendObeliskBindPoint` then `sendKiskBindPoint`. Payload byte comparison remains covered only by generic packet tests, not Java runtime output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_INFO` | `SmBindPointInfo` | Packet | Partial | Unit Tested | Needs Verification | Packet type/order covered for login restore; byte serialization has generic C# tests but no Java-generated restored-login packet comparison in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE` | `SmKiskUpdate` | Packet | Partial | Unit Tested | Needs Verification | Direct restored-kisk packet order covered; packet bytes and live Java output remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlanOrdersRestoredKiskUpdateBeforeBindPointsLikeJavaEnterWorld` | Unit / packet planner | `PlayerEnterWorldService`, `KiskService.onLogin`, `TeleportService` | Restored existing kisk emits `SmKiskUpdate`, obelisk bind point, then kisk bind point. | Deterministic packet-order regression from Java source audit. | Does not run full enter-world socket flow or compare Java packet bytes. |
| `CreatePlanBroadcastsAddedMemberAfterDirectJavaLoginPackets` | Unit / packet planner | `Kisk.addPlayer`, `PlayerEnterWorldService` | Restored added-member case still emits direct packets in Java login order and exposes a broadcast intent. | Deterministic planner regression. | Java `broadcastKiskUpdate` recipient nuances remain covered by separate fanout tests, not this planner. |
| `CreatePlanSendsOnlyObeliskBindPointWhenNoKiskWasRestored` | Unit / packet planner | `PlayerEnterWorldService`, `TeleportService.sendObeliskBindPoint` | No restored kisk sends only the obelisk bind-point packet. | Deterministic planner regression. | Full no-kisk enter-world sequence remains broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite currently has two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Full enter-world packet sequence was not replayed through `CmEnterWorld`; this unit verifies the extracted restored-kisk packet planner and production wiring.
- C# still sends a direct `SmKiskUpdate` for restored added-member cases before broadcasting with the current fanout planner; Java `Kisk.addPlayer` enters `broadcastKiskUpdate` for added members. Recipient-level added-member restore remains Needs Verification.
- Packet byte parity for restored-login `SmKiskUpdate` and `SmBindPointInfo` was not compared against Java output.
- Direct Java `Player.kisk` object references differ from C# `BoundKiskObjectId` / runtime registry references.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 packet planner service added and 1 enter-world sequence adjusted
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java runtime artifact generation, full enter-world replay comparison, restored-added fanout recipient comparison, packet byte comparison, direct Java object-reference semantics, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: duplicate bind socket response audit.
- Preferred next slice: inspect whether C# interactive duplicate bind responses should stay as `BindstoneAlreadyRegistered` guards or whether a stale/member-only state needs a direct `SmKiskUpdate` recovery path modeled after Java `Kisk.addPlayer` duplicate branch.

## Suggested Acceptance Criteria

- Re-audit Java `KiskAI.handleDialogStart`, `KiskService.onBind`, and `Kisk.addPlayer`.
- Re-audit C# `HandleKiskBindQuestionResponseAsync`, `PlayerKiskAuthorizationService`, and `PlayerKiskBindService`.
- Add a focused regression if there is an untested stale/member-only branch.
- Document intentional differences if the current C# guard is retained.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Duplicate bind socket response audit | bind response tests/services | Medium | Sequential if touching connection fixture or authorization services. |
| B | Restored-added fanout recipient audit | kisk login restore planner/fanout tests | Medium | Separate from duplicate interactive bind path but touches nearby kisk planner files. |
| C | Kisk packet byte comparison notes | packet tests/docs | Low | Blocked from Java runtime verification locally. |
| D | Live player aggro list design | future aggro model/service/test files | High | Separate blocked workstream. |

## Do Not Parallelize

- Shared connection fixture edits.
- Shared kisk bind/restore planner edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1476] Align kisk login restore packet order`.
- Files changed in UOW-1476:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKiskLoginRestorePacketPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKiskLoginRestorePacketPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKZ-Completion.md`
