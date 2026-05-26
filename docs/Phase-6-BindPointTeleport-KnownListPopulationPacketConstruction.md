# Phase 6 Bind-Point Teleport Known-List Population Packet Construction

Date: May 26, 2026
Unit of Work: UOW-1271
Scope: Carry operation-level player side-effect packet construction metadata through non-live known-list population candidate plans.
Source of truth: Java project.

## Summary

UOW-1271 extends `PlayerKnownListPopulationPlanService` so candidate plans can optionally carry operation-level packet construction results for their attached `see` / `notSee` side effects.

This remains metadata-only. The service does not send packets, execute Java `KnownList`, mutate live world known-lists, hydrate live player/effect/motion/ride facts, call `PacketSendUtility`, or wire `GameServerConnection`.

## Java Source Findings

- Java `KnownList.update()` processes `forgetObjectsOrUpdateVisibility()` before `findVisibleObjects()`.
- Java `findVisibleObjects()` preserves candidate-first two-way add ordering: `newObject.getKnownList().add(owner)` before `add(newObject)`.
- Java `updateVisibility()` sends `see` or `notSee` only when cached visible state changes.
- Java `del()` removes the known object, sends `notSee` when it was visible, then sends `notKnow`.
- Java `PlayerController.sendPlayerInfoPackets` constructs `SM_PLAYER_INFO`, `SM_MOTION`, optional ride `SM_EMOTION`, and optional `SM_PLAYER_STANCE`; `PlayerController.see` appends `SM_ABNORMAL_EFFECT` when effects exist.
- Java runtime packet facts come from live `Player`, motion, ride/stat, viewer, and `EffectController` state. The C# population candidate facts intentionally do not infer those values.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`:

- `PlayerKnownListPopulationPlanRequest` can now accept `PacketConstructionFactsByPlayerObjectId`.
- `PlayerKnownListPopulationCandidatePlan` can now carry `SideEffectPacketConstructionPlan`.
- `PlayerKnownListPopulationPlan` now exposes `ConstructedControllerSideEffectPackets`.
- `PlayerKnownListPopulationPlanService` composes `PlayerKnownListOperationSideEffectPacketConstructionService` for each candidate side-effect attachment when supplied subject facts are available.
- `ExecutedControllerSideEffects` remains `false`.
- `ConstructedControllerSideEffectPackets` means packet metadata was constructed or attempted; it does not mean live packets were sent.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPlanServiceTests|PlayerKnownListOperationSideEffectPacketConstructionServiceTests" --nologo` passed 12 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 281 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Parallel Work

Parallel work discovery was performed before implementation.

| Agent | Task | Result |
|---|---|---|
| Explorer | Read-only population candidate packet-fact and ordering audit | Completed with no file edits; confirmed population candidates lack runtime packet facts and that Java add/remove/visibility ordering must be preserved. Agent was closed. |
| Orchestrator | Implement population packet-construction metadata composition, tests, docs, and commit | Completed locally to avoid shared-file conflicts. |

## Migration Parity Table - UOW-1271

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Known-List Population Composition | Partial | Unit Tested | Partial Parity | Population composition can now carry operation-level packet construction metadata per candidate. It still does not execute Java synchronized update, live region storage, `forgetObjectsOrUpdateVisibility` before region scan, or controller callbacks. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `PlayerKnownListPopulationCandidatePlan.SideEffectPacketConstructionPlan` | Visibility Side-Effect Packet Metadata | Partial | Unit Tested | Partial Parity | In-range visible candidates can carry directional `see` packet construction results in Java operation-step order. Runtime facts are supplied; no live `owner.canSee`, cached visible-state transition, pet visibility cascade, or packet send occurs. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListPopulationCandidatePlan.SideEffectPacketConstructionPlan` | Removal Side-Effect Packet Metadata | Partial | Unit Tested | Partial Parity | Out-of-range visible known candidates can carry directional delete packet construction results. It does not execute Java `notKnow`, target cleanup, `ObjectDeleteAnimation` live propagation beyond supplied facts, or socket sends. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListOperationSideEffectPacketConstructionService` through population composition | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | Population plans can now construct metadata for `SmPlayerInfo`, `SmMotion`, ride `SmEmotion`, `SmPlayerStance`, `SmAbnormalEffect`, and `SmDelete` through the existing bridge. Active motions, viewer context, ride speeds, and abnormal effects are supplied. |
| `com.aionemu.gameserver.controllers.PlayerController.see` abnormal-effect tail | population packet-construction metadata plus supplied `SmAbnormalEffect` facts | Controller Packet Construction Dependency | Partial | Unit Tested | Needs Verification | Missing abnormal-effect facts remain blocked/partial metadata. Live `EffectController.isEmpty`, abnormal mask/effect collection hydration, remaining-time calculation, and slot enum parity are not ported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | known-list population candidate packet metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future fanout planning can inspect candidate-level packet metadata. Live scheduled callbacks, sockets, movement, cooldown, world mutation, runtime fact hydration, and Java runtime comparison remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_ConstructsPopulationSideEffectPacketMetadataWhenSubjectFactsAreSupplied` | Unit / Composition | `KnownList.findVisibleObjects`; `KnownList.updateVisibility`; `PlayerController.sendPlayerInfoPackets` | Population candidate carries operation packet construction metadata in Java add/see order and constructs owner/candidate packet type sequences without live sends. | Source-derived ordering and C# packet type assertions. | Supplied facts only; no Java runtime packet capture or live controller dispatch. |
| `Plan_RecordsPartialPopulationPacketMetadataWhenSubjectFactsAreMissing` | Unit / Composition Guard | C# supplied-fact boundary around Java runtime player facts | Missing candidate subject facts block only the affected directional attachment while preserving the other direction. | Guarded metadata assertion. | Java has live object references instead of supplied-fact dictionaries. |

## Remaining Risks

- Population packet construction is non-live and unwired.
- Runtime player objects, active motions, viewer context, ride stats, abnormal masks/effects, abnormal timers, and stance state remain supplied facts.
- No live Java `KnownList`, `PlayerController`, `PacketSendUtility`, `notKnow`, target cleanup, or socket dispatch occurs.
- Reflection behavior did not change.
- Threading differs from Java `synchronized` plus `ConcurrentHashMap`; C# is still metadata composition.
- Serialization is only tested through existing packet constructors; no Java runtime golden packet comparison was run.
- Precision/rounding for ride speed and date/time behavior for effect timers remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 population packet-construction composition extension plus 2 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 runtime player/motion/stat fact hydrator, 1 live effect-controller hydrator, 1 live known-list fanout executor, 1 live world region/known-list population path, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a read-only or metadata-only runtime fact hydration audit for population packet construction: identify the safest future sources for active viewer context, active motions, ride movement/stat facts, stance state, and abnormal-effect facts before any live socket dispatch is attempted.

## Update After UOW-1272

The runtime fact hydration audit is complete. It confirms the operation packet construction facts record is correctly shaped, but automatic hydration should remain disabled until active viewer context, attack-speed stat resolution, live motion timing, ride stat hydration, stance observer lifecycle, and abnormal-effect entry/timer sources are explicitly modeled.
