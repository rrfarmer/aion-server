# Phase 6 Bind-Point Teleport Known-List Operation Side-Effect Attachment

Date: May 26, 2026
Unit of Work: UOW-1263
Scope: Attach descriptor-only player packet side-effect plans to two-way known-list operation steps.
Source of truth: Java project.

## Summary

UOW-1263 adds `PlayerKnownListOperationSideEffectAttachmentService`, a non-live composition layer that consumes `PlayerKnownListTwoWayOperationPlan` values and attaches player `see` / `notSee` packet side-effect descriptors to the operation steps that would trigger Java controller callbacks.

This service does not send packets, mutate membership, wire `GameServerConnection`, or execute live world lifecycle callbacks. It only joins existing operation-step metadata with UOW-1262 packet-intent metadata.

## Java Source Findings

- Java `KnownList.updateVisibility` calls controller `see` when an object becomes visible.
- Java `KnownList.del` calls controller `notSee` before `notKnow` when the cached `KnownObject` is visible.
- The UOW-1258 two-way planner already records owner/candidate `see` and `notSee` step directions.
- The UOW-1262 player side-effect planner records Java `PlayerController.see(Player)` and `notSee(Player)` packet intent for a single viewer/subject direction.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListOperationSideEffectAttachmentService.cs`:

- `Attach` skips rejected operation plans.
- `OwnerSeesCandidate` maps to owner-viewing-candidate `PlanSee`.
- `CandidateSeesOwner` maps to candidate-viewing-owner `PlanSee`.
- `OwnerNotSeesCandidate` maps to owner-viewing-candidate `PlanNotSee`.
- `CandidateNotSeesOwner` maps to candidate-viewing-owner `PlanNotSee`.
- Direction facts carry aggro, ride, stance, abnormal effects, viewer spawned state, and not-see animation metadata.
- Viewer and subject object ids are derived from the operation plan to avoid caller cross-wiring.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListOperationSideEffectAttachmentServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 260 tests.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1263

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `Aion.GameServer.Services.PlayerKnownListOperationSideEffectAttachmentService.Attach` | Known-List Side-Effect Composition | Partial | Unit Tested | Partial Parity | Attaches player `see` packet descriptors to operation-plan visibility steps by direction. Does not execute live visibility recomputation, controller dispatch, or exception handling. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListOperationSideEffectAttachmentService.Attach` | Known-List Removal Side-Effect Composition | Partial | Unit Tested | Partial Parity | Attaches player `notSee` delete descriptors before existing `notKnow` metadata steps. Does not execute target cleanup, packet send, `notKnow`, or live map mutation. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | clear operation plans consumed by side-effect attachment service | Known-List Clear Side-Effect Composition | Partial | Unit Tested | Needs Verification | Clear plans with visible not-see steps can receive descriptors, but supplied animation semantics are caller metadata and no live clear iteration occurs. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPlayerSideEffectPlanService` through attachment service | Controller Packet Intent | Partial | Unit Tested | Partial Parity | Directional see descriptors attach to candidate/owner see steps. Java `super.see`, abnormal-effect packet serialization, and live sends remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee` | `PlayerKnownListPlayerSideEffectPlanService` through attachment service | Controller Packet Intent | Partial | Unit Tested | Partial Parity | Directional notSee descriptors attach to visible removal steps and preserve viewer-unspawned skip metadata. Java `super.notSee`, target cleanup, and live sends remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` / `SM_MOTION` / `SM_EMOTION` / `SM_PLAYER_STANCE` / `SM_ABNORMAL_EFFECT` / `SM_DELETE` | side-effect descriptors attached to two-way operation steps | Packet / Serialization Dependencies | Partial | Unit Tested | Needs Verification | Descriptor composition references existing packet support statuses. `SmPlayerInfo` enemy/aggro behavior remains partial; `SmPlayerStance` and `SmAbnormalEffect` remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | non-live operation side-effect attachment plus known-list population stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout can now carry operation-step packet descriptors. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Attach_AddPlanMapsCandidateAndOwnerSeeStepsToCorrectViewerDirections` | Unit / Composition | `KnownList.updateVisibility`; `PlayerController.see` | Candidate and owner see steps attach directional see packet descriptors with correct viewer/subject ids and facts. | Source-derived ordering/direction. | No live controller dispatch or Java runtime comparison. |
| `Attach_RemovePlanMapsNotSeeStepsToDeleteDescriptorsWithDirectionAnimations` | Unit / Composition | `KnownList.del`; `PlayerController.notSee` | Owner and candidate notSee steps attach directional delete descriptors and supplied animations. | Source-derived direction/order. | Animation values are supplied metadata; no live clear/remove execution. |
| `Attach_RemovePlanKeepsUnspawnedViewerNotSeeAsSkippedSideEffect` | Unit / Composition | `PlayerController.notSee` unspawned guard | Unspawned viewer notSee step remains attached but has skipped packet status and no descriptors. | Source-derived branch. | Does not execute teleport/spawned state from live player. |
| `Attach_RejectedPlanDoesNotAttachDescriptors` | Unit / Guard | `KnownList.add` rejected paths | Rejected two-way plans attach no descriptors. | Source-derived stop condition. | No Java runtime comparison. |
| `Attach_PlanWithoutSeeOrNotSeeStepsReportsNoSideEffectSteps` | Unit / Guard | C# descriptor boundary | Membership-only operation plans report no side-effect steps. | C# staging guard. | Java live callbacks depend on visibility state, supplied earlier by planners. |

## Remaining Risks

- Attachment service is non-live and unwired.
- It depends on operation-plan steps and caller-supplied directional facts.
- It does not execute live `KnownList`, `PlayerController`, packet sends, `notKnow`, or target cleanup.
- `SmPlayerInfo` enemy/aggro behavior remains partial.
- `SmPlayerStance` and `SmAbnormalEffect` packet classes are still missing.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and serialization behavior remain unverified for live known-list callback dispatch.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 operation side-effect attachment service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 live world known-list callback path, 1 `SmPlayerInfo` enemy/aggro packet gap, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled integration composition that carries operation side-effect attachments through `PlayerKnownListPopulationPlanService` results, or start a focused `SmPlayerInfo(enemy)` serializer parity audit before any live player-see dispatch work.

## Update After UOW-1264

`PlayerKnownListPopulationPlanService` now carries operation side-effect attachment plans per candidate. The next safe blocker is packet serializer readiness, especially `SmPlayerInfo(enemy)`, `SmPlayerStance`, and `SmAbnormalEffect`, before any live known-list controller dispatcher can be considered.

## Update After UOW-1267

`SmPlayerStance` packet serialization is now available and player side-effect descriptors point to the concrete C# packet type. Operation side-effect attachment remains non-live and still cannot instantiate/send packets or cover `SmAbnormalEffect`.

## Update After UOW-1268

`SmAbnormalEffect` packet serialization is now partially available from supplied effect facts. Operation side-effect attachment still carries descriptors only and cannot hydrate effect-controller facts or instantiate/send concrete packets.
