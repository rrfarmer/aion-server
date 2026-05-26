# Phase 6 Bind-Point Teleport Known-List Operation Packet Construction

Date: May 26, 2026
Unit of Work: UOW-1270
Scope: Apply non-live player side-effect packet construction across directional known-list operation attachments.
Source of truth: Java project.

## Summary

UOW-1270 adds `PlayerKnownListOperationSideEffectPacketConstructionService`, a metadata-only bridge that consumes `PlayerKnownListOperationSideEffectAttachmentPlan` and applies per-player side-effect packet construction to each attached directional `see` / `notSee` step.

This remains non-live. The bridge requires supplied subject player facts, active motions, optional viewer context, ride speed/stat facts, and abnormal-effect facts. It never sends packets, mutates known lists, reads live world state, or calls `GameServerConnection`.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Operation attachment packet-construction bridge | `KnownList.updateVisibility`, `KnownList.del`, `PlayerController.see/notSee` | new service/tests/docs | Service / Test | Limited | Medium | Main implementation touches shared known-list bridge/test docs. |
| B | Ride `SM_EMOTION` stat/speed audit | `PlayerController.sendPlayerInfoPackets`, `SM_EMOTION` | read-only | Java Analysis | Yes | Medium | Independent sidecar audit; no file edits. |
| C | Effect-controller hydration audit | `EffectController`, `Effect` | docs/read-only | Java Analysis | Yes | Medium | Needed before live abnormal effects. |
| D | Population fanout trace bridge | `KnownList.update`, population plans | new service/tests | No with A | Medium | Likely consumes the same operation-side construction outputs. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Audit Java/C# ride `SM_EMOTION` constructor stat/speed inputs | Java/C# Analysis | read-only | all writes | none | ride input map, tests, risks |
| Orchestrator | Implement operation packet-construction bridge and docs | Service/Test/Docs | new service/test/docs and bridge facts | live dispatch/world mutation | none | code, tests, parity docs |

The explorer completed read-only analysis and was closed.

## Java Source Findings

