# Phase 6 Bind-Point Teleport Known-List Ride Attack-Speed Audit

Date: May 26, 2026
Unit of Work: UOW-1277
Scope: Read-only audit of ride attack-speed facts for known-list player-see packet construction.
Source of truth: Java project.

## Summary

Java `PlayerController.sendPlayerInfoPackets` emits `SM_EMOTION(player, EmotionType.RIDE, 0, player.ride.getNpcId())` when the seen player is in ride mode. The `SM_EMOTION` constructor reads the subject player's live `GameStats` and stores:

- `creature.getGameStats().getMovementSpeedFloat()`;
- `creature.getGameStats().getAttackSpeed().getBase()`;
- `creature.getGameStats().getAttackSpeed().getCurrent()`.

C# known-list packet construction can already carry ride movement speed and attack-speed values through supplied facts, but it does not yet compute attack speed from a Java-equivalent live stat container. `PlayerKnownListPacketConstructionFactPlanService` correctly blocks ride packet fact construction when `RideAttackSpeedFacts` is missing.

Important nuance: Java captures base/current attack-speed values in the `SM_EMOTION` constructor, but the `RIDE` branch does not serialize those two fields. They are serialized in the `CHANGE_SPEED` branch. For player-see ride construction, attack-speed facts are therefore constructor-parity metadata and future shared speed-fact prerequisites, while movement speed and ride id are the wire-visible ride concerns.

This unit is documentation-only. No production code changed, no live dispatch was enabled, and no parity was verified by runtime comparison.

## Java Source Findings

- `PlayerController.sendPlayerInfoPackets(Player player)` sends the ride packet only when `player.isInPlayerMode(PlayerMode.RIDE)`.
- Java `SM_EMOTION(Creature, EmotionType, int, int)` copies state, movement speed, and attack-speed values from `creature.getGameStats()`.
- Java `SM_EMOTION.writeImpl` writes base/current attack speed only for `EmotionType.CHANGE_SPEED`; `RIDE` writes optional ride NPC id plus three constant floats after the leading speed field.
- Java `PlayerGameStats.getAttackSpeed()` starts from:
  - default base `1500`;
  - main-hand weapon attack speed when equipped;
  - plus off-hand weapon attack speed divided by `4`, unless off-hand is the same object as main hand.
- Java `AttackSpeedFunction` is a `DuplicateStatFunction`.
- `DuplicateStatFunction` applies matching weapon modifiers from main hand, fused item, or off hand, choosing the maximum applicable non-PvP value when duplicates exist.
- Java `Stat2.getBase()` and `getCurrent()` truncate floats to `int`.

## C# Current State

- `SmEmotion` accepts `speed`, `baseAttackSpeed`, and `currentAttackSpeed` constructor values and serializes them for `EmotionType.ChangeSpeed`; for `Ride`, the current packet shape serializes ride id plus three constant floats, matching the current C# serializer behavior already covered elsewhere.
- `PlayerKnownListPacketConstructionFactPlanService` requires explicit `RideAttackSpeedFacts` when `SubjectIsInRideMode` is true.
- `PlayerKnownListOperationSideEffectPacketConstructionFacts` carries `RideBaseAttackSpeed` and `RideCurrentAttackSpeed`.
- `PlayerKnownListPlayerSideEffectPacketConstructionService` passes those values into `SmEmotion`.
- `PlayerVisualStatsUpdateService` has an isolated attack-speed approximation for visual speed updates:
  - default base `1500`;
  - equipped main-hand weapon attack speed;
  - plus off-hand attack speed divided by `4`;
  - static item template lookup;
  - no full Java `DuplicateStatFunction` parity.
- `SmStatsInfo` has broader static-data based stat calculation, including attack speed modifiers, but it is packet-local and not a reusable known-list fact hydrator.

## Migration Parity Table - UOW-1277

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary | Partial | Manual Only | Needs Verification | Java reads ride attack-speed facts from live `PlayerGameStats`; C# still requires supplied `RideAttackSpeedFacts` and blocks when absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride constructor path | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion`; known-list packet construction services | Packet / Ride Emotion | Partial | Manual Only | Needs Verification | C# packet can carry supplied speed/attack-speed values through construction metadata, but this audit did not add Java golden-byte validation. Java ride packet writes ride id plus constants after a leading speed field from game stats; base/current attack-speed fields are constructor metadata for `RIDE` and serialized for `CHANGE_SPEED`. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | `PlayerVisualStatsUpdateService.ResolveAttackSpeed`; future reusable known-list attack-speed resolver | Stat Resolver | Partial | Manual Only | Needs Verification | C# visual stats has a main/off-hand approximation, but no reusable Java-equivalent stat resolver for known-list packet facts. Modifiers, fused weapon duplicate rules, current/base distinction, and exact truncation remain unverified. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | future reusable attack-speed modifier resolver | Stat Function | Not Started | Manual Only | Needs Verification | Java duplicate-stat modifier selection is not ported as a reusable service for known-list fact planning. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | Manual Only | Needs Verification | Java truncates base/current floats to int and applies base/bonus/fixed bonus rates. C# known-list packet planning has no equivalent reusable primitive yet. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1277 | Documentation/readiness audit | Java `PlayerController`, `SM_EMOTION`, `PlayerGameStats`, `AttackSpeedFunction`, `Stat2`; C# known-list packet construction and visual stats surfaces | Identifies why ride attack-speed facts remain supplied/blocked and where a future resolver could start. | Manual source/C# inspection only. | No executable resolver, no Java runtime comparison, no packet golden capture. |

## Remaining Risks

- Attack-speed resolution remains supplied metadata for known-list ride packet construction and future shared speed-fact parity.
- Treating ride attack speed as ride-derived would be wrong; Java ride movement speed comes from ride/player state, while attack speed still comes from normal player weapon/stat calculation.
- C# visual stat attack-speed logic is an approximation, not proven Java parity.
- Java duplicate stat modifier behavior, fusion/off-hand rules, exact truncation, bonus/base/fixed-rate handling, and calculation-type filtering remain unverified.
- Threading and live stat invalidation behavior are not modeled.
- Serialization parity was not newly tested in this unit.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts; 1 attack-speed readiness audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 reusable attack-speed stat resolver, 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a small disabled attack-speed fact resolver that reuses the existing C# static item-template attack-speed approximation from `PlayerVisualStatsUpdateService` and returns explicit `NeedsJavaStatParity` metadata. Keep it non-live and do not wire it into packet dispatch until Java duplicate-stat modifier behavior is modeled or intentionally blocked. The resolver should be documented as normal player attack-speed resolution, not ride-derived speed.

## Update After UOW-1278

`PlayerKnownListAttackSpeedFactResolverService` now exists as a disabled approximation resolver. It returns `PlayerKnownListPacketConstructionAttackSpeedFacts` plus explicit `NeedsJavaStatParity` metadata for default, main-hand, and off-hand-quarter attack-speed cases, but it is not wired into known-list packet fact planning yet.
