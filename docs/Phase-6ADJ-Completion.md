# Phase 6ADJ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1278
Status: Phase 6 continues; a disabled attack-speed fact resolver now exists for known-list packet construction metadata. Live known-list player-see dispatch remains disabled because the resolver is an approximation, is not wired into fact planning, runtime stat hydration is incomplete, socket dispatch is disabled, and Java runtime validation is missing.

## Session Summary

UOW-1278 added `PlayerKnownListAttackSpeedFactResolverService`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAttackSpeedFactResolverService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListAttackSpeedFactResolverServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListAttackSpeedFactResolver.md`
- `docs/Phase-6-BindPointTeleport-KnownListRideAttackSpeed-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADJ-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAttackSpeedFactResolverServiceTests|PlayerVisualStatsUpdateServiceTests|PlayerKnownListPacketConstructionFactPlanServiceTests" --nologo` passed 19 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 308 tests.
- No Java runtime packet capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Parallel Work

Parallel work discovery was performed before implementation.

| Agent | Task | Result |
|---|---|---|
| Explorer | Read-only audit of reusable C# item-template attack-speed extraction points and tests | Completed with no file edits; confirmed the isolated resolver shape and warned that known-list, `CHANGE_SPEED`, and stats packets can drift until the approximation is centralized. Agent was closed. |
| Orchestrator | Implement disabled resolver/tests/docs and commit | Completed locally. |

## Migration Parity Table - UOW-1278

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.container.PlayerGameStats.getAttackSpeed` | `Aion.GameServer.Services.PlayerKnownListAttackSpeedFactResolverService` | Stat Resolver / Metadata | Partial | Unit Tested | Partial Parity | C# now resolves default/main-hand/off-hand-quarter attack speed from supplied player inventory and item templates. Current attack speed equals base and does not apply Java `Stat2`, effects, caps, calculation types, or duplicate modifier rules. |
| `com.aionemu.gameserver.model.stats.calc.functions.AttackSpeedFunction` / `DuplicateStatFunction` | `PlayerKnownListAttackSpeedFactResolution.NeedsJavaStatParity` | Stat Function / Blocker Metadata | Not Started | Unit Tested | Needs Verification | Resolver explicitly reports `NeedsJavaStatParity`. Java duplicate-stat modifier selection is not implemented. |
| `com.aionemu.gameserver.model.stats.calc.Stat2` | future stat value model or adapter | Stat Primitive | Partial | No Tests | Needs Verification | Resolver does not model Java base/bonus/fixed-rate math or float-to-int truncation beyond integer item template speeds. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | future consumer of `PlayerKnownListAttackSpeedFactResolverService` through `PlayerKnownListPacketConstructionFactPlanService` | Controller Packet Fact Boundary | Partial | Unit Tested | Needs Verification | Resolver creates fact records compatible with known-list packet planning, but the fact planner is not wired to call it yet. Live controller dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` constructor speed/attack-speed capture | `PlayerKnownListPacketConstructionAttackSpeedFacts`; `SmEmotion` | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Resolver supplies constructor-parity attack-speed metadata. Java `RIDE` does not serialize base/current attack-speed fields; `CHANGE_SPEED` does. No Java golden-byte validation in this unit. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled resolver service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 Java duplicate-stat modifier resolver, 1 live stat-container adapter, 1 fact-planner resolver-consumption bridge, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Resolver is an approximation and must not be treated as Java stat parity.
- Current attack speed equals base attack speed; Java current speed can differ through stat functions, effects, and caps.
- Java duplicate stat modifier behavior, fusion/off-hand rules, `StatCapUtil`, calculation-type filtering, and live invalidation remain unimplemented.
- C# uses slot guards for two-hand filtering; Java's exact behavior uses equipment accessors/object identity.
- The known-list fact planner does not yet consume this resolver.
- Serialization parity was not newly tested against Java runtime output.
- Date/time behavior did not change.
- Reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled fact-planner resolver-consumption bridge.
- Scope:
  - allow `PlayerKnownListPacketConstructionFactPlanService` to consume an explicit resolved attack-speed fact result, or compose the new resolver in a separate adapter;
  - preserve existing supplied `RideAttackSpeedFacts` precedence;
  - keep missing/approximate resolver metadata explicit;
  - keep non-live and do not send packets.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Resolver-consumption bridge | fact-plan service/tests or new adapter/test pair | Medium | Best next executable metadata slice. |
| B | Effect-controller entry/timer design | docs/read-only | Medium | Needed before abnormal-effect live facts. |
| C | Pet visibility ordering audit | docs/read-only | Medium | Java pet visibility follows player info in `PlayerController.see`. |
| D | Java packet-observer design for player-see sequence | docs/read-only | Low/Medium | Needed before verified runtime parity. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Audit safest consumption bridge shape and precedence tests | read-only Java/C# inspection | all writes |
| Orchestrator | Implement resolver-consumption bridge/tests/docs | fact-plan service/test files or new adapter/test pair, docs | live dispatch/world mutation/socket sends |

### Do Not Parallelize

- Multiple agents editing `PlayerKnownListPacketConstructionFactPlanService.cs` or its tests.
- Resolver consumption and live known-list dispatch.
- Shared progress/handoff docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_EMOTION.java`
  - `game-server/src/com/aionemu/gameserver/model/stats/container/PlayerGameStats.java`
  - `game-server/src/com/aionemu/gameserver/model/stats/calc/functions/PlayerStatFunctions.java`
  - `game-server/src/com/aionemu/gameserver/model/stats/calc/Stat2.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAttackSpeedFactResolverService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPacketConstructionFactPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListAttackSpeedFactResolverServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPacketConstructionFactPlanServiceTests.cs`
- Latest completed commits:
  - `07a43b8f1 [Phase 6][UOW-1276] Track population packet fact sources`
  - `30fbc2fc8 [Phase 6][UOW-1277] Audit ride attack speed facts`
  - UOW-1278 should be committed as `[Phase 6][UOW-1278] Add attack speed fact resolver`
- Next commit after this handoff should be `[Phase 6][UOW-1279] ...` for resolver-consumption metadata or another selected safe unit.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
