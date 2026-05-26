# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Fact Resolver

Date: May 26, 2026
Unit of Work: UOW-1282
Scope: Add a disabled abnormal-effect fact resolver for known-list packet construction metadata.
Source of truth: Java project.

## Summary

UOW-1282 adds `PlayerKnownListAbnormalEffectFactResolverService`, a non-live resolver that normalizes supplied abnormal-effect snapshots into `SmAbnormalEffect` packet facts.

The resolver intentionally does not read a live C# `EffectController`. It models the packet-facing Java inputs that are currently available from supplied snapshots:

- abnormal bitmask from `Player.AbnormalState`;
- supplied effect entries with effector id, skill id, skill level, target slot id, target slot ordinal, and remaining display time;
- Java-style no-show toggle filtering;
- Java-style slot filtering for targeted packet refreshes;
- `-1` remaining-time sentinel passthrough for permanent or too-large effects.

The result carries `NeedsJavaEffectControllerParity = true`, `IsLive = false`, and `IsJavaEffectControllerParity = false`.

## Java Source Findings

- `SM_ABNORMAL_EFFECT(Creature)` reads `effected.getEffectController().getAbnormals()` and `getAbnormalEffects()`, with `SkillTargetSlot.FULLSLOTS`.
- `SM_ABNORMAL_EFFECT(Creature, int, Collection<Effect>, int)` filters effects by `(slots & effect.getTargetSlot().getId()) != 0` when slots are not `FULLSLOTS`.
- `effectType` is `2` for players and `1` for non-player creatures.
- Player packet entries write effector id, skill id, skill level, target-slot ordinal, and remaining display time.
- `EffectController.getAbnormalEffects()` filters no-show toggle effects.
- `Effect.getRemainingTimeToDisplay()` returns `-1` for duration `0`, NPC effects with duration at least `86400000`, or values larger than `Integer.MAX_VALUE`; otherwise it returns `endTime - System.currentTimeMillis()` truncated to `int`.
- `EffectController` uses a `StampedLock` and `LinkedHashMap`, so live map ordering/threading parity is not modeled by this resolver.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectFactResolverService.cs`:

- `PlayerKnownListAbnormalEffectFactResolutionStatus`;
- `PlayerKnownListAbnormalEffectSnapshotEntry`;
- `PlayerKnownListAbnormalEffectFacts`;
- `PlayerKnownListAbnormalEffectFactResolution`;
- `PlayerKnownListAbnormalEffectFactResolverService.Resolve(Player?, IReadOnlyList<PlayerKnownListAbnormalEffectSnapshotEntry>?, int)`.

The resolver produces facts compatible with current known-list packet construction, but it is not wired into fact planning or population planning yet.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAbnormalEffectFactResolverServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListPlayerSideEffectPacketConstructionServiceTests|SmAbnormalEffect" --nologo` passed 22 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 323 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1282

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.effect.EffectController` | `Aion.GameServer.Services.PlayerKnownListAbnormalEffectFactResolverService` | Effect Controller / Metadata Resolver | Partial | Unit Tested | Needs Verification | C# resolver consumes supplied snapshots and player abnormal mask only. It does not hydrate live `StampedLock` maps, add/remove effects, conflict handling, broadcasts, or live timers. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbnormalEffect`; `PlayerKnownListAbnormalEffectFacts` | Packet / Fact Dependency | Partial | Unit Tested | Partial Parity | Resolver emits entries compatible with the existing C# packet serializer and tests cover no-show filtering, slot filtering, mask, and remaining-time passthrough. No Java golden-byte or runtime packet comparison was run. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `PlayerKnownListAbnormalEffectSnapshotEntry` | Effect DTO / Snapshot | Partial | Unit Tested | Needs Verification | Snapshot contains only packet-facing fields. Java duration/end-time calculation, `Integer.MAX_VALUE` overflow handling, NPC 24h sentinel behavior, and task scheduling remain supplied or unmodeled. |
| `com.aionemu.gameserver.skillengine.model.SkillTargetSlot` | `SmAbnormalEffect.FullSkillTargetSlots`; resolver slot filtering | Enum / Slot Metadata | Partial | Unit Tested | Partial Parity | Resolver uses Java `FULLSLOTS = 127` and bitwise slot filtering. Full enum type and `DispelSlotType` conversion are not ported here. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | future consumer through `PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Boundary | Partial | Unit Tested | Needs Verification | Resolver creates abnormal-effect facts for future known-list fact planning, but fact planner/population planner do not consume it yet and live dispatch remains disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Resolve_UsesPlayerAbnormalMaskAndFiltersNoShowToggleLikeEffectController` | Unit / Resolver | `EffectController.getAbnormals`; `getAbnormalEffects`; `SM_ABNORMAL_EFFECT` | Player abnormal mask is used, no-show toggles are filtered, and metadata is non-live/non-parity. | Source-derived C# assertions. | No live effect map or Java runtime comparison. |
| `Resolve_FiltersEntriesByRequestedSlots` | Unit / Resolver | `SM_ABNORMAL_EFFECT` slot-filter constructor | Entries are filtered by `(slots & targetSlotId) != 0`. | Source-derived C# assertion. | Full `SkillTargetSlot` enum conversion is not modeled. |
| `Resolve_PreservesInputOrderAfterFiltering` | Unit / Resolver | `EffectController` uses `LinkedHashMap` and copies ordered values | Resolver preserves supplied snapshot ordering after no-show filtering. | Source-derived C# assertion. | Caller must preserve Java insertion order. |
| `Resolve_KeepsNonToggleNoShowEntriesForPacketSource` | Unit / Resolver | `EffectController.getAbnormalEffects` excludes only no-show toggles | Non-toggle `NOSHOW` entries are kept for packet-source snapshots. | Source-derived C# assertion. | Caller must compute `IsNoShowToggle` faithfully. |
| `Resolve_PreservesSnapshotRemainingTimeSentinels` | Unit / Resolver | `Effect.getRemainingTimeToDisplay` | `-1` remaining-time sentinel passes through unchanged. | Source-derived sentinel assertion. | End-time calculation and overflow behavior are not computed. |
| `Resolve_MissingInputsReturnExplicitBlockedMetadata` | Unit / Guard | C# supplied input boundary | Missing player/effect snapshots return explicit statuses and no facts. | C# guard assertion. | Java would have live creature/effect-controller state. |

## Remaining Risks

- Resolver is snapshot-only and must not be treated as live Java `EffectController` parity.
- Java effect map ordering, `StampedLock` behavior, conflict handling, broadcasts, no-show toggle classification, and add/remove lifecycle are not implemented.
- Remaining-time calculation is supplied, not computed from Java `endTime`/duration/task state.
- NPC/non-player effect-type hydration is not represented by this player-known-list resolver.
- Fact planner and population planner do not yet consume this resolver.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior remains incomplete because remaining display time is not computed.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled resolver service plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java effect timing calculator, 1 fact-planner resolver-consumption bridge, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled fact-plan consumption bridge for explicit abnormal-effect resolver results, mirroring the attack-speed bridge pattern: preserve supplied `AbnormalEffects`/mask precedence, consume explicit resolver facts only when supplied facts are missing, keep blocked metadata explicit, and do not send packets.
