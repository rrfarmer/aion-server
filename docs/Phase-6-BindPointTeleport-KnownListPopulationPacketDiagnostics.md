# Phase 6 Bind-Point Teleport Known-List Population Packet Diagnostics

Date: May 26, 2026
Unit of Work: UOW-1275
Scope: Add disabled summary diagnostics for population packet-construction metadata.
Source of truth: Java project.

## Summary

UOW-1275 adds `PlayerKnownListPopulationPacketConstructionDiagnosticService`, a metadata-only projection over existing known-list population candidate plans.

The diagnostic service does not execute Java `KnownList`, call `PlayerController.see` / `notSee`, mutate known-list membership, hydrate live runtime facts, send packets, or wire `GameServerConnection`. It summarizes existing fact-plan and packet-construction metadata so future live-readiness work can see which candidates are complete, partial, blocked, or missing packet construction inputs.

## Java Source Findings

- Java `KnownList.findVisibleObjects` preserves region candidate scan order while processing candidate-first two-way add/see behavior.
- Java `KnownList.updateVisibility` and `KnownList.del` produce directional controller side effects after cached visibility transitions.
- Java `PlayerController.sendPlayerInfoPackets` emits the player info packet sequence for a viewer/subject pair.
- Java `PlayerController.see` appends abnormal-effect packets after player-info packets when effect state exists.
- Java pet visibility after `SM_PLAYER_INFO` is not represented by the current player-only C# diagnostic projection.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPacketConstructionDiagnosticService.cs`:

- candidate diagnostics preserve population candidate order;
- fact-plan diagnostics preserve per-direction fact-plan attachment order;
- packet-construction diagnostics preserve attached operation-step order;
- aggregate counters cover candidate/range/attachment counts, fact-plan complete/blocked counts, blocker counts by kind, operation packet-construction status counts, operation result counts, player-packet result counts, and constructed packet counts by descriptor kind;
- root and candidate statuses are `NoPacketConstructionMetadata`, `Complete`, `Partial`, or `Blocked`;
- `ExecutesLivePackets`, `IsLive`, and `IsJavaControllerParity` remain `false`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListOperationSideEffectPacketConstructionServiceTests" --nologo` passed 23 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 292 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Parallel Work

Parallel work discovery was performed before implementation.

| Agent | Task | Result |
|---|---|---|
| Explorer | Read-only audit of diagnostic/status fields and ordering | Completed with no file edits; recommended aggregate status/blocker/packet-kind counters and highlighted ordering risks. Agent was closed. |
| Orchestrator | Implement diagnostic projection, tests, docs, and commit | Completed locally because production/test/docs integration was shared. |

