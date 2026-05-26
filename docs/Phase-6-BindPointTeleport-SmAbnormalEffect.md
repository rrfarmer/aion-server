# Phase 6 Bind-Point Teleport SM_ABNORMAL_EFFECT Packet

Date: May 26, 2026
Unit of Work: UOW-1268
Scope: Add focused C# packet support for Java `SM_ABNORMAL_EFFECT` from supplied effect facts.
Source of truth: Java project.

## Summary

UOW-1268 adds a scalar `SmAbnormalEffect` packet serializer for Java's `SM_ABNORMAL_EFFECT` payload and updates player known-list side-effect descriptors to reference the packet as partial C# support.

This is still not live effect parity. C# does not hydrate effects from a live `EffectController`, does not manage abnormal-state masks, does not apply/removal effect runtime behavior, and does not send packets from live known-list callbacks.

## Java Source Findings

- Java opcode is `50`.
- Java `SM_ABNORMAL_EFFECT(Creature)` reads:
  - effected object id;
  - `effected.getEffectController().getAbnormals()`;
  - `effected.getEffectController().getAbnormalEffects()`;
  - `SkillTargetSlot.FULLSLOTS`.
- Java full constructor filters effects when `slots != SkillTargetSlot.FULLSLOTS` using `(slots & effect.getTargetSlot().getId()) != 0`.
- Java `effectType` is `2` for `Player`, otherwise `1`.
- Payload prefix:
  - `D` effected object id;
  - `C` effect type;
  - `D` zero time placeholder;
  - `D` abnormal mask;
  - `D` zero placeholder;
  - `C` slots;
  - `H` filtered effect count.
- Player effects include `D effectorId` before each effect body.
- Player and creature effect bodies write `H skillId`, `C skillLevel`, `C targetSlot.ordinal()`, `D remainingTimeToDisplay`.
- Java `PlayerController.see` sends `SM_ABNORMAL_EFFECT` after player-specific see packets when a seen creature's effect controller is not empty.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAbnormalEffect.cs`:

- `PacketOpCode = 50`.
- `SmAbnormalEffectEntry` captures explicit effect facts:
  - effector object id;
  - skill id;
  - skill level;
  - target slot id;
  - target slot ordinal;
  - remaining display time.
- `SmAbnormalEffect` supports player and non-player effect types.
- Filters supplied effects by Java slot-bit semantics when `slots != FullSkillTargetSlots`.
- Writes Java-shaped payload fields from supplied facts.

Updated `PlayerKnownListPlayerSideEffectPlanService`:

- abnormal-effect descriptors now point to `SmAbnormalEffect`;
- support status changed from `Missing` to `Partial`;
- descriptor remains non-live and does not hydrate effects or send packets.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "SmAbnormalEffect|PlayerKnownListPlayerSideEffectPlanServiceTests" --nologo` passed 8 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 270 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1268

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbnormalEffect` | Packet / Serialization | Partial | Unit Tested | Partial Parity | C# writes Java-shaped player and non-player payloads from supplied facts, including slot filtering. No live `EffectController` hydration or Java runtime packet capture exists. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbnormalEffectEntry` | Packet DTO / Effect Snapshot | Partial | Unit Tested | Needs Verification | C# entry captures packet-facing fields only. It does not port effect lifecycle, stack handling, target-slot enum semantics beyond supplied id/ordinal, remaining-time calculation, or skill template behavior. |
| `com.aionemu.gameserver.controllers.effect.EffectController.getAbnormalEffects` / `getAbnormals` | supplied `SmAbnormalEffect` inputs | Effect Controller Dependency | Not Started | No Tests | Needs Verification | Live abnormal mask/effect collection hydration is not ported in this unit. The caller must supply already-filterable effect facts. |
| `com.aionemu.gameserver.controllers.PlayerController.see` abnormal-effect branch | `PlayerKnownListPlayerSideEffectPlanService` abnormal-effect descriptor | Controller Packet Intent / Packet Dependency | Partial | Unit Tested | Partial Parity | Descriptor now references concrete packet support as partial. It still does not instantiate/send packets, evaluate live `EffectController.isEmpty`, or hydrate effect facts. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | player known-list descriptor stack with partial `SmAbnormalEffect` packet support | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout has a packet serializer prerequisite, but live scheduled callbacks, sockets, movement, cooldown, effect hydration, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmAbnormalEffect_PlayerWritesEffectorAndFiltersBySlotLikeJava` | Unit / Packet | `SM_ABNORMAL_EFFECT.writeImpl`; constructor slot filter | Player payload includes effector id and filters by target-slot bit id. | Source-derived byte assertion. | Effect facts and remaining time are supplied, not live-computed. |
| `SmAbnormalEffect_CreatureOmitsEffectorLikeJava` | Unit / Packet | `SM_ABNORMAL_EFFECT.writeImpl` | Non-player payload omits effector id and writes skill/level/slot/time. | Source-derived byte assertion. | No live creature/effect model or Java runtime capture. |

Tests updated:

| Test Name | Type | What Changed | Gaps |
|---|---|---|---|
| `PlanSee_AbnormalEffectsAppendAfterPlayerSpecificPackets` | Unit / Planner | Abnormal-effect descriptor now expects `SmAbnormalEffect` and `Partial` C# support. | Descriptor is still non-live and does not hydrate effects or send packets. |

## Remaining Risks

- No live `EffectController` hydration exists for abnormal mask/effect entries.
- Remaining-time calculation is supplied, not computed from Java-equivalent effect timers.
- Target-slot id and ordinal are supplied separately; enum parity is not yet enforced by a shared C# skill target slot model.
- Effect lifecycle, stacking, `NOSHOW` toggle filtering, passive effect maps, cooldown conflicts, and broadcast ordering are not ported.
- Known-list `sendPlayerInfoPackets` remains descriptor-only.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and broader live packet ordering remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 focused packet serializer plus 1 packet DTO, 2 focused packet tests, and 1 descriptor support update
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live effect-controller hydration path, 1 live controller side-effect dispatcher, 1 active-player context computation path, 1 Java runtime capture path, and 1 live scheduled callback path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live player `see` descriptor-to-packet input bridge that can convert supplied `SM_PLAYER_INFO`, `SM_MOTION`, optional ride, stance, and abnormal-effect descriptor facts into concrete packet construction metadata without sending packets. Keep live `GameServerConnection` dispatch disabled.

## Update After UOW-1269

`PlayerKnownListPlayerSideEffectPacketConstructionService` now performs that non-live conversion for individual player side-effect plans and blocks abnormal-effect construction when effect facts are missing. It does not hydrate live `EffectController` data or send packets.

## Update After UOW-1270

Operation-level packet construction propagates partial abnormal-effect construction results when effect facts are missing. Live `EffectController` hydration remains the blocker before any real abnormal-effect fanout.
