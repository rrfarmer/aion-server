# Phase 6ACF Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1248
Status: Phase 6 continues; bind-point scheduled Kinah now has SQL/send adapter seams, visible-distance fanout characterization, and a non-live expected Java known-list fanout trace model. Live known-list membership, source-first executor, movement, and `GameServerConnection` dispatch remain disabled.

## Session Summary

UOW-1248 added `BindPointTeleportKnownListFanoutTraceService`, a non-live source-derived model for Java bind-point fanout. It projects source-first delivery followed by represented known-list player recipients, without distance-only filtering or registry sends.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutTraceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKnownListFanoutTraceServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACF-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutTraceServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 191 tests.
- No Java runtime fanout capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1248

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutTraceService` | Network Utility / Expected Trace | Partial | Unit Tested | Needs Verification | Models source-first send plus known-list recipients as source-derived metadata only. No live send, registry use, source-online gate execution, or Java runtime capture. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive(VisibleObject,AionServerPacket)` | `BindPointTeleportKnownListFanoutTraceService` via login cooldown fanout plan | Network Utility / Expected Trace | Partial | Unit Tested | Needs Verification | Login cooldown fanout preserves `broadcastPacketAndReceive` Java utility metadata while projecting the same source-first plus known-list recipient shape. Runtime ordering is not golden-verified. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | represented known-list recipient input list in `BindPointTeleportKnownListFanoutTraceService` | Known-List Traversal | Partial | Unit Tested | Needs Verification | Trace consumes represented known-list player ids, including known-but-not-visible recipients. It does not implement persistent known-list membership or per-recipient exception behavior. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | expected Java action `3` fanout trace model | Service / Callback Fanout | Partial | Unit Tested | Needs Verification | Captures expected post-Kinah cooldown fanout shape only. Live scheduled callback dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` in trace packet metadata | Packet / Serialization | Partial | Regression Tested | Needs Verification | Packet shape remains source-derived; no Java golden-byte comparison added by the trace model. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live expected Java fanout trace service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 persistent known-list membership model, 1 source-online send gate executor, 1 per-recipient exception policy executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Trace model is metadata only and does not implement live known-list membership.
- Source `player.isOnline()` gate and per-recipient known-list exception handling are documented but not executed.
- Java source duplicate in known-list is normally excluded by add/update paths; corrupt/manual duplicate behavior is not modeled.
- Live scheduled callback dispatch, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a persistent known-list membership design or adapter prerequisite for bind-point fanout.
- Scope:
  - Define how C# represents known-but-not-visible players.
  - Define owner/source exclusion.
  - Define whether source-online and per-recipient exception policies live in a future executor.
  - Keep live fanout executor and `GameServerConnection` dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Known-list membership design/adapter prerequisite | docs or new metadata service/tests | Medium | Best next fanout blocker. |
| B | Source-online/per-recipient exception policy model | new trace/policy tests | Low/Medium | Could be separate from membership storage. |
| C | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Still blocked after fanout. |

### Do Not Parallelize

- Known-list membership and live movement execution in the same unit.
- `GameServerConnection` dispatch with metadata-only fanout traces.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutTraceService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFanoutPlanService.cs`
  - `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- Latest completed commits:
  - `0af62a4e0 [Phase 6][UOW-1247] Characterize bind point teleport known-list fanout gap`
  - next commit should be `[Phase 6][UOW-1248] Add bind point teleport known-list fanout trace`

Keep live bind-point behavior disabled until known-list membership, source-first fanout execution, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