## Migration Parity Table - UOW-1275

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListPopulationPacketConstructionDiagnosticService` | Diagnostic / Known-List Population Metadata | Partial | Unit Tested | Partial Parity | Diagnostic preserves C# population candidate order and summarizes missing candidate facts/range plans. It does not execute Java region scans, synchronized known-list updates, world mutation, or live `isInRange` / `canSee` recomputation. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `PlayerKnownListPopulationPacketConstructionCandidateDiagnostic`; packet construction result diagnostics | Diagnostic / Visibility Side-Effect Metadata | Partial | Unit Tested | Partial Parity | Diagnostic exposes directional operation-step packet construction results and partial/blocked statuses. Cached visible-state transitions, pet visibility cascade, live controller callbacks, and socket sends remain missing. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListPopulationPacketConstructionResultDiagnostic` | Diagnostic / Removal Side-Effect Metadata | Partial | Unit Tested | Needs Verification | Diagnostic can summarize delete-side packet construction results when present, but no dedicated remove-case diagnostic test was added in this unit. Live `notKnow`, target cleanup, and animation propagation remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPopulationPacketConstructionDiagnosticService` over `PlayerKnownListOperationSideEffectPacketConstructionPlan` | Diagnostic / Controller Packet Metadata | Partial | Unit Tested | Partial Parity | Diagnostic counts constructed player-packet descriptor kinds and blocked player-packet result statuses without sending. Java active connection/viewer context, stat/equipment/account details, and runtime packet comparison remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController.see` abnormal-effect tail | diagnostic blocked/player-packet status counts | Diagnostic / Effect Packet Metadata | Partial | Unit Tested | Needs Verification | Diagnostic can surface blocked abnormal-effect packet construction through player-packet result status counts, but live `EffectController` entries, masks, no-show filtering, slots, timers, and Java packet order remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | constructed packet counts by `PlayerKnownListPlayerSideEffectKind.SmPlayerInfo` | Packet Diagnostic Dependency | Partial | Unit Tested | Needs Verification | Counted as descriptor metadata only. C# packet serializer coverage exists elsewhere, but this unit did not perform Java golden-byte validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION` | constructed packet counts by `PlayerKnownListPlayerSideEffectKind.SmMotion` | Packet Diagnostic Dependency | Partial | Unit Tested | Needs Verification | Counted as descriptor metadata only; Java active-motion timing/expiration remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | fact-plan blocker counts and constructed `SmEmotionRide` descriptor counts | Packet Diagnostic Dependency | Partial | Unit Tested | Needs Verification | Missing ride info and attack-speed facts are surfaced. Live ride stat hydration, precision/rounding, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | constructed packet counts by `PlayerKnownListPlayerSideEffectKind.SmPlayerStance` | Packet Diagnostic Dependency | Partial | Unit Tested | Needs Verification | Counted as descriptor metadata only. Java stance observer lifecycle remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | player-packet result status counts and future `SmAbnormalEffect` descriptor counts | Packet Diagnostic Dependency | Partial | Unit Tested | Needs Verification | Diagnostic can count constructed/blocked abnormal-effect descriptor results when present. Live effect-controller hydration and date/time remaining-time behavior remain missing. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Summarize_CompletePopulationMetadataPreservesCandidateAndOperationOrdering` | Unit / Diagnostic Projection | `KnownList.findVisibleObjects`; `KnownList.updateVisibility`; `PlayerController.sendPlayerInfoPackets` | Complete fact plans and packet construction are summarized, candidate order is preserved, operation-step order is exposed, and packet descriptor kind counts are grouped. | Source-derived ordering and C# metadata assertions. | No Java runtime packet capture, live controller dispatch, pet visibility, or socket sends. |
| `Summarize_PartialMetadataSurfacesBlockedFactPlansAndPacketConstructionResults` | Unit / Diagnostic Guard | Java ride/stat runtime dependencies for player-see packets | Missing ride info and attack-speed facts are surfaced through blocker counts while the opposite direction remains constructed. | Source-derived required input boundaries. | Java uses live state rather than diagnostic blockers. |
| `Summarize_NoPacketConstructionMetadataStillReportsPopulationShape` | Unit / Diagnostic Guard | C# non-live population snapshot boundary around Java region scan | Population shape and missing candidate facts remain visible when no packet metadata exists. | C# safety and ordering assertion. | Java would have live region/player objects, not supplied facts. |

## Remaining Risks

- Diagnostic projection is metadata only and must not be mistaken for live fanout readiness.
- Request-level packet fact override/source counts are not yet separated from generated fact-plan facts.
- Pet visibility after player info is not represented.
- Viewer enemy/neutral facts, attack speed, abnormal effects, masks, slots, remaining time, and live motion timing are still supplied or blocked.
- Threading and locking remain different from Java `KnownList` synchronization and `EffectController` state access.
- Serialization evidence is indirect; this unit did not add Java golden-byte or runtime packet comparison.
- Date/time behavior for abnormal-effect and motion remaining time remains unverified.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped artifact rows in this unit
- Total artifacts ported: 1 diagnostic projection service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: 1 live active-viewer context adapter, 1 request/generated fact source diagnostic split, 1 reusable stat resolver, 1 live motion timing verifier, 1 live effect-controller entry/timer hydrator, 1 pet visibility dispatcher, 1 live known-list packet dispatcher, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a small metadata source-origin diagnostic for population packet construction facts: distinguish request-level facts from generated fact-plan facts in population packet construction so future diagnostics can report which packet facts came from explicit caller input versus supplied-snapshot fact planning. Keep it non-live and do not send packets.

## Update After UOW-1276

Population packet-construction diagnostics now include source-origin metadata. They distinguish request-level packet facts, generated fact-plan facts that were accepted, and generated fact-plan facts ignored because request-level facts for the same subject remained authoritative. Request-level source rows are candidate-consumed diagnostics, so unrelated request-wide facts are not counted for candidates whose attached side effects did not use them.
