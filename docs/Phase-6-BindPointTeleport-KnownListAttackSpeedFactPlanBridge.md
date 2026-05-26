# Phase 6 Bind-Point Teleport Known-List Attack-Speed Fact-Plan Bridge

Date: May 26, 2026
Unit of Work: UOW-1279
Scope: Add a disabled fact-planner consumption bridge for explicit attack-speed resolver results.
Source of truth: Java project.

## Summary

UOW-1279 lets `PlayerKnownListPacketConstructionFactPlanService` consume an explicit `PlayerKnownListAttackSpeedFactResolution` when ride attack-speed facts were not already supplied by the caller.

This keeps the existing C# precedence rule intact:

- supplied `RideAttackSpeedFacts` remain authoritative;
- an explicit resolver result is consumed only when supplied facts are absent;
- blocked resolver results keep the plan blocked with `MissingRideAttackSpeedFacts`;
- no resolver is auto-invoked by the fact planner;
- no live known-list dispatch or packet send is enabled.

The bridge adds metadata to the fact plan:

- `RideAttackSpeedFactSource`: `None`, `Supplied`, or `ResolvedApproximation`;
- `RideAttackSpeedResolutionStatus`: the explicit resolver result status when one was provided.

## Java Source Findings

- Java `PlayerController.sendPlayerInfoPackets` constructs player-see packets from live `Player`, stats, motion, ride, stance, and effect state.
- Java `SM_EMOTION` captures attack-speed values from `creature.getGameStats().getAttackSpeed()`.
- Java `PlayerGameStats.getAttackSpeed()` computes default/main-hand/off-hand-quarter base and then delegates current-value behavior to `getStat`.
- Java `AttackSpeedFunction`, `DuplicateStatFunction`, and `Stat2` still do not have C# parity in the known-list fact planner.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPacketConstructionFactPlanService.cs`:

- added `PlayerKnownListPacketConstructionAttackSpeedFactSource`;
- extended `PlayerKnownListPacketConstructionFactPlanRequest` with `RideAttackSpeedResolution`;
- extended `PlayerKnownListPacketConstructionFactPlan` with source/status metadata;
- added resolver-result consumption only as an explicit, caller-supplied input;
- preserved supplied `RideAttackSpeedFacts` precedence.

Updated `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPacketConstructionFactPlanServiceTests.cs` with resolver bridge coverage.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAttackSpeedFactResolverServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests" --nologo` passed 29 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 312 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1279

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary | Partial | Unit Tested | Needs Verification | C# can now consume an explicit disabled attack-speed resolver result for ride packet construction metadata, but it still does not read live `PlayerGameStats` or execute Java known-list callbacks. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` constructor speed/attack-speed capture | `PlayerKnownListPacketConstructionFactPlanRequest.RideAttackSpeedResolution`; `PlayerKnownListOperationSideEffectPacketConstructionFacts` | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Resolver-derived base/current attack-speed facts can flow into packet-construction facts when supplied facts are missing. Java `RIDE` does not serialize base/current attack-speed fields; `CHANGE_SPEED` does. No Java golden-byte validation in this unit. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | `PlayerKnownListAttackSpeedFactResolverService`; fact-plan resolver-result bridge | Stat Resolver / Metadata | Partial | Unit Tested | Partial Parity | Bridge consumes the existing approximation result only when explicitly provided. It does not auto-hydrate from live game stats or item templates. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | `PlayerKnownListAttackSpeedFactResolution`; fact-plan source/status metadata | Stat Function / Blocker Metadata | Not Started | Unit Tested | Needs Verification | Approximate resolver status is preserved in fact-plan metadata, but Java duplicate-stat modifier behavior remains unimplemented. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | No Tests | Needs Verification | Current-value math, float truncation, caps, and effect-modified attack speed are still not modeled by this bridge. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_RideSubjectUsesResolvedAttackSpeedWhenSuppliedFactsAreMissing` | Unit / Fact Planner | `PlayerController.sendPlayerInfoPackets`; `SM_EMOTION` constructor | Explicit resolver facts can satisfy ride attack-speed metadata when supplied facts are absent. | C# fact-plan assertion. | Resolver remains an approximation and is not Java runtime verified. |
| `Plan_SuppliedRideAttackSpeedFactsRemainAuthoritativeOverResolvedApproximation` | Unit / Fact Planner | C# staging precedence around supplied facts | Supplied ride attack-speed facts remain authoritative over an explicit approximation result. | C# precedence assertion. | Java has no supplied/resolved staging distinction. |
| `Plan_NonRideSubjectDoesNotConsumeAttackSpeedMetadata` | Unit / Fact Planner | Java ride-branch conditional in `PlayerController.sendPlayerInfoPackets` | Non-ride fact plans do not consume ride attack-speed metadata even if caller supplies unused inputs. | C# metadata assertion. | Java does not have this staging request shape. |
| `Plan_RideSubjectWithBlockedAttackSpeedResolutionKeepsMissingFactBlocker` | Unit / Fact Planner | C# supplied input boundary | A blocked resolver result preserves `MissingRideAttackSpeedFacts` and exposes the resolver status. | C# blocker/status assertion. | Java would have live player/game-data state. |

## Remaining Risks

- The bridge consumes explicit approximation results only; it does not hydrate live stats.
- Supplied/resolved precedence is a C# staging boundary, not Java runtime behavior.
- Current attack speed can differ from base in Java through stat functions, effects, caps, and duplicate modifiers.
- Java `Stat2`, `AttackSpeedFunction`, `DuplicateStatFunction`, fusion/off-hand modifier behavior, and live stat invalidation remain unimplemented.
- Population fact generation still does not auto-compose the resolver from player/item-template inputs.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled fact-plan consumption bridge plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 resolver auto-composition adapter, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live resolver auto-composition adapter that can create `RideAttackSpeedResolution` from a supplied subject player plus `ItemTemplateTable`, then attach that result to generated fact-plan requests while preserving explicit `RideAttackSpeedFacts` precedence. Keep it disabled and do not send packets.

## Update After UOW-1280

`PlayerKnownListAttackSpeedFactPlanRequestAdapterService` now exists and `PlayerKnownListPopulationPlanService` can optionally attach disabled resolver results to generated fact-plan requests when `ItemTemplates` are supplied. Supplied facts and explicit resolver results remain authoritative, and live dispatch remains disabled.
