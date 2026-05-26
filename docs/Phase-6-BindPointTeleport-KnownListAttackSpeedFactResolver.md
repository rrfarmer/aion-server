# Phase 6 Bind-Point Teleport Known-List Attack-Speed Fact Resolver

Date: May 26, 2026
Unit of Work: UOW-1278
Scope: Add a disabled attack-speed fact resolver for known-list packet construction metadata.
Source of truth: Java project.

## Summary

UOW-1278 adds `PlayerKnownListAttackSpeedFactResolverService`, an isolated metadata resolver for normal player attack-speed facts.

The resolver intentionally models only the current C# static item-template approximation:

- default base/current attack speed `1500`;
- main-hand weapon template attack speed when equipped;
- plus off-hand weapon template attack speed divided by `4`;
- current attack speed equals base attack speed until Java `Stat2`, `AttackSpeedFunction`, duplicate-stat modifier behavior, effects, caps, and live stat invalidation are ported.

This is not live Java stat parity. The result carries `NeedsJavaStatParity = true`, `IsLive = false`, and `IsJavaStatParity = false`.

## Java Source Findings

- `PlayerGameStats.getAttackSpeed()` starts from default `1500`.
- If a main-hand weapon exists, Java uses its weapon template attack speed as the base.
- If an off-hand weapon exists and is not the same object as the main-hand weapon, Java adds `offHand.attackSpeed / 4`.
- Java returns `getStat(StatEnum.ATTACK_SPEED, base)`, so current values can be changed by stat functions/effects.
- `AttackSpeedFunction` inherits `DuplicateStatFunction`, which selects applicable weapon modifiers and duplicate behavior.
- `Stat2.getBase()` and `getCurrent()` truncate computed float values to `int`.
- Ride movement speed is ride/player-state derived, but ride attack speed is normal player weapon/stat attack speed.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAttackSpeedFactResolverService.cs`:

- `PlayerKnownListAttackSpeedFactResolutionStatus`;
- `PlayerKnownListAttackSpeedFactResolution`;
- `PlayerKnownListAttackSpeedFactResolverService.Resolve(Player?, ItemTemplateTable?)`.

The resolver returns `PlayerKnownListPacketConstructionAttackSpeedFacts` so future known-list fact-planning work can consume the same fact record already used by `PlayerKnownListPacketConstructionFactPlanService`.

It is not wired into live dispatch or the known-list fact planner yet. Missing player and missing item-template inputs return explicit blocked metadata with no facts.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAttackSpeedFactResolverServiceTests|PlayerVisualStatsUpdateServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests" --nologo` passed 19 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 308 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1278

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | `Aion.GameServer.Services.PlayerKnownListAttackSpeedFactResolverService` | Stat Resolver / Metadata | Partial | Unit Tested | Partial Parity | C# now resolves default/main-hand/off-hand-quarter attack speed from supplied player inventory and item templates. Current attack speed equals base and does not apply Java `Stat2`, effects, caps, calculation types, or duplicate modifier rules. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | `PlayerKnownListAttackSpeedFactResolution.NeedsJavaStatParity` | Stat Function / Blocker Metadata | Not Started | Unit Tested | Needs Verification | Resolver explicitly reports `NeedsJavaStatParity`. Java duplicate-stat modifier selection is not implemented. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | No Tests | Needs Verification | Resolver does not model Java base/bonus/fixed-rate math or float-to-int truncation beyond integer item template speeds. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | future consumer of `PlayerKnownListAttackSpeedFactResolverService` through `PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary | Partial | Unit Tested | Needs Verification | Resolver creates fact records compatible with known-list packet planning, but the fact planner is not wired to call it yet. Live controller dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` constructor speed/attack-speed capture | `PlayerKnownListPacketConstructionAttackSpeedFacts`; `SmEmotion` | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Resolver supplies constructor-parity attack-speed metadata. Java `RIDE` does not serialize base/current attack-speed fields; `CHANGE_SPEED` does. No Java golden-byte validation in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Resolve_WithoutWeaponUsesJavaDefaultAttackSpeedAsApproximation` | Unit / Resolver | `PlayerGameStats.getAttackSpeed` default base | No weapon returns `1500/1500` with non-live `NeedsJavaStatParity` metadata. | Source-derived default. | No Java stat functions/effects. |
| `Resolve_UsesMainHandAndQuarterOffHandWeaponAttackSpeed` | Unit / Resolver | `PlayerGameStats.getAttackSpeed` main/off-hand rule | Main-hand weapon plus off-hand quarter speed is resolved. | Source-derived arithmetic. | No duplicate modifiers, fusion, effects, or Java runtime comparison. |
| `Resolve_IgnoresOffHandCandidateWhenItOccupiesTwoHandedSlot` | Unit / C# Approximation Guard | Existing C# slot guard | Off-hand candidates occupying a two-hand slot are ignored by the approximation. | C# guard assertion. | Java compares off-hand object identity rather than this C# slot guard. |
| `Resolve_MissingInputsReturnExplicitBlockedMetadata` | Unit / Guard | C# supplied input boundary | Missing player/templates return explicit statuses and no facts. | C# safety boundary. | Java would have live player/game-data state. |

## Remaining Risks

- Resolver is an approximation and must not be treated as Java stat parity.
- Current attack speed equals base attack speed; Java current speed can differ through stat functions, effects, and caps.
- Java duplicate stat modifier behavior, fusion/off-hand rules, `StatCapUtil`, calculation-type filtering, and live invalidation remain unimplemented.
- C# uses slot guards for two-hand filtering; Java's exact behavior uses equipment accessors/object identity.
- The known-list fact planner does not yet consume this resolver.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled resolver service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 fact-planner resolver-consumption bridge, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled fact-planner resolver-consumption bridge: allow `PlayerKnownListPacketConstructionFactPlanService` to consume an explicit resolved attack-speed fact result, or compose the new resolver in a separate adapter, while preserving existing supplied `RideAttackSpeedFacts` precedence. Keep it non-live and do not send packets.

## Update After UOW-1279

`PlayerKnownListPacketConstructionFactPlanService` can now consume an explicit `PlayerKnownListAttackSpeedFactResolution` when supplied ride attack-speed facts are absent. Supplied `RideAttackSpeedFacts` remain authoritative, blocked resolver results still produce `MissingRideAttackSpeedFacts`, and no resolver is auto-invoked by the fact planner.
