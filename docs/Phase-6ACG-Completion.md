# Phase 6ACG Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1249
Status: Phase 6 continues; bind-point scheduled Kinah now has SQL/send adapter seams, visible-distance fanout characterization, a non-live expected Java known-list fanout trace, and a metadata-only player known-list membership prerequisite. Live known-list population/execution, source-online gating, per-recipient exception handling, movement, and `GameServerConnection` dispatch remain disabled.

## Session Summary

UOW-1249 added `PlayerKnownListMembershipService` and `BindPointTeleportKnownListFanoutMembershipAdapterService`. Together they model owner-player known-player membership metadata for bind-point fanout, retain known-but-not-visible players, exclude the owner/source through the normal add path, collapse duplicate object ids, and project snapshots into the existing source-first known-list fanout trace.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutMembershipAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListMembershipServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKnownListFanoutMembershipAdapterServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipMetadata.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACG-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipServiceTests|BindPointTeleportKnownListFanoutMembershipAdapterServiceTests" --nologo` passed 6 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 193 tests.
- No Java runtime fanout capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1249

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.knownObjects` | `Aion.GameServer.Services.PlayerKnownListMembershipService` | Known-List Membership / Metadata Store | Partial | Unit Tested | Needs Verification | C# now records owner-player to known-player membership snapshots and collapses duplicate known-player ids. It is metadata-only, not integrated with live world add/remove/update flows or two-way known-list maintenance. |
| `com.aionemu.gameserver.world.knownlist.KnownObject.isVisible` and `KnownList.sees` | `PlayerKnownListMembershipEntry.IsVisibleToOwner`; `PlayerKnownListMembershipService.GetKnownPlayerObjectIds(..., includeInvisible)` | Known-List Visibility Metadata | Partial | Unit Tested | Needs Verification | Visibility is stored separately from membership, and invisible known players remain in default fanout projection. No live `owner.canSee(object)` recomputation, sighted-player fanout, or see/notSee packets are implemented. |
| `com.aionemu.gameserver.world.knownlist.KnownList.isAwareOf` | `PlayerKnownListMembershipService.UpsertKnownPlayers` owner/source exclusion | Known-List Guard | Partial | Unit Tested | Needs Verification | Normal metadata add/update path excludes the owner/source. Corrupt/manual Java states where the owner is already present are not modeled. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `BindPointTeleportKnownListFanoutMembershipAdapterService` -> `BindPointTeleportKnownListFanoutTraceService` | Known-List Traversal Adapter | Partial | Unit Tested | Needs Verification | Adapter projects membership entries into source-first trace recipients, including invisible members. It does not execute live sends, online gating, exception handling, or Java `ConcurrentHashMap` runtime ordering. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutMembershipAdapterService` | Network Utility / Expected Trace | Partial | Regression Tested | Needs Verification | Source-first expected trace now has a membership metadata source. No socket send, registry replacement, Java runtime capture, or `GameServerConnection` dispatch was added. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 metadata services plus 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 source-online send gate executor, 1 per-recipient exception policy executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Metadata is not populated by live C# world known-list flows.
- Java two-way known-list side effects, `sendSee`, `notSee`, `notKnow`, and cleanup side effects are not implemented.
- Source `player.isOnline()` gating and per-recipient log-and-continue exception handling are still missing.
- Live scheduled callback dispatch, source-first socket fanout, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a non-live source-online and per-recipient exception policy model for bind-point known-list fanout.
- Scope:
  - Define source-send and known-recipient send result statuses.
  - Model Java `player.isOnline()` gating at send time.
  - Model per-recipient exception policy as log-and-continue metadata.
  - Keep actual socket sends, registry replacement, live executor, movement, and `GameServerConnection` dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Source-online/per-recipient exception policy model | new service/test pair plus fanout docs | Low/Medium | Best next fanout blocker. |
| B | Disabled source-first executor design | docs or metadata service/tests | Medium | Should consume membership snapshots but not send live packets. |
| C | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Still blocked after fanout. |

### Do Not Parallelize

- Live known-list population and live movement execution in the same unit.
- `GameServerConnection` dispatch with metadata-only fanout traces.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutMembershipAdapterService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutTraceService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
  - `docs/Phase-6-BindPointTeleport-KnownListMembershipMetadata.md`
- Latest completed commits:
  - `0af62a4e0 [Phase 6][UOW-1247] Characterize bind point teleport known-list fanout gap`
  - `af39bdb89 [Phase 6][UOW-1248] Add bind point teleport known-list fanout trace`
  - next commit should be `[Phase 6][UOW-1249] Add bind point teleport known-list membership metadata`

Keep live bind-point behavior disabled until known-list membership population, source-first fanout execution, source-online gating, per-recipient exception policy, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.

