# Phase 6ACO Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1257
Status: Phase 6 continues; C# now has a disabled region snapshot model and a non-live adapter into player known-list membership metadata. Full Java two-way known-list mutation and live bind-point dispatch remain disabled.

## Session Summary

UOW-1257 added `PlayerKnownListRegionMembershipAdapterService`, which consumes supplied `PlayerKnownListRegionSnapshot` values and upserts their candidate ids into `PlayerKnownListMembershipService` using the new `RegionSnapshotRefresh` update reason. It preserves existing membership by default and can optionally remove missing snapshot candidates.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionMembershipAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListRegionMembershipAdapterServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListRegionMembershipAdapter.md`
- `docs/Phase-6-BindPointTeleport-KnownListRegionSnapshot.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACO-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListRegionMembershipAdapterServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 224 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1257

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListRegionMembershipAdapterService` | Known-List Population Adapter | Partial | Unit Tested | Partial Parity | Region snapshot candidate ids can now seed membership metadata. Missing Java range, can-see, already-known checks, live region scan, two-way add order, and controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.add` | `PlayerKnownListMembershipService.UpsertKnownPlayers` via region adapter | Known-List Membership Mutation | Partial | Unit Tested | Needs Verification | Owner-side metadata upsert only. Does not mutate the candidate object's known-list first like Java. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | optional stale removal in `PlayerKnownListRegionMembershipAdapterService` | Known-List Cleanup / Visibility | Partial | Unit Tested | Needs Verification | Adapter preserves existing membership by default because Java can retain invisible known objects. Optional stale removal is not a Java-verified range/can-see implementation. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` with `RegionSnapshotRefresh` reason | Known-Object / Visibility Metadata | Partial | Unit Tested | Needs Verification | Caller supplies visible state because Java `owner.canSee` is not ported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | region snapshot adapter plus existing known-list fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Better non-live membership seeding exists, but no scheduled callback, socket execution, movement, or live dispatch wiring was added. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 region membership adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live region object store, 1 bidirectional known-list mutation engine, 1 range/can-see visibility engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Adapter is non-live and unwired.
- No live region object store exists.
- No two-way Java known-list mutation exists.
- Range, max visible-distance negotiation, `canSee`, hidden/search behavior, and already-known checks remain missing.
- Controller `see`/`notSee`/`notKnow` side effects remain missing.
- Optional stale removal is an approximation and must not be treated as verified Java parity.
- Threading differs from Java synchronized update over `ConcurrentHashMap`.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled two-way membership operation planner for player-player known-list add/remove semantics.
- Scope:
  - Plan Java's `newObject.getKnownList().add(owner)` before owner-side `add(newObject)`.
  - Model successful, duplicate, rejected, and remove paths as metadata.
  - Do not mutate live world objects or sockets.
  - Do not wire into `GameServerConnection`.
  - Document that actual object ownership, locking, and controller packet side effects remain missing.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Two-way membership operation planner | new service/tests/docs | Medium | Best next executable step. Keep non-live. |
| B | Known-list packet side-effect design | docs only | Low/Medium | Useful before dispatching `see`/`notSee` packets. |
| C | Region object store design audit | docs only | Low/Medium | Useful before live world lifecycle work. |
| D | Region membership adapter edge tests | tests only | Low | Add null/empty/remove policy cases if avoiding planner work. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect Java `KnownList.add/del/clear` ordering and current membership service constraints for a two-way planner | read-only Java/C# inspection | all writes |
| Worker | Implement disabled two-way player membership operation planner and focused tests | new service/test/doc files only | `GameServerConnection`, socket executor live wiring, world mutation paths |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes outside the planner |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Region object storage and controller packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionSnapshotService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionMembershipAdapterService.cs`
- Latest completed commits:
  - `724d683fe [Phase 6][UOW-1255] Audit player known-list population requirements`
  - `02e62487c [Phase 6][UOW-1256] Add player known-list region snapshot model`
  - next commit should be `[Phase 6][UOW-1257] Add player known-list region membership adapter`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
