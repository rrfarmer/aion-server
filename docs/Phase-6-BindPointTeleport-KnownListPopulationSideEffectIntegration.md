# Phase 6 Bind-Point Teleport Known-List Population Side-Effect Integration

Date: May 26, 2026
Unit of Work: UOW-1264
Scope: Carry operation side-effect attachment metadata through non-live player known-list population plans.
Source of truth: Java project.

## Summary

UOW-1264 extends `PlayerKnownListPopulationPlanService` so each candidate plan can now carry the descriptor-only player packet side-effect attachment plan created from its two-way operation plan.

This is still not live Java known-list parity. The service does not send packets, mutate live world known-lists, wire `GameServerConnection`, execute scheduler callbacks, or move players. It only makes the non-live population composition preserve controller packet-intent metadata end to end.

## Java Source Findings

- Java `KnownList.update()` runs cleanup/visibility updates before region candidate discovery.
- Java `KnownList.updateVisibility` triggers `see` when cached visibility changes to true.
- Java `KnownList.del` triggers `notSee` before `notKnow` when the cached known object was visible.
- Java `PlayerController.see(Player)` and `notSee(Player)` own the concrete packet side effects.
- Prior C# planners modeled those pieces separately; this unit carries their metadata through the population composition result.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`:

- `PlayerKnownListPopulationCandidateFact` now accepts optional owner-viewing-candidate and candidate-viewing-owner side-effect facts.
- `PlayerKnownListPopulationCandidatePlan` now carries `SideEffectAttachmentPlan`.
- `PlayerKnownListPopulationPlan` now exposes `AttachedControllerSideEffectDescriptors`.
- `PlayerKnownListPopulationPlanService` now composes each visibility/range operation plan through `PlayerKnownListOperationSideEffectAttachmentService`.
- `ExecutedControllerSideEffects` remains `false`; descriptors are metadata only.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPlanServiceTests" --nologo` passed 6 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 262 tests.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1264

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Known-List Population Composition | Partial | Unit Tested | Partial Parity | Population composition now carries operation side-effect attachment metadata per candidate. Does not execute Java synchronized update, live region storage, or controller callbacks. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `PlayerKnownListPopulationCandidatePlan.SideEffectAttachmentPlan` | Visibility Side-Effect Metadata | Partial | Unit Tested | Partial Parity | In-range visible candidates can carry directional `see` packet descriptors. Caller supplies side-effect facts; no live `owner.canSee` or controller dispatch occurs. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListPopulationCandidatePlan.SideEffectAttachmentPlan` | Removal Side-Effect Metadata | Partial | Unit Tested | Partial Parity | Out-of-range visible known candidates can carry directional `notSee` delete descriptors. Does not execute `notKnow`, target cleanup, or packet sends. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListPopulationPlanService.Plan` | Candidate Population Composition | Partial | Unit Tested | Partial Parity | Region candidates flow through range, two-way operation, optional membership metadata, and side-effect attachment metadata. Does not scan live `MapRegion` objects. |
| `com.aionemu.gameserver.controllers.PlayerController.see` / `notSee` | `PlayerKnownListOperationSideEffectAttachmentService` through population composition | Controller Packet Intent Metadata | Partial | Unit Tested | Partial Parity | Population results can now expose directional packet intents. `SmPlayerInfo` enemy/aggro remains partial; `SmPlayerStance` and `SmAbnormalEffect` remain missing; no live sends. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | population side-effect metadata plus known-list composition stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future bind-point fanout can inspect population-side controller packet descriptors. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_AttachesPlayerSideEffectDescriptorsToVisibleOperationPlansWithoutExecutingThem` | Unit / Composition | `KnownList.updateVisibility`; `PlayerController.see` | Population composition attaches directional see descriptors and supplied packet facts while executing no packets. | Source-derived ordering and direction. | No Java runtime comparison or live controller dispatch. |
| `Plan_AttachesSkippedNotSeeDescriptorForOutOfRangeUnspawnedViewer` | Unit / Composition | `KnownList.del`; `PlayerController.notSee` | Out-of-range visible known candidate carries skipped notSee metadata when the viewer is unspawned. | Source-derived branch. | Spawned state is supplied metadata, not live player state. |

## Remaining Risks

- Population side-effect integration is non-live and unwired.
- It depends on supplied candidate facts and supplied directional side-effect facts.
- It does not execute live `KnownList`, `PlayerController`, packet sends, `notKnow`, target cleanup, or Java exception handling.
- `SmPlayerInfo` enemy/aggro behavior remains partial.
- `SmPlayerStance` and `SmAbnormalEffect` packet classes remain missing.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and serialization behavior remain unverified for live known-list callback dispatch.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 population-side attachment integration plus 2 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 live world known-list callback path, 1 `SmPlayerInfo` enemy/aggro packet gap, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Start a focused `SmPlayerInfo(enemy)` serializer parity audit and test slice, or add a non-live fanout trace bridge that can consume population candidate side-effect descriptors without sending packets. Prefer the packet audit before any live player-see dispatch work.

## Update After UOW-1265

The focused `SmPlayerInfo(enemy)` slice is now present for the Java creature-type byte. Population-side descriptors can eventually target a packet constructor that accepts the enemy flag, but population composition still does not instantiate or send live packets and still lacks viewer-sensitive race projection.

## Update After UOW-1266

`SmPlayerInfo` now also accepts supplied viewer-context metadata for Java's enemy/opposite-race projection and neutral-to-all-player override. Population-side descriptors still do not hydrate those metadata inputs, so a future descriptor-to-packet bridge must provide active viewer race, computed enemy state, and neutral-state facts before live sends.

## Update After UOW-1267

`SmPlayerStance` packet serialization is now available for stance descriptors carried by population-side attachment metadata. Population composition remains disabled from live world lifecycle and socket dispatch, and `SmAbnormalEffect` remains the next missing packet serializer in the player-see sequence.

## Update After UOW-1268

`SmAbnormalEffect` packet serialization is now partially available for supplied abnormal-effect facts. Population composition still cannot hydrate live effect-controller data or construct/send packets, so the next safe bridge is descriptor-to-packet metadata without live socket dispatch.
