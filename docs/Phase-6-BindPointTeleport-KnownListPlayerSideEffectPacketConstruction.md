# Phase 6 Bind-Point Teleport Player Side-Effect Packet Construction

Date: May 26, 2026
Unit of Work: UOW-1269
Scope: Add a non-live bridge from player known-list side-effect descriptors to concrete packet construction metadata.
Source of truth: Java project.

## Summary

UOW-1269 adds `PlayerKnownListPlayerSideEffectPacketConstructionService`, a disabled/non-sending bridge that converts supplied player `see` / `notSee` side-effect descriptor plans into concrete packet objects or blocked metadata.

This is not live known-list dispatch parity. The service requires supplied `Player`, motion, viewer-context, ride, stance, and abnormal-effect facts. It never calls `GameServerConnection`, `PacketSendUtility`, sockets, world known-list mutation, or live effect-controller hydration.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Player see descriptor-to-packet bridge | `PlayerController.sendPlayerInfoPackets`, player-see packet classes | new service/test/docs | Service / Test | Limited | Medium | Main implementation touches shared descriptor/test docs, but read-only constructor mapping can run separately. |
| B | Effect-controller hydration audit | `EffectController`, `Effect`, `SkillTargetSlot` | docs only | Java Analysis | Yes | Medium | Independent but not required before a supplied-facts bridge. |
| C | Java packet-observer design | controller send order / packet send utility | docs only | Java Analysis | Yes | Low/Medium | Useful before runtime parity claims. |
| D | Population fanout trace bridge | `KnownList.update`, `PlayerController.see` | new service/tests | Service | No with A | Medium | Consumes same side-effect descriptors and would collide conceptually. |
| E | Live readiness checklist refresh | known-list dispatch readiness docs | docs only | Documentation | Yes | Low/Medium | Safe but lower value than bridge. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Map packet constructor inputs and blockers for the bridge | Java/C# Analysis | read-only | all writes | none | packet order, required inputs, blocked metadata cases |
| Orchestrator | Implement bridge, tests, docs, commit | Service/Test/Docs | new service/test/docs | live `GameServerConnection`, world mutation | explorer output can refine local work | code, tests, parity docs |

The explorer completed read-only analysis and was closed. No sub-agent wrote files.

## Java Source Findings

- Java `PlayerController.see` calls `sendPlayerInfoPackets(player)` for seen players.
- Java player-see packet order is:
  1. `SM_PLAYER_INFO`
  2. `SM_MOTION`
  3. optional ride `SM_EMOTION`
  4. optional `SM_PLAYER_STANCE`
  5. optional `SM_ABNORMAL_EFFECT` from the outer creature branch after player-specific packets.
