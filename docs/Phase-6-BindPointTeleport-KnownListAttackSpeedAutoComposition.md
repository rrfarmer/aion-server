# Phase 6 Bind-Point Teleport Known-List Attack-Speed Auto-Composition

Date: May 26, 2026
Unit of Work: UOW-1280
Scope: Add a non-live adapter that attaches disabled attack-speed resolver results to generated fact-plan requests.
Source of truth: Java project.

## Summary

UOW-1280 adds `PlayerKnownListAttackSpeedFactPlanRequestAdapterService` and lets `PlayerKnownListPopulationPlanService` optionally attach `RideAttackSpeedResolution` to generated packet fact-plan requests when an `ItemTemplateTable` is supplied.

The adapter is conservative:

- it runs only for ride-mode fact-plan requests;
- supplied `RideAttackSpeedFacts` remain authoritative;
- explicit `RideAttackSpeedResolution` remains authoritative;
- missing item templates produce explicit blocked resolver metadata;
- no live stats, controller callbacks, world mutation, or socket sends are enabled.

## Java Source Findings

- Java `PlayerController.sendPlayerInfoPackets` reads live player state and constructs `SM_EMOTION` for ride-mode player-see packets.
- Java `SM_EMOTION` reads attack-speed values from `PlayerGameStats.getAttackSpeed()`.
- Java `PlayerGameStats.getAttackSpeed()` uses default/main-hand/off-hand-quarter base speed, then Java stat calculation can modify current speed.
- Java `AttackSpeedFunction`, `DuplicateStatFunction`, and `Stat2` still do not have C# parity in this known-list path.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAttackSpeedFactPlanRequestAdapterService.cs`:

- `AttachRideAttackSpeedResolution(request, itemTemplates)` returns the original request unless it is a ride request missing both supplied facts and an explicit resolver result;
- when eligible, it calls `PlayerKnownListAttackSpeedFactResolverService.Resolve(subject, itemTemplates)` and attaches the result.

Updated `PlayerKnownListPopulationPlanService`:

- `PlayerKnownListPopulationPlanRequest` now accepts optional `ItemTemplates`;
- fact-plan requests are adapted before they are planned and before generated packet facts are merged.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAttackSpeedFactPlanRequestAdapterServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAttackSpeedFactResolverServiceTests|PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests" --nologo` passed 33 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 316 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1280

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService`; `PlayerKnownListAttackSpeedFactPlanRequestAdapterService` | Controller Packet Fact Boundary / Adapter | Partial | Unit Tested | Needs Verification | Population planning can now attach disabled attack-speed resolver metadata to generated packet fact-plan requests when item templates are supplied. It still does not execute Java known-list callbacks or live controller sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` constructor speed/attack-speed capture | adapted `PlayerKnownListPacketConstructionFactPlanRequest.RideAttackSpeedResolution`; generated packet-construction facts | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Resolver-derived attack-speed facts can now reach generated packet-construction metadata through population planning. Java `RIDE` does not serialize base/current attack speed; no Java golden-byte validation occurred. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | `PlayerKnownListAttackSpeedFactResolverService`; `PlayerKnownListAttackSpeedFactPlanRequestAdapterService` | Stat Resolver / Adapter | Partial | Unit Tested | Partial Parity | Auto-composition uses the disabled static item-template approximation. Current speed equals base and live Java stat functions are still absent. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | resolver metadata propagated through fact-plan requests | Stat Function / Blocker Metadata | Not Started | Unit Tested | Needs Verification | Adapter preserves resolver status, but Java duplicate-stat modifier selection, fusion/off-hand modifiers, effects, caps, and current-value behavior remain missing. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | No Tests | Needs Verification | No Java `Stat2` float/base/current math or truncation behavior was implemented in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AttachRideAttackSpeedResolution_AddsResolverResultForRideRequest` | Unit / Adapter | `PlayerGameStats.getAttackSpeed` source-derived base rule | Eligible ride request receives disabled resolver metadata. | C# resolver fact assertion. | Approximation only; no Java runtime comparison. |
| `AttachRideAttackSpeedResolution_PreservesSuppliedFactsAndExplicitResolution` | Unit / Adapter | C# staging precedence | Supplied facts and explicit resolver results are not overwritten. | C# identity assertion. | Java has no staging request shape. |
| `AttachRideAttackSpeedResolution_NonRideRequestIsUnchanged` | Unit / Adapter | Java ride branch conditional | Non-ride requests do not receive ride attack-speed metadata. | C# identity assertion. | Java runtime not executed. |
| `Plan_AttachesResolvedRideAttackSpeedToGeneratedFactPlanRequests` | Unit / Population Composition | `PlayerController.sendPlayerInfoPackets`; `SM_EMOTION` constructor | Population planning attaches resolver metadata, completes the generated fact plan, and records generated packet facts. | C# population/fact-plan assertions. | No live known-list execution or Java packet capture. |

## Remaining Risks

- Auto-composition uses static item templates only and is still not Java stat parity.
- Current attack speed equals base attack speed; Java current speed can differ through effects, caps, duplicate modifiers, and stat functions.
- Missing item templates become explicit blocked metadata, but live Java game data access is not modeled.
- Supplied/resolved precedence remains a C# staging boundary.
- Java `Stat2`, `AttackSpeedFunction`, `DuplicateStatFunction`, live stat invalidation, and fusion/off-hand modifier behavior remain unimplemented.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live auto-composition adapter, 1 population-plan optional static-data bridge, and 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 Java runtime packet capture path, 1 live known-list packet dispatcher, and 1 reusable Java-equivalent current-stat calculation surface
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add source/status diagnostics for generated attack-speed resolver composition in population packet construction diagnostics, or move to the next hydration prerequisite such as abnormal-effect entry/timer resolution. Keep live dispatch disabled.

## Update After UOW-1281

Population packet-construction diagnostics now expose ride attack-speed fact source and resolver status per fact plan, plus aggregate counts. This makes supplied, resolved approximation, and missing/none attack-speed paths visible without changing packet construction or live dispatch.