- Java `KnownList.updateVisibility` and `KnownList.del` produce directional `PlayerController.see` / `notSee` callbacks after known-list membership operations.
- Java add ordering preserves candidate-side callbacks before owner-side callbacks when both sides see each other.
- Java remove ordering preserves owner-side notSee before candidate-side notSee when both sides previously knew/saw each other.
- Java `sendPlayerInfoPackets` ride branch calls `new SM_EMOTION(player, EmotionType.RIDE, 0, player.ride.getNpcId())`.
- Java `SM_EMOTION` ride serialization writes sender id, type `15`, state, live movement speed, ride NPC id, and trailing `63/63/64` floats.
- Java ride constructor derives live movement speed and attack-speed stats; attack-speed fields are not serialized for `RIDE`, but movement speed is.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListOperationSideEffectPacketConstructionService.cs`:

- `PlayerKnownListOperationSideEffectPacketConstructionFacts`
- `PlayerKnownListOperationSideEffectPacketConstructionRequest`
- `PlayerKnownListOperationSideEffectPacketConstructionPlan`
- per-direction construction result records and statuses.

The service:

- preserves attached operation-step order;
- applies `PlayerKnownListPlayerSideEffectPacketConstructionService` to each attached side-effect plan;
- blocks missing subject facts per direction;
- propagates partial per-player construction results;
- stays non-live and non-sending.

Also extended `PlayerKnownListPlayerSideEffectPacketConstructionRequest` and operation facts with supplied ride movement/stat facts:

- `RideMovementSpeed`
- `RideBaseAttackSpeed`
- `RideCurrentAttackSpeed`

The bridge now passes those facts into ride `SmEmotion` construction instead of always using C# defaults.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListOperationSideEffectPacketConstructionServiceTests|PlayerKnownListPlayerSideEffectPacketConstructionServiceTests|PlayerKnownListOperationSideEffectAttachmentServiceTests" --nologo` passed 14 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 279 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1270

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `Aion.GameServer.Services.PlayerKnownListOperationSideEffectPacketConstructionService` | Operation Packet Construction Bridge | Partial | Unit Tested | Partial Parity | C# applies packet construction metadata per attached directional see step and preserves operation-step order. It does not execute live known-list mutation, visibility checks, controller callbacks, or sends. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListOperationSideEffectPacketConstructionService` | Operation Packet Construction Bridge | Partial | Unit Tested | Partial Parity | C# applies packet construction metadata for directional notSee/delete steps. It does not execute Java `notKnow`, target cleanup, or socket sends. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | operation bridge plus player packet construction bridge | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | Operation-level bridge can construct directional packet metadata from supplied facts. Live aggro/viewer facts, active motions, ride stats, stance state, and effects remain supplied inputs. |
| `com.aionemu.gameserver.controllers.PlayerController.see` abnormal-effect tail | operation bridge plus abnormal-effect packet construction | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | Partial packet construction result is propagated when abnormal-effect facts are missing. Live `EffectController` hydration remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` via construction bridge | Packet / Serialization Dependency | Partial | Unit Tested | Partial Parity | Bridge now accepts supplied ride movement speed and passes it to `SmEmotion`; focused test asserts ride payload speed/trailing floats. Java live stat hydration and runtime comparison remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | operation attachment packet construction metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future fanout can now produce packet construction metadata for operation attachments. Live scheduled callbacks, sockets, movement, cooldown, world mutation, and Java runtime comparison remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_AddPlanBuildsDirectionalPacketConstructionPlansInOperationOrder` | Unit / Bridge | `KnownList.updateVisibility`; `PlayerController.sendPlayerInfoPackets` | Candidate-see-owner then owner-see-candidate construction plans preserve Java operation order and packet types. | Source-derived ordering and packet type assertions. | Supplied facts only; no live world/controller dispatch. |
| `Construct_MissingSubjectFactsBlocksOnlyThatDirectionalAttachment` | Unit / Bridge Guard | C# supplied-fact boundary | Missing subject facts block only the affected directional attachment. | C# guard for source-fact consistency. | Java has live object references instead of supplied facts. |
| `Construct_PropagatesPartialPacketConstructionForMissingAbnormalFacts` | Unit / Bridge | `PlayerController.see` abnormal-effect tail | Missing effect facts propagate a partial packet-construction result. | Source-derived required input. | No live effect-controller hydration. |
| `Construct_RemovePlanBuildsDeletePacketsForBothDirections` | Unit / Bridge | `KnownList.del`; `PlayerController.notSee` | Owner/candidate notSee steps construct delete packet metadata in Java remove order. | Source-derived ordering and packet type assertions. | No live `super.notSee`, target cleanup, or sends. |

Tests updated:

| Test Name | Type | What Changed | Gaps |
|---|---|---|---|
| `Construct_SeeWithAllSuppliedFactsCreatesPacketsInJavaOrderWithoutSending` | Unit / Bridge | Supplies ride movement speed and asserts ride `SmEmotion` payload speed, NPC id, and trailing `63/63/64` floats. | Still no Java runtime golden packet; live stat hydration remains external. |

## Remaining Risks

- Operation bridge is non-live and does not execute Java `KnownList`, `PlayerController`, `PacketSendUtility`, or socket sends.
- Subject player facts, active motions, viewer context, ride stats, stance state, abnormal masks/effects, and timers remain supplied metadata.
- Ride movement speed can now be supplied, but no live Java-equivalent stat resolver hydrates it for known-list fanout.
- Attack-speed facts can be supplied for constructor parity but are not serialized for `RIDE`; `CHANGE_SPEED` remains separate behavior.
- Population-level candidate plans do not yet carry packet construction metadata end to end.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, serialization edge cases, and live socket ordering remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 operation packet-construction bridge, ride stat plumbing, 4 focused bridge tests, and 1 updated ride payload assertion
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 active-player context computation path, 1 live motion/stat hydration path, 1 live effect-controller hydration path, 1 live known-list fanout executor, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a population-side packet-construction metadata composition layer that carries operation-level packet construction results into `PlayerKnownListPopulationPlanService` candidate plans. Keep it disabled/non-live and continue requiring supplied per-player facts.
