# Phase 6 Bind-Point Teleport Known-List Abnormal-Effect Fact-Plan Bridge

Date: May 26, 2026
Unit of Work: UOW-1283
Scope: Consume explicit disabled abnormal-effect resolver facts in known-list packet construction planning.
Source of truth: Java project.

## Summary

UOW-1283 extends `PlayerKnownListPacketConstructionFactPlanService` so packet construction planning can consume an explicit `PlayerKnownListAbnormalEffectFactResolution` when supplied packet facts are missing.

The bridge mirrors the existing ride attack-speed resolver pattern:

- supplied `AbnormalEffects`, abnormal mask, and slot facts remain authoritative;
- explicit resolver facts are consumed only for subjects marked as having abnormal effects;
- blocked or missing resolver facts preserve `MissingAbnormalEffectFacts`;
- non-abnormal subjects ignore stray abnormal-effect metadata;
- source/status metadata is recorded without enabling live packet dispatch.

This unit does not hydrate live `EffectController` state, compute Java remaining-time values, execute known-list callbacks, or send packets.

## Java Source Findings

- `PlayerController.see` and `sendPlayerInfoPackets` build player-info side-effect packets from the live subject player and its `EffectController`.
- `SM_ABNORMAL_EFFECT(Creature)` reads `EffectController.getAbnormals()` for the mask and `EffectController.getAbnormalEffects()` for packet entries.
- Java has no intermediate fact-plan request object; the C# source/status fields are intentional staging diagnostics for disabled migration work.
- Java abnormal-effect packet behavior still depends on live effect-map ordering, timer/end-time math, no-show toggle classification, and `SkillTargetSlot` filtering.

## C# Implementation

Updated `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPacketConstructionFactPlanService.cs`:

- added `PlayerKnownListPacketConstructionAbnormalEffectFactSource`;
- added optional `AbnormalEffectResolution` to `PlayerKnownListPacketConstructionFactPlanRequest`;
- added `AbnormalEffectFactSource` and `AbnormalEffectResolutionStatus` to fact-plan metadata;
- added resolver-consumption logic that preserves supplied facts before using explicit resolver output.

Updated `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPacketConstructionFactPlanServiceTests.cs` with abnormal-effect bridge coverage.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|PlayerKnownListPlayerSideEffectPacketConstructionServiceTests|SmAbnormalEffect" --nologo` passed 26 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 327 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1283

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary | Partial | Unit Tested | Needs Verification | Fact planning can now consume explicit disabled abnormal-effect resolver output when supplied facts are missing. Java executes directly from live player/effect-controller state; C# remains non-live and request driven. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPacketConstructionFactPlanRequest.AbnormalEffectResolution`; generated packet-construction facts | Controller Known-List Boundary | Partial | Unit Tested | Needs Verification | Resolver facts can reach packet-construction facts for planned player-see metadata. Live known-list callbacks, source-first fanout, and socket sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | `PlayerKnownListAbnormalEffectFacts`; `PlayerKnownListPacketConstructionAbnormalEffectFactSource` | Packet Fact Dependency / Diagnostic Metadata | Partial | Unit Tested | Partial Parity | Existing packet-compatible entries can now be sourced from explicit resolver facts. Supplied facts are authoritative. No Java golden-byte or runtime packet comparison was run. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `PlayerKnownListAbnormalEffectFactResolution` consumed by fact planner | Effect Controller / Resolver Boundary | Partial | Unit Tested | Needs Verification | Planner consumes disabled resolver metadata only. It does not hydrate Java-equivalent `StampedLock` maps, no-show classification, add/remove lifecycle, broadcasts, or live timers. |
| `com.aionemu.gameserver.skillengine.model.Effect` | `PlayerKnownListAbnormalEffectFacts` passed through fact planning | Effect DTO / Snapshot | Partial | Unit Tested | Needs Verification | Remaining-time values and packet entries are passed through from snapshots. Java duration/end-time math, NPC 24h sentinel, and overflow behavior remain outside this planner. |
| `com.aionemu.gameserver.skillengine.model.SkillTargetSlot` | `PlayerKnownListAbnormalEffectFacts.Slots`; fact-plan packet facts | Enum / Slot Metadata | Partial | Unit Tested | Needs Verification | Planner preserves slots from supplied or resolved facts; full enum conversion and `DispelSlotType` behavior are still not ported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_AbnormalEffectsUsesResolvedSnapshotWhenSuppliedFactsAreMissing` | Unit / Fact Planner | `PlayerController.sendPlayerInfoPackets`; `SM_ABNORMAL_EFFECT` | Explicit resolver facts complete abnormal-effect packet construction when supplied facts are absent. | C# fact-plan assertions. | No live EffectController or Java runtime comparison. |
| `Plan_SuppliedAbnormalEffectFactsRemainAuthoritativeOverResolvedSnapshot` | Unit / Fact Planner | C# disabled staging boundary | Supplied packet facts override explicit resolver output. | C# identity and metadata assertions. | Java has no staging precedence object. |
| `Plan_AbnormalEffectSubjectWithBlockedResolutionKeepsMissingFactBlocker` | Unit / Guard | C# supplied input boundary | Blocked resolver output still leaves `MissingAbnormalEffectFacts`. | C# blocker/status assertions. | Java would read live controller state instead of this resolver metadata. |
| `Plan_NonAbnormalSubjectDoesNotConsumeAbnormalEffectMetadata` | Unit / Guard | Java abnormal-effect branch conditional | Stray abnormal-effect metadata is ignored when subject direction facts say no abnormal effects. | C# source/status and packet fact assertions. | Java runtime branch not executed. |
| Existing `Plan_AbnormalEffectsWithSuppliedEntriesAndMaskCreatesFacts` update | Unit / Regression | `SM_ABNORMAL_EFFECT` packet facts | Supplied abnormal entries/mask/slots still create packet facts and report supplied source. | C# packet fact assertions. | No Java golden-byte comparison. |

## Remaining Risks

- Fact planning is still request driven and disabled; it is not live Java `PlayerController` parity.
- Live `EffectController` map hydration, `StampedLock` behavior, no-show toggle classification, effect lifecycle, broadcasts, and timer calculations remain missing.
- Resolver source/status metadata is not yet surfaced in population packet diagnostics.
- Population planning does not yet auto-attach abnormal-effect resolver results.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior remains supplied because remaining display time is not computed.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 fact-plan resolver-consumption bridge plus 4 focused tests and 1 regression assertion update
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java effect timing calculator, 1 population-plan abnormal-effect resolver adapter, 1 diagnostic source/status surface, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Expose abnormal-effect source/status metadata in population packet construction diagnostics, mirroring UOW-1281 for attack-speed diagnostics. Keep it diagnostic-only and non-live.
