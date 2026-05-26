# Phase 6 Bind-Point Teleport Known-List Attack-Speed Diagnostics

Date: May 26, 2026
Unit of Work: UOW-1281
Scope: Surface attack-speed fact source/status metadata in population packet-construction diagnostics.
Source of truth: Java project.

## Summary

UOW-1281 extends `PlayerKnownListPopulationPacketConstructionDiagnosticService` so fact-plan diagnostics report ride attack-speed fact provenance:

- per fact plan: `RideAttackSpeedFactSource`;
- per fact plan: `RideAttackSpeedResolutionStatus`;
- aggregate counts by source;
- aggregate counts by resolver status.

This is diagnostic-only. It does not change packet construction, resolver behavior, population planning, live dispatch, or socket sends.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAttackSpeedFactPlanRequestAdapterServiceTests|PlayerKnownListAttackSpeedFactResolverServiceTests" --nologo` passed 34 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 317 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1281

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPopulationPacketConstructionDiagnosticService` | Diagnostic / Controller Packet Metadata | Partial | Unit Tested | Intentional Difference | Java does not expose diagnostic provenance for supplied/generated fact-plan inputs. C# diagnostic metadata is a non-live staging tool to track whether attack-speed facts were supplied, absent, or resolved by approximation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` constructor speed/attack-speed capture | `PlayerKnownListPopulationFactPlanDiagnostic.RideAttackSpeedFactSource`; `RideAttackSpeedResolutionStatus` | Packet Fact Diagnostic | Partial | Unit Tested | Needs Verification | Diagnostics can show whether ride attack-speed metadata came from supplied facts or resolved approximation. No Java runtime packet comparison was performed. |
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | diagnostic counts for `PlayerKnownListAttackSpeedFactResolverService` outputs | Stat Resolver Diagnostic | Partial | Unit Tested | Partial Parity | Diagnostics count resolver approximation status, but the underlying resolver remains static item-template approximation and not Java current-stat parity. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | resolver-status diagnostic metadata | Stat Function / Diagnostic Blocker | Not Started | Unit Tested | Needs Verification | Diagnostics make unresolved Java stat-function gaps visible; they do not implement modifier behavior. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | No Tests | Needs Verification | No stat primitive behavior changed. Current/base math, truncation, caps, and effects remain unimplemented. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Summarize_AttackSpeedResolverMetadataCountsResolvedApproximation` | Unit / Diagnostic Projection | `PlayerController.sendPlayerInfoPackets`; `PlayerGameStats.getAttackSpeed` | Resolved approximation source/status appears per fact plan and in aggregate diagnostic counts. | C# diagnostic assertions. | Diagnostic-only; no Java runtime comparison. |
| Existing `Summarize_CompletePopulationMetadataPreservesCandidateAndOperationOrdering` update | Unit / Regression | Java player-see ordering | Supplied ride attack-speed and non-ride fact plans are counted separately as `Supplied` and `None`. | C# diagnostic assertions. | Java has no staging metadata. |
| Existing `Summarize_PartialMetadataSurfacesBlockedFactPlansAndPacketConstructionResults` update | Unit / Regression | C# blocked fact-plan metadata | Blocked/missing attack-speed fact plans count as `None`. | C# diagnostic assertions. | Java runtime state is not executed. |

## Remaining Risks

- Diagnostics are C# staging metadata, not Java runtime behavior.
- The underlying resolver remains an approximation and must not be treated as Java stat parity.
- Java `Stat2`, `AttackSpeedFunction`, `DuplicateStatFunction`, live stat invalidation, effects, caps, and fusion/off-hand modifiers remain unimplemented.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 diagnostic metadata extension plus 1 new focused test and 2 regression assertion updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 Java runtime packet capture path, 1 live known-list packet dispatcher, and 1 reusable Java-equivalent current-stat calculation surface
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Move to the next hydration prerequisite, preferably a disabled abnormal-effect entry/timer resolver design or implementation slice. Keep live dispatch disabled.
