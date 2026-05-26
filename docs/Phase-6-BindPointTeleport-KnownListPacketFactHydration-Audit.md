# Phase 6 Bind-Point Teleport Known-List Packet Fact Hydration Audit

Date: May 26, 2026
Unit of Work: UOW-1272
Scope: Read-only audit of Java runtime fact sources and current C# readiness for hydrating known-list player packet construction facts.
Source of truth: Java project.

## Summary

UOW-1272 audits the runtime facts needed before `PlayerKnownListPopulationPlanService` can safely hydrate `PlayerKnownListOperationSideEffectPacketConstructionFacts` from live C# state.

Result: do not hydrate or send live known-list player packets yet. C# has useful scalar surfaces for some inputs, but Java derives the full packet sequence from live connection, player, controller, stat, motion, ride, and effect-controller state. Several of those sources are incomplete, supplied-only, or not attached to a Java-equivalent world/known-list lifecycle in C#.

## Java Runtime Fact Sources

| Packet Fact | Java Source | Java Notes | Current C# Surface | Readiness |
|---|---|---|---|---|
| Active viewer context for `SM_PLAYER_INFO` race/enemy projection | `SM_PLAYER_INFO.writeImpl(AionConnection con)` reads `con.getActivePlayer()`, `activePlayer.isEnemy(player)`, `activePlayer.getOppositeRace()`, and both players' `CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS`. | Packet serialization is connection/viewer-sensitive and returns early when the active player is missing. | `SmPlayerInfoViewerContext`; `Player.Race`; no complete live active-player context assembler for known-list sends. | Partial; needs a connection/viewer fact adapter and neutral/enemy-state source. |
| `SM_PLAYER_INFO` movement speed and attack speed | `player.getGameStats().getMovementSpeedFloat()` and `player.getGameStats().getAttackSpeed().getBase()/getCurrent()`. | Movement and attack speed are live stat-container values, not constants. | `PlayerMovementSpeedResolver.ResolveKnownMovementSpeed(player)` for movement approximation; no general `PlayerGameStats` live stat container for known-list packets. | Partial for movement; attack speed hydration remains missing. |
| Active motions for `SM_MOTION` player-see action `7` | `player.getMotions().getActiveMotions()` supplies a map by motion type. | Java writes five motion-type slots in order, zero-filling missing active motions. | `Player.Motions`; `PlayerMotion`; `SmMotion(int playerObjectId, IReadOnlyList<PlayerMotion>)`; repository and enter-world load surfaces. | Partial; can use `Player.Motions.Where(IsActive)`, but live expiration/update semantics need verification. |
| Ride NPC id for ride `SM_EMOTION` | `player.isInPlayerMode(PlayerMode.RIDE)` and `player.ride.getNpcId()`. | Java assumes active ride info when ride mode is true. | `Player.IsInRideMode`; `Player.RideInfo`; `PlayerRideInfo.NpcId`; ride action state methods. | Partial; guard needed when mode and ride info diverge. |
| Ride movement speed and attack speed | `SM_EMOTION(Creature, ...)` reads `creature.getGameStats().getMovementSpeedFloat()` and attack-speed base/current. | Ride packet serializes speed in the common header and stores attack-speed facts even though `RIDE` payload does not write the attack-speed fields. | Packet construction facts include supplied ride movement/base/current attack speed; `SmEmotion` accepts them. | Needs Verification; live stat source is missing for known-list construction. |
| Stance state for `SM_PLAYER_STANCE` | `PlayerController.isUnderStance()` checks `stanceObserver != null`; player-see sends `new SM_PLAYER_STANCE(player, 1)`. | Stance start/stop also broadcasts state `1`/`0` live. | `Player.StanceSkillId`; `Player.IsUnderStance()`; `SmPlayerStance`. | Partial; scalar state exists, but controller observer lifecycle is not live-equivalent. |
| Abnormal effect mask | `SM_ABNORMAL_EFFECT(Creature)` reads `effected.getEffectController().getAbnormals()`. | Mask comes from `EffectController.abnormals`. | `Player.AbnormalState`; `PlayerAbnormalState`; `SmAbnormalEffect` accepts supplied mask. | Partial for scalar mask; not a complete effect-controller source. |
| Abnormal effect entries | `EffectController.getAbnormalEffects()` filters `abnormalEffectMap` and excludes no-show toggles; packet filters by `SkillTargetSlot` when requested. | Entries include effector id, skill id, skill level, target slot id/ordinal, and remaining display time. | `SmAbnormalEffectEntry` DTO and packet serializer; no live `Effect` collection on `Player`. | Blocked; requires effect-controller/effect lifecycle hydration. |
| Remaining abnormal display time | `Effect.getRemainingTimeToDisplay()`. | Time is runtime/lifecycle-derived and changes continuously. | `SmAbnormalEffectEntry.RemainingTimeToDisplayMillis` supplied value only. | Blocked; needs live effect timer model and date/time semantics. |

## Current C# Hydration Candidate Map

