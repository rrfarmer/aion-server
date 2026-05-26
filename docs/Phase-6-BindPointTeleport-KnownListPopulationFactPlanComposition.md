# Phase 6 Bind-Point Teleport Known-List Population Fact-Plan Composition

Date: May 26, 2026
Unit of Work: UOW-1274
Scope: Compose disabled packet construction fact plans into known-list population candidate metadata.
Source of truth: Java project.

## Summary

UOW-1274 extends `PlayerKnownListPopulationPlanService` so candidate plans can carry per-direction packet construction fact-plan metadata and use completed fact plans to populate packet construction facts.

This remains non-live. It does not execute Java `KnownList`, call `PlayerController.see` / `notSee`, send packets, mutate world known-lists, hydrate live effects, or wire `GameServerConnection`.

## Java Source Findings

- Java `KnownList.findVisibleObjects` performs candidate-side add before owner-side add.
- Java `KnownList.updateVisibility` emits `see` / `notSee` side effects only after cached visibility changes.
- Java `PlayerController.sendPlayerInfoPackets` owns the viewer-subject packet sequence.
- Java packet construction is directional: owner viewing candidate and candidate viewing owner have different viewers, subjects, aggro/enemy facts, and packet inputs.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`:

- `PlayerKnownListPopulationCandidateFact` can now carry:
  - `OwnerViewingCandidatePacketFactPlanRequest`;
  - `CandidateViewingOwnerPacketFactPlanRequest`.
- Added `PlayerKnownListPopulationPacketConstructionFactPlanDirection`.
- Added `PlayerKnownListPopulationPacketConstructionFactPlanAttachment`.
- `PlayerKnownListPopulationCandidatePlan` can now expose `SideEffectFactPlans`.
- `PlayerKnownListPopulationPlanService` composes `PlayerKnownListPacketConstructionFactPlanService` per candidate direction.
- Completed fact plans supplement the packet construction fact dictionary.
- Existing request-level `PacketConstructionFactsByPlayerObjectId` remains authoritative and is not overwritten by generated fact-plan facts.
- Blocked fact plans remain visible on candidate plans and produce partial packet construction metadata where attached side effects lack facts.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListOperationSideEffectPacketConstructionServiceTests" --nologo` passed 20 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 289 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Parallel Work

Parallel work discovery was performed before and during implementation.

| Agent | Task | Result |
|---|---|---|
| Explorer | Read-only audit of population fact-plan composition points and ordering | Completed with no file edits; identified request-level facts as authoritative over generated fact plans. Agent was closed. |
| Orchestrator | Implement population fact-plan composition, tests, docs, and commit | Completed locally because production/test files were shared and needed exclusive ownership. |

## Migration Parity Table - UOW-1274

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Known-List Population Composition | Partial | Unit Tested | Partial Parity | Candidate plans now carry directional packet fact-plan metadata and can feed completed supplied-snapshot facts into packet construction. No live region scan, known-list add, synchronized update, or world mutation occurs. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `PlayerKnownListPopulationCandidatePlan.SideEffectFactPlans`; `SideEffectPacketConstructionPlan` | Visibility Side-Effect Metadata | Partial | Unit Tested | Partial Parity | Directional see/notSee packet facts are composed after attachment planning and preserve operation-step order. Cached visibility transitions and live controller callbacks remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPacketConstructionFactPlanService` through population composition | Controller Packet Fact Planning | Partial | Unit Tested | Partial Parity | Population composition can derive construction facts from supplied viewer/subject snapshots per direction. It does not read live `AionConnection`, compute enemy/neutral state, or send packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `SmPlayerInfoViewerContext` through population fact-plan composition | Viewer-Sensitive Packet Fact | Partial | Unit Tested | Needs Verification | Viewer context is supplied in fact-plan requests. No live active-player connection lookup or Java runtime comparison exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | generated or request-level `PlayerKnownListOperationSideEffectPacketConstructionFacts` | Ride Packet Fact Source | Partial | Unit Tested | Needs Verification | Request-level facts remain authoritative over generated facts; generated ride facts still require supplied attack speed and supplied/snapshot ride state. Live stat hydration remains missing. |
| `com.aionemu.gameserver.controllers.effect.EffectController` / `SM_ABNORMAL_EFFECT` | generated or request-level abnormal-effect facts | Effect Packet Fact Source | Partial | Unit Tested | Needs Verification | Blocked fact plans remain visible when abnormal-effect entries or masks are missing. Live effect map, no-show filtering, slots, timers, and Java runtime comparison remain missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_ComposesFactPlansIntoPopulationPacketConstructionMetadata` | Unit / Composition | `KnownList.findVisibleObjects`; `PlayerController.sendPlayerInfoPackets` | Per-direction fact plans create construction facts and packet metadata for candidate-see-owner and owner-see-candidate in operation order. | Source-derived directional ordering and packet type assertions. | Supplied snapshots only; no live controller dispatch or Java runtime capture. |
| `Plan_PreservesBlockedFactPlanMetadataAndPartialPacketConstruction` | Unit / Guard | Java ride/stat facts are live runtime dependencies | Blocked fact plans remain on candidate metadata, and packet construction records partial missing-subject facts for affected direction. | Source-derived required input boundaries. | Java uses live state rather than blockers. |
| `Plan_RequestLevelPacketFactsRemainAuthoritativeOverGeneratedFactPlans` | Unit / Regression | C# explicit fact input boundary plus Java packet speed field | Existing request-level facts are not overwritten by generated fact-plan facts. | C# packet payload speed assertion against explicit fact value. | No Java runtime golden packet; explicit C# precedence is a staging rule. |

## Remaining Risks

- Population fact-plan composition is non-live and unwired from actual scheduled fanout.
- Viewer enemy/neutral facts are still supplied.
- Attack speed, abnormal-effect entries, masks, slots, and remaining time are still supplied or blocked.
- Request-level fact precedence is a C# staging rule to preserve existing callers, not a Java runtime behavior.
- Threading and locking remain metadata-only; Java `KnownList` and `EffectController` synchronization are not reproduced.
- Serialization evidence is via C# packet constructors and source-derived tests, not Java golden captures.
- Date/time behavior for motion/effect remaining time remains unverified.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 population fact-plan composition extension plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live active-viewer context adapter, 1 reusable stat resolver, 1 live motion timing verifier, 1 live effect-controller entry/timer hydrator, 1 live known-list packet dispatcher, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled population packet-construction summary/diagnostic projection that aggregates completed, partial, and blocked fact-plan/packet-construction results across candidate plans. Keep it metadata-only and do not send packets.

## Update After UOW-1275

The disabled diagnostic projection now exists as `PlayerKnownListPopulationPacketConstructionDiagnosticService`. It aggregates fact-plan, blocker, operation packet-construction, operation-result, player-packet-result, and constructed descriptor-kind counts without live dispatch.
