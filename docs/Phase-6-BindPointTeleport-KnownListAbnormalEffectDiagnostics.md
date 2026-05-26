# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Diagnostics

Date: May 26, 2026
Unit of Work: UOW-1284
Scope: Surface abnormal-effect fact source and resolver status in population packet-construction diagnostics.
Source of truth: Java project.

## Summary

UOW-1284 extends `PlayerKnownListPopulationPacketConstructionDiagnosticService` so generated fact-plan diagnostics expose abnormal-effect provenance:

- per fact plan `AbnormalEffectFactSource`;
- per fact plan `AbnormalEffectResolutionStatus`;
- aggregate abnormal-effect source counts;
- aggregate abnormal-effect resolver-status counts.

This mirrors the attack-speed diagnostic pattern from UOW-1281 and remains a C# staging diagnostic only. Java does not expose this intermediate metadata because `PlayerController` reads live player and `EffectController` state directly.

## Java Source Findings

- `PlayerController.see` and `sendPlayerInfoPackets` determine whether known-list player-info side-effect packets are needed and then construct them from live player state.
- `SM_ABNORMAL_EFFECT(Creature)` pulls abnormal mask and effect entries from `EffectController`.
- Diagnostic source/status metadata is an intentional C# migration aid, not a Java runtime artifact.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPacketConstructionDiagnosticService.cs`:

- `PlayerKnownListPopulationFactPlanDiagnostic` now includes abnormal-effect source/status metadata;
- `PlayerKnownListPopulationPacketConstructionDiagnosticPlan` now includes source/status count dictionaries;
- summary aggregation counts `None`, `Supplied`, and `ResolvedSnapshot` sources plus non-null resolver statuses.

Updated `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests.cs`:

- added resolved-snapshot abnormal-effect diagnostic coverage;
- updated existing complete/partial/attack-speed diagnostics to assert abnormal-effect `None` metadata where appropriate.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|SmAbnormalEffect" --nologo` passed 28 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 328 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1284

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPopulationPacketConstructionDiagnosticService` | Diagnostic / Controller Packet Metadata | Partial | Unit Tested | Intentional Difference | Java has no diagnostic fact-source/status surface. C# records metadata to track disabled packet-fact hydration work without executing live controller sends. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPopulationFactPlanDiagnostic.AbnormalEffectFactSource`; aggregate counts | Diagnostic / Known-List Metadata | Partial | Unit Tested | Needs Verification | Diagnostics can now report whether planned player-see packet facts used no abnormal facts, supplied facts, or explicit resolved snapshots. Live known-list callbacks remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | diagnostic source/status metadata over `PlayerKnownListAbnormalEffectFacts` | Packet Fact Diagnostic | Partial | Unit Tested | Needs Verification | Diagnostics expose abnormal-effect provenance for packet-compatible facts, but no Java golden-byte or runtime packet comparison was run. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `PlayerKnownListAbnormalEffectFactResolutionStatus` diagnostic counts | Effect Controller / Diagnostic Blocker | Partial | Unit Tested | Needs Verification | Diagnostics surface resolver status only. Live effect map hydration, timer calculation, ordering, and no-show classification are still missing. |
| `com.aionemu.gameserver.skillengine.model.Effect` | diagnostic projection over snapshot-derived packet entries | Effect DTO / Diagnostic Metadata | Partial | Unit Tested | Needs Verification | Entry details and remaining-time values remain snapshot supplied. Java end-time/duration behavior is not computed. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Summarize_AbnormalEffectResolverMetadataCountsResolvedSnapshot` | Unit / Diagnostic Projection | `PlayerController.sendPlayerInfoPackets`; `SM_ABNORMAL_EFFECT` | Resolved snapshot source/status appears per fact plan and in aggregate diagnostic counts. | C# diagnostic assertions. | Diagnostic-only; no live EffectController or Java runtime comparison. |
| Existing `Summarize_CompletePopulationMetadataPreservesCandidateAndOperationOrdering` update | Unit / Regression | Java player-see ordering | Non-abnormal fact plans are counted as abnormal-effect source `None`. | C# diagnostic assertions. | Java has no staging metadata. |
| Existing `Summarize_PartialMetadataSurfacesBlockedFactPlansAndPacketConstructionResults` update | Unit / Regression | C# blocked fact-plan metadata | Blocked ride-only fact plans still count abnormal-effect source `None`. | C# diagnostic assertions. | Java runtime state is not executed. |
| Existing `Summarize_AttackSpeedResolverMetadataCountsResolvedApproximation` update | Unit / Regression | Attack-speed diagnostic source/status | Attack-speed diagnostics coexist with abnormal-effect `None` metadata. | C# diagnostic assertions. | Java has no combined diagnostic surface. |

## Remaining Risks

- Diagnostics are C# staging metadata, not Java runtime behavior.
- Population planning does not yet auto-attach abnormal-effect resolver results.
- Live `EffectController` map hydration, `StampedLock` behavior, no-show toggle classification, effect lifecycle, broadcasts, and timer calculations remain missing.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior remains supplied because remaining display time is not computed.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 diagnostic metadata extension plus 1 new focused test and 3 regression assertion updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 population-plan abnormal-effect resolver adapter, 1 live EffectController map hydrator, 1 Java effect timing calculator, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a population-plan abnormal-effect resolver auto-composition adapter that can attach explicit disabled resolver metadata from supplied player snapshots and supplied abnormal-effect snapshot entries. Keep supplied request facts authoritative, keep resolver execution opt-in, and do not send packets.
