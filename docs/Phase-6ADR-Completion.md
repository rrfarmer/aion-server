# Phase 6ADR Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1286
Status: Phase 6 continues; a deterministic helper now models Java abnormal-effect remaining-time display behavior from explicit snapshots. Live known-list player-see dispatch remains disabled because live `EffectController` hydration/timers, snapshot-entry factory wiring, socket dispatch, and Java runtime validation are still missing.

## Session Summary

UOW-1286 added `PlayerKnownListAbnormalEffectRemainingTimeDisplayService`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectRemainingTimeDisplayService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListAbnormalEffectRemainingTimeDisplayServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListAbnormalEffectRemainingTime.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ADR-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListAbnormalEffectRemainingTimeDisplayServiceTests|PlayerKnownListAbnormalEffectFactResolverServiceTests|PlayerKnownListAbnormalEffectFactPlanRequestAdapterServiceTests|PlayerKnownListPopulationPlanServiceTests|SmAbnormalEffect" --nologo` passed 33 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|PlayerKnownListAttackSpeed|PlayerKnownListAbnormalEffect|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect|PlayerVisualStatsUpdate" --nologo` passed 341 tests.
- No Java runtime packet capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1286

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeToDisplay` | `Aion.GameServer.Services.PlayerKnownListAbnormalEffectRemainingTimeDisplayService` | Utility / Effect Timing Helper | Partial | Unit Tested | Partial Parity | Helper ports the deterministic display-time branch with explicit timestamp inputs. It is not wired to live effects, does not calculate duration, and has no Java runtime comparison. |
| `com.aionemu.gameserver.skillengine.model.Effect.getRemainingTimeMillis` | `PlayerKnownListAbnormalEffectRemainingTimeSnapshot.EndTimeUnixTimeMilliseconds`; `NowUnixTimeMilliseconds` | Utility / Time Snapshot | Partial | Unit Tested | Partial Parity | C# computes `endTime - now` from supplied values instead of calling `System.currentTimeMillis()`. This is intentional for deterministic planning. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `PlayerKnownListAbnormalEffectRemainingTimeSnapshot.EffectedIsNpc` | Creature Type Flag | Partial | Unit Tested | Needs Verification | NPC detection is a supplied flag. No live C# creature hierarchy or reflection/type check is performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_EFFECT` | future snapshot remaining-time producer for `SmAbnormalEffectEntry.RemainingTimeToDisplayMillis` | Packet Fact Dependency | Partial | Unit Tested | Needs Verification | Helper can produce the integer value consumed by packet entries, but resolver callers are not yet wired to compute it. No Java packet comparison was run. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 deterministic timing helper plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 live EffectController map hydrator, 1 Java duration/endTime producer, 1 resolver integration point for computed remaining time, 1 Java runtime packet capture path, and 1 live known-list packet dispatcher
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Helper is deterministic and not live; it does not hydrate or inspect Java-equivalent `Effect` objects.
- Duration calculation, PVP duration scaling, cumulative resist duration, toggle timer selection, and `endTime` scheduling remain outside this helper.
- Resolver callers are not yet wired to compute snapshot `RemainingTimeToDisplayMillis` with this helper.
- Java runtime packet capture was not performed.
- Date/time behavior is only partial because current time is supplied rather than read from live runtime.
- Reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a snapshot-entry factory or adapter for abnormal effects.
- Scope:
  - accept supplied packet-facing effect fields plus duration/end-time/current-time/effected-is-NPC inputs;
  - compute `RemainingTimeToDisplayMillis` via `PlayerKnownListAbnormalEffectRemainingTimeDisplayService`;
  - emit `PlayerKnownListAbnormalEffectSnapshotEntry`;
  - preserve explicit caller-supplied remaining-time values when provided;
  - do not read live clocks or live `EffectController` state.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Snapshot-entry factory | new isolated service/test files | Medium | Best next executable prerequisite. |
| B | Player-see packet-order observer design | docs/read-only | Low/Medium | Needed before Java runtime validation. |
| C | Pet visibility ordering audit | docs/read-only | Medium | Java pet visibility follows player info in `PlayerController.see`. |

### Do Not Parallelize

- Multiple agents editing abnormal-effect resolver/adapter files while snapshot factory shape is in flux.
- Live `EffectController` hydration and live known-list dispatch.
- Shared progress/handoff docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/skillengine/model/Effect.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ABNORMAL_EFFECT.java`
  - `game-server/src/com/aionemu/gameserver/controllers/effect/EffectController.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectRemainingTimeDisplayService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectFactResolverService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListAbnormalEffectFactPlanRequestAdapterService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListAbnormalEffectRemainingTimeDisplayServiceTests.cs`
- Latest completed commits:
  - `1f1d3c393 [Phase 6][UOW-1284] Add abnormal effect diagnostic sources`
  - `d7aa21aa1 [Phase 6][UOW-1285] Compose abnormal effect fact plans`
  - UOW-1286 should be committed as `[Phase 6][UOW-1286] Add abnormal effect remaining time helper`
- Next commit after this handoff should be `[Phase 6][UOW-1287] ...` for snapshot-entry factory, packet-order observer design, or another selected safe unit.

Keep live bind-point behavior disabled until Java-equivalent known-list population, runtime fact hydration, source-first fanout execution, live scheduled callback dispatch, concrete packet serializers, socket dispatch ordering, and Java packet/runtime validation have focused parity slices.