| Needed Fact | Candidate C# Artifact | Status | Gap |
|---|---|---|---|
| Subject `Player` object | `Aion.GameServer.Model.GameObjects.Player` | Partial | Player model has many packet-facing fields but is not guaranteed to be Java live world object state inside known-list population. |
| Active viewer/player context | `GameServerConnection`; `SmPlayerInfoViewerContext` | Partial | No isolated known-list packet fact adapter reads active-player race/enemy/neutral facts for a viewer-subject pair. |
| Active motions | `Player.Motions`; `PlayerMotion`; `PlayerEnterWorldService`; `MotionLearnService`; `ExpirableTaskService` | Partial | Enter-world and mutation surfaces exist, but active motion expiration and live known-list packet timing need verification. |
| Ride metadata | `Player.IsInRideMode`; `Player.RideInfo`; `PlayerRideInfo` | Partial | NPC id exists, but movement/attack stat values still need a live stat resolver. |
| Movement speed | `PlayerMovementSpeedResolver` | Partial | Existing resolver is packet-oriented and approximate; Java stat modifiers need broader verification. |
| Attack speed | `SmStatsInfo` internal calculations; no shared known-list stat resolver | Blocked | No reusable `PlayerGameStats` equivalent source for base/current attack speed in known-list packet construction. |
| Stance | `Player.StanceSkillId`; `Player.IsUnderStance()` | Partial | No live `StanceObserver` lifecycle or Java controller source is wired to known-list construction. |
| Abnormal mask | `Player.AbnormalState` | Partial | Represents bit flags, but not full `EffectController.abnormals` lifecycle parity. |
| Abnormal effect entries/timers | `SmAbnormalEffectEntry` only | Blocked | No live collection of effect entries with effector id, skill level, slot id/ordinal, and remaining display time. |

## Readiness Decision

Do not add a live packet fact hydrator in this unit.

The next executable slice should be a disabled, supplied-player snapshot adapter only if it clearly labels missing facts and returns blocked metadata when live-derived values are unavailable. A full live hydrator must wait for:

- a viewer/subject active-player context adapter;
- a reusable movement/attack stat source;
- verified active motion expiration semantics;
- ride stat hydration;
- stance observer lifecycle parity;
- effect-controller entry and timer hydration;
- Java runtime packet-order comparison.

## Validation

- No production code changed in this unit.
- No tests were added or run for this documentation-only audit.
- Validation was a read-only source audit of Java and C# packet/model surfaces.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1272

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerInfo`; `SmPlayerInfoViewerContext` | Packet / Viewer-Sensitive Fact Source | Partial | Manual Only | Needs Verification | C# packet accepts supplied viewer context, but no live known-list fact adapter reads `AionConnection.activePlayer`, enemy state, neutral custom state, movement speed, or attack speed with Java parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION` | `Aion.GameServer.Network.Aion.ServerPackets.SmMotion`; `Player.Motions` | Packet / Motion Fact Source | Partial | Manual Only | Needs Verification | C# has active motion records and serializers, but Java's `getActiveMotions()` map, expiration timing, and live update ordering are not verified for known-list sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion`; `Player.RideInfo`; supplied ride stats | Packet / Ride Fact Source | Partial | Manual Only | Needs Verification | C# can store ride NPC id and supplied speed/stat facts, but no Java-equivalent live stat resolver hydrates movement/base/current attack speed for known-list construction. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerStance`; `Player.StanceSkillId` | Packet / Controller Fact Source | Partial | Manual Only | Needs Verification | Scalar stance state exists, but Java `StanceObserver` lifecycle and controller broadcast behavior are not live-equivalent in known-list construction. |
| `com.aionemu.gameserver.controllers.effect.EffectController` / `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `Player.AbnormalState`; `SmAbnormalEffectEntry`; `SmAbnormalEffect` | Effect Controller / Packet Fact Source | Partial | Manual Only | Needs Verification | C# can serialize supplied mask/effect entries, but has no live abnormal-effect map, no-show filtering source, target-slot model parity, or remaining-time computation. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` / `PlayerController.see` | future known-list packet fact hydrator | Controller Packet Hydration Boundary | Not Started | Manual Only | Needs Verification | No live hydrator exists. Population packet construction remains supplied-facts metadata and must not send packets. |

## Remaining Risks

- Live fact hydration is not implemented.
- The audit is manual evidence, not executable parity.
- Java connection-sensitive serialization cannot be inferred from subject `Player` alone.
- Motion expiration, ride stats, stance observer state, abnormal effect lifecycle, and remaining-time calculations remain unverified.
- Threading differs from Java controller/effect locks and known-list synchronization.
- Serialization is unchanged in this unit, and no Java runtime packet capture was performed.
- Date/time behavior matters for motion/effect remaining time and is not verified.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts; 1 fact hydration audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 active-viewer context adapter, 1 reusable stat resolver, 1 active motion live-timing verifier, 1 ride stat hydrator, 1 stance observer lifecycle bridge, 1 effect-controller entry/timer hydrator, 1 live known-list packet dispatcher, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled `PlayerKnownListPacketConstructionFactPlanService` that consumes supplied viewer/subject `Player` snapshots and returns either complete `PlayerKnownListOperationSideEffectPacketConstructionFacts` or explicit blocked metadata for missing viewer context, ride stats, attack speed, and abnormal-effect entries. Keep it non-live and do not send packets.

## Update After UOW-1273

`PlayerKnownListPacketConstructionFactPlanService` now exists as a disabled supplied-snapshot fact planner. It blocks missing viewer/subject players, ride info, ride attack-speed facts, and abnormal-effect facts rather than hydrating live state. The next safe step is composing these fact plans with population packet construction metadata.
