# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Remaining Time

Date: May 26, 2026
Unit of Work: UOW-1286
Scope: Add deterministic Java-shaped remaining-time display calculation for abnormal-effect snapshots.
Source of truth: Java project.

## Summary

UOW-1286 adds a small deterministic helper for Java `Effect.getRemainingTimeToDisplay()` behavior.

The helper does not read a live clock or live `EffectController` state. It accepts explicit snapshot inputs:

- duration in milliseconds;
- effect end time in milliseconds;
- whether the effected creature is an NPC;
- current time in milliseconds.

It returns Java-shaped display values for permanent effects, NPC 24h effects, ordinary remaining times, upper overflow, and expired negative values.

## Java Source Findings

`com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeToDisplay()`:

- returns `-1` when `getDuration() == 0`;
- returns `-1` for NPC effects with duration at least `86400000`;
- computes `endTime - System.currentTimeMillis()`;
- returns `-1` if the computed value is greater than `Integer.MAX_VALUE`;
- otherwise returns the Java `long` to `int` cast value.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectRemainingTimeDisplayService.cs`:

- `PlayerKnownListAbnormalEffectRemainingTimeSnapshot`;
- `PlayerKnownListAbnormalEffectRemainingTimeDisplayService.Resolve(...)`;
- `NpcPermanentDisplayDurationMillis = 86_400_000`.

The helper is deterministic and explicit-clock only. It is not yet wired into `PlayerKnownListAbnormalEffectFactResolverService`; snapshot callers must still decide when to compute and supply the remaining display value.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAbnormalEffectRemainingTimeDisplayServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|PlayerKnownListAbnormalEffectFactPlanRequestAdapterServiceTests|PlayerKnownListPopulationPlanServiceTests|SmAbnormalEffect" --nologo` passed 33 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 341 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1286

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeToDisplay` | `Aion.GameServer.Services.PlayerKnownListAbnormalEffectRemainingTimeDisplayService` | Utility / Effect Timing Helper | Partial | Unit Tested | Partial Parity | Helper ports the deterministic display-time branch with explicit timestamp inputs. It is not wired to live effects, does not calculate duration, and has no Java runtime comparison. |
| `com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeMillis` | `PlayerKnownListAbnormalEffectRemainingTimeSnapshot.EndTimeUnixTimeMilliseconds`; `NowUnixTimeMilliseconds` | Utility / Time Snapshot | Partial | Unit Tested | Partial Parity | C# computes `endTime - now` from supplied values instead of calling `System.currentTimeMillis()`. This is intentional for deterministic planning. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `PlayerKnownListAbnormalEffectRemainingTimeSnapshot.EffectedIsNpc` | Creature Type Flag | Partial | Unit Tested | Needs Verification | NPC detection is a supplied flag. No live C# creature hierarchy or reflection/type check is performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | future snapshot remaining-time producer for `SmAbnormalEffectEntry.RemainingTimeToDisplayMillis` | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Helper can produce the integer value consumed by packet entries, but resolver callers are not yet wired to compute it. No Java packet comparison was run. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Resolve_NullOrZeroDurationMatchesJavaPermanentDisplaySentinel` | Unit / Timing Helper | `Effect.getDuration`; `getRemainingTimeToDisplay` | Null or zero duration returns `-1`. | Source-derived C# assertions. | No Java runtime comparison. |
| `Resolve_NpcDurationAtLeastTwentyFourHoursMatchesJavaSentinel` | Unit / Timing Helper | NPC 24h branch | NPC duration below threshold returns remaining time; at threshold returns `-1`. | Source-derived C# assertions. | NPC detection is supplied. |
| `Resolve_PlayerTwentyFourHourDurationUsesRemainingTime` | Unit / Timing Helper | NPC-only sentinel condition | Player 24h duration is not forced to `-1`. | Source-derived C# assertion. | No live creature type check. |
| `Resolve_RemainingTimeGreaterThanIntegerMaxMatchesJavaOverflowSentinel` | Unit / Timing Helper | `Integer.MAX_VALUE` branch | Upper overflow returns `-1`. | Source-derived C# assertion. | No Java runtime comparison. |
| `Resolve_OrdinaryRemainingTimeAndExpiredNegativeValuesUseJavaIntCast` | Unit / Timing Helper | `getRemainingTimeMillis` cast | Ordinary and expired negative values are returned via Java-shaped int cast. | Source-derived C# assertion. | Extreme lower overflow is not separately validated. |

## Remaining Risks

- Helper is deterministic and not live; it does not hydrate or inspect Java-equivalent `Effect` objects.
- Duration calculation, PVP duration scaling, cumulative resist duration, toggle timer selection, and `endTime` scheduling remain outside this helper.
- Resolver callers are not yet wired to compute snapshot `RemainingTimeToDisplayMillis` with this helper.
- Java runtime packet capture was not performed.
- Date/time behavior is only partial because current time is supplied rather than read from live runtime.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 deterministic timing helper plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java duration/endTime producer, 1 resolver integration point for computed remaining time, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Wire the deterministic remaining-time helper into a snapshot-entry factory or adapter that can build `PlayerKnownListAbnormalEffectSnapshotEntry` values from supplied duration/end-time snapshots, without reading live clocks or live `EffectController` state.
