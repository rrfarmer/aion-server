# Phase 6ACE Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1247
Status: Phase 6 continues; bind-point scheduled Kinah now has SQL and send adapter seams, plus a characterization test proving current C# action `3` fanout is still a visible-distance approximation rather than Java known-list parity.

## Session Summary

UOW-1247 added a focused characterization test for bind-point action `3` fanout. It documents and tests the current C# registry behavior: source included, same-world player within 95m included, out-of-range and different-world players excluded. Java remains the source of truth and uses source-first known-list fanout, so parity is still not verified.

Files changed:

- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeFanoutServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-KinahInventoryLiveSendAdapter-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACE-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeFanoutServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 187 tests.
- No Java runtime fanout capture was run.
- No `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1247

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportRuntimeFanoutService.BroadcastFanoutPlanAsync` | Network Utility / Fanout | Partial | Regression Tested | Partial Parity | C# includes source and visible same-world players through registry fanout. Java sends self first, then known-list players. C# does not yet model known-list membership or self-first per-recipient ordering. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | current C# registry/`WorldVisibility` approximation | Known-List / Visibility | Not Started | Unit Tested | Needs Verification | Discovered dependency. Java known-list membership can include invisible known players; C# test only characterizes same-world/95m visibility filtering. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `BindPointTeleportRuntimeFanoutService`; `BindPointTeleportFanoutPlanService` | Service / Callback Fanout | Partial | Regression Tested | Needs Verification | Action `3` packet shape and C# visible-distance fanout are tested. Live scheduled callback dispatch remains disabled and Java known-list parity is not proven. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Regression Tested | Needs Verification | Action `3` payload shape is asserted in C# tests. No Java golden-byte runtime comparison in this unit. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 characterization regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 known-list membership model, 1 self-first fanout executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Current C# fanout is still a visible-distance approximation, not Java known-list parity.
- Java sends source first through a direct send before known-list iteration; C# registry broadcast has no executable self-first guarantee.
- Java known-list can include invisible known players; C# `WorldVisibility` excludes them.
- Live scheduled callback dispatch, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a non-live known-list-backed fanout plan or expected Java trace model.
- Scope:
  - Represent source-first delivery plus known-list-player recipients.
  - Avoid distance-only filtering for the expected Java trace.
  - Keep live registry fanout and `GameServerConnection` dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Expected Java known-list fanout trace model | new service/test plus docs | Medium | Best next fanout parity slice. |
| B | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| C | Movement side-effect readiness audit | docs only | Low/Medium | Next major live blocker after fanout. |
| D | DB integration test design for Kinah SQL | docs or gated test skeleton | Medium | Keep off default CI unless explicitly enabled. |

### Do Not Parallelize

- Known-list trace model and live movement execution in the same unit.
- `GameServerConnection` dispatch with approximate fanout.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFanoutPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeFanoutServiceTests.cs`
  - `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- Latest completed commits:
  - `230a0f8bd [Phase 6][UOW-1246] Add bind point teleport Kinah inventory send adapter`
  - next commit should be `[Phase 6][UOW-1247] Characterize bind point teleport known-list fanout gap`

Keep live bind-point behavior disabled until known-list-backed fanout, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
