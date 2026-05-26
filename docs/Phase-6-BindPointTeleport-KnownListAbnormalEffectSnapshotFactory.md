# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Snapshot Factory

Date: May 26, 2026
Unit of Work: UOW-1287
Scope: Build abnormal-effect snapshot entries from supplied packet-facing fields and deterministic timing snapshots.
Source of truth: Java project.

## Summary

UOW-1287 adds `PlayerKnownListAbnormalEffectSnapshotEntryFactoryService`, a non-live factory for creating `PlayerKnownListAbnormalEffectSnapshotEntry` values.

The factory accepts supplied packet-facing effect fields and either:

- preserves an explicit caller-supplied remaining display time; or
- computes remaining display time using `PlayerKnownListAbnormalEffectRemainingTimeDisplayService` from explicit duration/end-time/current-time/effected-is-NPC inputs.

If neither explicit remaining time nor complete timing inputs are supplied, it returns explicit `MissingTimingSnapshot` metadata and no entry. It does not read live clocks, hydrate `EffectController`, inspect live `Effect` objects, or send packets.

## Java Source Findings

- `SM_ABNORMAL_EFFECT` writes effector object id, skill id, skill level, target-slot ordinal, and `Effect.getRemainingTimeToDisplay()` for player targets.
- `Effect.getRemainingTimeToDisplay()` is the Java source for display remaining-time calculation.
- `EffectController.getAbnormalEffects()` supplies ordered effect objects after no-show toggle filtering; this factory does not model that controller state.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectSnapshotEntryFactoryService.cs`:

- `PlayerKnownListAbnormalEffectSnapshotEntryInput`;
- `PlayerKnownListAbnormalEffectSnapshotEntryFactoryStatus`;
- `PlayerKnownListAbnormalEffectSnapshotEntryFactoryResult`;
- `PlayerKnownListAbnormalEffectSnapshotEntryFactoryService.Create(...)`.

The existing resolver still consumes entries after they are created. This unit keeps snapshot construction separate from live resolver/planner composition.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAbnormalEffectSnapshotEntryFactoryServiceTests|PlayerKnownListAbnormalEffectRemainingTimeDisplayServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|PlayerKnownListAbnormalEffectFactPlanRequestAdapterServiceTests|PlayerKnownListPopulationPlanServiceTests|SmAbnormalEffect" --nologo` passed 37 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 345 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1287

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `Aion.GameServer.Services.PlayerKnownListAbnormalEffectSnapshotEntryFactoryService`; `PlayerKnownListAbnormalEffectSnapshotEntry` | Packet Fact Factory / DTO | Partial | Unit Tested | Partial Parity | Factory creates packet-facing snapshot entries compatible with the existing C# packet resolver. It does not serialize packets directly and no Java golden-byte comparison was run in this unit. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `PlayerKnownListAbnormalEffectSnapshotEntryInput`; `PlayerKnownListAbnormalEffectSnapshotEntry` | Effect DTO / Snapshot Factory | Partial | Unit Tested | Needs Verification | Packet-facing fields are supplied by callers. Java effect lifecycle, duration calculation, end-time scheduling, no-show toggle classification, and reflection/type behavior are not modeled. |
| `com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeToDisplay` | `PlayerKnownListAbnormalEffectRemainingTimeDisplayService` invoked by factory | Utility / Timing Dependency | Partial | Unit Tested | Partial Parity | Factory can compute display time through the deterministic helper when complete timing snapshots are supplied. No live clock or Java runtime comparison. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | future caller supplying `PlayerKnownListAbnormalEffectSnapshotEntryInput` values | Effect Controller / Source Dependency | Not Started | No Tests | Needs Verification | Live `StampedLock` map hydration, ordering, no-show toggle classification, broadcasts, add/remove lifecycle, and concurrency behavior remain missing. |
| `com.aionemu.gameserver.skillengine.model.SkillTargetSlot` | `TargetSlotId`; `TargetSlotOrdinal` inputs | Enum / Slot Metadata | Partial | Unit Tested | Needs Verification | Factory preserves supplied slot id and ordinal. Full enum conversion and `DispelSlotType` mapping remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Create_PreservesExplicitRemainingTimeAndPacketFacingFields` | Unit / Factory | `SM_ABNORMAL_EFFECT`; `Effect.getRemainingTimeToDisplay` | Explicit display time and packet-facing fields are preserved. | C# field assertions based on Java packet fields. | Caller still supplies fields; no Java runtime comparison. |
| `Create_ComputesRemainingTimeFromSuppliedTimingSnapshot` | Unit / Factory | `Effect.getRemainingTimeToDisplay` | Factory computes ordinary remaining time through deterministic helper. | Source-derived helper assertion. | No live effect/endTask. |
| `Create_UsesJavaRemainingTimeSentinelsForComputedSnapshots` | Unit / Factory | Java permanent/NPC 24h sentinel branches | Computed entries preserve Java `-1` sentinel cases. | Source-derived C# assertions. | NPC detection is supplied. |
| `Create_MissingTimingSnapshotReturnsExplicitBlockedMetadata` | Unit / Guard | C# snapshot boundary | Missing timing inputs return explicit metadata and no entry. | C# guard assertion. | Java would have live effect state. |

## Remaining Risks

- Factory is snapshot-only and does not represent live Java `Effect` or `EffectController` behavior.
- Duration calculation, end-time scheduling, toggle timer selection, PVP duration scaling, cumulative resist duration, and effect lifecycle remain outside this unit.
- Slot id/ordinal and no-show toggle status are still caller supplied.
- Java runtime packet capture was not performed.
- Date/time behavior is deterministic but partial because current time is supplied.
- Threading and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 snapshot-entry factory plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java duration/endTime producer, 1 full SkillTargetSlot enum/DispelSlotType mapper, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Use the snapshot-entry factory in a small composition layer that converts supplied abnormal-effect snapshot inputs into resolver-ready entry lists for population planning, or switch to the player-see packet-order observer design if runtime validation scaffolding is the safer next dependency.