- Java `SM_PLAYER_INFO` depends on the visible player, aggro/enemy flag, and active viewer context.
- Java `SM_MOTION` depends on `player.getMotions().getActiveMotions()`.
- Java ride `SM_EMOTION` depends on `player.ride.getNpcId()` plus player stat/movement inputs in the packet constructor.
- Java `SM_PLAYER_STANCE` uses the visible player object id and state `1`.
- Java `SM_ABNORMAL_EFFECT` depends on `EffectController.getAbnormals()` and `getAbnormalEffects()`.
- Java `PlayerController.notSee` skips deletion packets when the viewer is unspawned; otherwise the player fallback sends `SM_DELETE(object, animation)`.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPlayerSideEffectPacketConstructionService.cs`:

- `PlayerKnownListPlayerSideEffectPacketConstructionRequest`
- `PlayerKnownListPlayerSideEffectPacketConstructionPlan`
- `PlayerKnownListPlayerSideEffectPacketConstructionResult`
- construction statuses for all-constructed, partial, and no-descriptor outcomes;
- per-result blocked statuses for subject mismatch, missing ride NPC id, missing abnormal-effect facts, and unsupported descriptor kinds.

The service constructs packet objects for:

- `SmPlayerInfo`
- `SmMotion`
- ride `SmEmotion`
- `SmPlayerStance`
- `SmAbnormalEffect`
- `SmDelete`

The service explicitly does not send packets and marks `ExecutesLivePackets`, `IsLive`, and `IsJavaControllerParity` false.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPlayerSideEffectPacketConstructionServiceTests|PlayerKnownListPlayerSideEffectPlanServiceTests" --nologo` passed 11 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 275 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1269

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPlayerSideEffectPacketConstructionService` | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | C# preserves descriptor order and constructs packet objects from supplied facts. It does not execute Java controller callbacks, compute live aggro/viewer facts, hydrate live motion/effect state, or send packets. |
| `com.aionemu.gameserver.controllers.PlayerController.see` abnormal-effect tail | `PlayerKnownListPlayerSideEffectPacketConstructionService` abnormal-effect result | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | Abnormal-effect packets can be constructed only when supplied effect facts are present. Live `EffectController.isEmpty`, `getAbnormals`, and effect collection hydration remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee` player fallback | `PlayerKnownListPlayerSideEffectPacketConstructionService` delete result | Controller Packet Construction Bridge | Partial | Unit Tested | Partial Parity | C# constructs `SmDelete` for planned notSee descriptors and preserves unspawned-viewer skip from the descriptor planner. No live `super.notSee`, target cleanup, or socket send occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerInfo` via construction bridge | Packet / Serialization Dependency | Partial | Unit Tested | Partial Parity | Bridge passes supplied player, aggro flag, and optional viewer context. Java active-connection viewer lookup and full `isAggroIconTo` computation remain external inputs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmMotion` via construction bridge | Packet / Serialization Dependency | Partial | Unit Tested | Partial Parity | Bridge passes supplied active motion list. Java `player.getMotions().getActiveMotions()` hydration remains external input. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` via construction bridge | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Bridge constructs ride packet when ride NPC id is supplied and blocks when missing. Java stat/speed constructor inputs remain approximated by current C# packet defaults. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerStance` via construction bridge | Packet / Serialization Dependency | Complete | Unit Tested | Partial Parity | Bridge uses supplied stance state, normally Java state `1`. No live `isUnderStance` computation or broadcast occurs. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbnormalEffect` via construction bridge | Packet / Serialization Dependency | Partial | Unit Tested | Partial Parity | Bridge constructs only with supplied abnormal mask/effect entries/slots and blocks when facts are missing. Effect lifecycle and timer hydration remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | known-list descriptor stack plus packet construction bridge | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future fanout can now produce non-live packet construction metadata. Live scheduled callbacks, sockets, movement, cooldown, world mutation, and Java runtime comparison remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_SeeWithAllSuppliedFactsCreatesPacketsInJavaOrderWithoutSending` | Unit / Bridge | `PlayerController.sendPlayerInfoPackets`; `PlayerController.see` abnormal tail | Full supplied player-see descriptor sequence constructs `SmPlayerInfo`, `SmMotion`, ride `SmEmotion`, `SmPlayerStance`, and `SmAbnormalEffect` in Java order without live sends. | Source-derived ordering and C# packet type assertions. | No Java runtime packet capture; supplied facts only. |
| `Construct_SeeRideDescriptorWithoutNpcIdBlocksOnlyRidePacket` | Unit / Bridge | Java ride branch reads `player.ride.getNpcId()` | Missing ride NPC id blocks ride packet metadata while preserving other packet results. | Source-derived required input. | Does not model Java null exception path; intentionally records blocked metadata. |
| `Construct_SeeAbnormalDescriptorWithoutEffectFactsBlocksAbnormalPacket` | Unit / Bridge | Java `SM_ABNORMAL_EFFECT` reads `EffectController` data | Missing abnormal-effect facts block abnormal packet metadata. | Source-derived required input. | No live effect-controller hydration. |
| `Construct_NotSeeCreatesDeletePacketWithoutSending` | Unit / Bridge | `PlayerController.notSee` fallback `SM_DELETE` | Planned notSee descriptor constructs `SmDelete` without live send. | Source-derived packet dependency. | No live target cleanup or `super.notSee`. |
| `Construct_SubjectMismatchBlocksDescriptorPacketConstruction` | Unit / Bridge Guard | C# safety boundary around supplied subject facts | Mismatched supplied subject player blocks construction. | C# guard for source-fact consistency. | Java has live object identity instead of supplied-fact guard. |

## Remaining Risks

- Bridge is non-live and does not execute Java `PlayerController`, `KnownList`, or `PacketSendUtility`.
- Active viewer context, aggro/enemy calculation, active motion list, ride NPC id, abnormal mask/effects, and abnormal timers are all supplied facts.
- `SmEmotion` ride speed and attack-speed parity remain dependent on current packet defaults and need separate verification.
- Effect lifecycle, target-slot enum parity, `NOSHOW` filtering, stacking, and timer calculation are not hydrated.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, serialization edge cases, and live socket ordering remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live packet construction bridge plus 5 focused bridge tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 active-player context computation path, 1 live motion hydration path, 1 live effect-controller hydration path, 1 live known-list fanout executor, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live population-side packet-construction attachment bridge that consumes `PlayerKnownListOperationSideEffectAttachmentPlan` / `PlayerKnownListPopulationPlanService` side-effect plans and applies the packet construction bridge per attached direction. Keep it metadata-only and do not send packets.

## Update After UOW-1270

`PlayerKnownListOperationSideEffectPacketConstructionService` now applies player side-effect packet construction to operation attachment plans per direction. The bridge also accepts supplied ride movement/stat facts and passes ride movement speed into `SmEmotion`; live stat hydration and population-level composition remain pending.

## Update After UOW-1271

`PlayerKnownListPopulationPlanService` now composes operation-level packet construction metadata per population candidate when supplied subject facts are available. The individual player packet construction bridge remains non-live and still depends on supplied active motions, viewer context, ride facts, and abnormal-effect facts.
