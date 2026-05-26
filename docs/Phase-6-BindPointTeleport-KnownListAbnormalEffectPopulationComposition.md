# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Population Composition

Date: May 26, 2026
Unit of Work: UOW-1285
Scope: Auto-attach disabled abnormal-effect resolver metadata during population packet fact planning from supplied snapshots.
Source of truth: Java project.

## Summary

UOW-1285 adds an opt-in population-plan adapter for abnormal-effect resolver metadata.

Population planning can now receive supplied abnormal-effect snapshot entries by subject player object id and attach an explicit disabled `PlayerKnownListAbnormalEffectFactResolution` to generated packet fact-plan requests. This mirrors the attack-speed auto-composition pattern while keeping live behavior disabled:

- request-level packet construction facts remain authoritative;
- request-level abnormal-effect facts and explicit resolutions are preserved;
- resolver execution occurs only when snapshot dictionaries are supplied;
- missing opt-in snapshots produce explicit blocked metadata;
- no live `EffectController` hydration, no timer calculation, and no socket sends occur.

## Java Source Findings

- `KnownList.findVisibleObjects` and `KnownList.updateVisibility` lead to `PlayerController.see`/`notSee` callbacks in Java.
- `PlayerController.sendPlayerInfoPackets` reads live player/effect state rather than staging packet facts through request objects.
- `SM_ABNORMAL_EFFECT(Creature)` consumes `EffectController.getAbnormals()` and `EffectController.getAbnormalEffects()`.
- The C# population adapter is an intentional disabled staging boundary used until Java-equivalent live EffectController hydration exists.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectFactPlanRequestAdapterService.cs`:

- attaches resolver output to abnormal-effect packet fact-plan requests;
- preserves supplied entries, masks, and explicit resolver metadata;
- leaves non-abnormal requests unchanged;
- returns explicit missing-snapshot metadata when opt-in snapshots are absent.

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`:

- `PlayerKnownListPopulationPlanRequest` now accepts `AbnormalEffectSnapshotsByPlayerObjectId`;
- generated owner/candidate fact-plan requests pass through the abnormal-effect adapter before planning.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAbnormalEffectFactPlanRequestAdapterServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListPopulationPacketConstructionDiagnosticServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|SmAbnormalEffect" --nologo` passed 46 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 334 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1285

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Known-List Population Planner | Partial | Unit Tested | Needs Verification | Population planning can attach disabled abnormal-effect resolver metadata from supplied snapshots. Java scans live region known-list state and invokes controller callbacks; C# remains non-live and request driven. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPopulationPlanService`; generated fact-plan requests | Controller Known-List Boundary | Partial | Unit Tested | Needs Verification | Generated player-see packet fact plans can receive abnormal-effect resolver metadata. Live callback execution, pet visibility, source-first fanout, and socket sends remain disabled. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListAbnormalEffectFactPlanRequestAdapterService`; `PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary / Adapter | Partial | Unit Tested | Needs Verification | Adapter supplies disabled resolver output to the fact planner. Java reads live player/effect-controller state directly and has no request adapter. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `PlayerKnownListAbnormalEffectFactResolverService`; adapter metadata | Effect Controller / Resolver Boundary | Partial | Unit Tested | Needs Verification | Snapshot dictionaries are caller supplied; no live `StampedLock` map hydration, add/remove lifecycle, broadcasts, no-show classification, or live timers are implemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | generated `PlayerKnownListOperationSideEffectPacketConstructionFacts.AbnormalEffects` | Packet Fact Dependency | Partial | Unit Tested | Partial Parity | Packet-compatible facts can now be generated through population planning from snapshot entries. No Java golden-byte or runtime packet comparison was run. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `PlayerKnownListAbnormalEffectSnapshotEntry` consumed by adapter/resolver | Effect DTO / Snapshot | Partial | Unit Tested | Needs Verification | Entry data and remaining-time values are supplied. Java duration/end-time calculation, NPC 24h sentinel, overflow behavior, and task scheduling remain unmodeled. |
| `com.aionemu.gameserver.skillengine.model.SkillTargetSlot` | resolver slot filtering and `PlayerKnownListAbnormalEffectFacts.Slots` | Enum / Slot Metadata | Partial | Unit Tested | Needs Verification | Adapter preserves request slots; resolver filters supplied entries. Full enum conversion and `DispelSlotType` behavior remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AttachAbnormalEffectResolution_AddsResolverResultForAbnormalRequest` | Unit / Adapter | `PlayerController.sendPlayerInfoPackets`; `EffectController.getAbnormalEffects` | Adapter attaches resolver output from supplied snapshots. | C# resolver/result assertions. | No live EffectController or Java runtime comparison. |
| `AttachAbnormalEffectResolution_PreservesSuppliedFactsAndExplicitResolution` | Unit / Adapter | C# disabled staging boundary | Supplied packet facts and explicit resolver metadata are not overwritten. | C# identity assertions. | Java has no request adapter. |
| `AttachAbnormalEffectResolution_NonAbnormalRequestIsUnchanged` | Unit / Adapter | Java abnormal-effect branch conditional | Non-abnormal requests do not receive resolver metadata. | C# identity assertion. | Java runtime branch not executed. |
| `AttachAbnormalEffectResolution_WithOptInMissingSnapshotAddsBlockedMetadata` | Unit / Adapter | C# opt-in snapshot boundary | Missing opt-in snapshots become explicit missing-effect metadata. | C# status assertions. | Java would read live controller state. |
| `Plan_AttachesResolvedAbnormalEffectsToGeneratedFactPlanRequests` | Unit / Population Composition | `KnownList` -> `PlayerController.see` -> `SM_ABNORMAL_EFFECT` | Population planning attaches resolver metadata, completes generated fact planning, and produces packet construction facts. | C# population/fact-plan assertions. | No live known-list execution or Java packet capture. |
| `Plan_PreservesBlockedAbnormalEffectMetadataWhenOptInSnapshotIsMissing` | Unit / Population Composition | C# opt-in snapshot boundary | Missing opt-in snapshots keep fact plan blocked and packet construction partial. | C# blocker/partial-plan assertions. | Java would read live effect state. |

## Remaining Risks

- Auto-composition is snapshot driven and disabled; it is not Java `KnownList` or `PlayerController` parity.
- Live `EffectController` map hydration, ordering, locking, no-show toggle classification, broadcasts, effect lifecycle, and timer calculations remain missing.
- Remaining-time values are still supplied, not computed from Java duration/end-time state.
- Pet visibility and full player-see packet sequence validation remain incomplete.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior remains supplied because remaining display time is not computed.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live abnormal-effect request adapter, 1 population-plan optional snapshot bridge, 4 focused adapter tests, and 2 population composition tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java effect timing calculator, 1 Java runtime packet capture path, 1 live known-list packet dispatcher, 1 pet visibility sequence adapter, and 1 Java packet-order observer path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Design or implement the next deterministic abnormal-effect prerequisite: either a remaining-time calculation helper with explicit clock/snapshot inputs, or a player-see packet-order observer design that can later validate the full generated sequence against Java runtime output. Keep live dispatch disabled.
