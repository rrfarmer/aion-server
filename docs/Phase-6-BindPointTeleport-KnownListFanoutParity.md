# Phase 6 Bind-Point Teleport Known-List Fanout Parity

Date: May 26, 2026
Unit of Work: UOW-1247
Scope: Characterize bind-point action `3` fanout mismatch between Java known-list broadcast and current C# visible-distance registry fanout.
Source of truth: Java project.

## Summary

UOW-1247 adds an executable characterization for the current C# bind-point action `3` fanout approximation. The test proves that C# routes through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` with `includeSourcePlayer: true`, includes the source and same-world players within `WorldVisibility.DefaultVisibleDistance`, and excludes out-of-range or different-world players.

This is not verified Java parity. Java sends to the source first and then iterates the source player's known-list players, including known-but-not-visible members. C# still lacks persistent known-list membership for this path.

## Java Source Findings

- `BindPointTeleportService.teleport` broadcasts action `3` only after scheduled Kinah decrease and cooldown storage.
- Java uses `PacketSendUtility.broadcastPacket(player, packet, true)`.
- `broadcastPacket(player, packet, true)` sends to the source player first, then calls the known-list broadcast.
- The known-list broadcast iterates `object.getKnownList().forEachPlayer(...)`.
- Java `KnownList` can contain visible and invisible known objects; `forEachPlayer` does not filter by cached visibility.
- The source player is not in its own known list, so the explicit self-send avoids duplicate self delivery.
- After the source send, Java recipient order follows `ConcurrentHashMap.values()` traversal and should not be treated as deterministic.

## C# Characterization

The current C# runtime fanout remains:

```text
BindPointTeleportRuntimeFanoutService
  -> IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)
  -> current registry/world visibility approximation
```

The new test uses a fake registry to characterize the approximation:

- source player included,
- same-world player at 94m included,
- same-world player at 96m excluded,
- different-world player excluded,
- direct `SendPacketToPlayerAsync` is not called,
- packet payload remains action `3`, player id, loc id, cooldown seconds.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeFanoutServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 187 tests.
- No Java runtime fanout capture was executed.
- No known-list-backed C# fanout implementation was added.

## Migration Parity Table - UOW-1247

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportRuntimeFanoutService.BroadcastFanoutPlanAsync` | Network Utility / Fanout | Partial | Regression Tested | Partial Parity | C# includes source and visible same-world players through registry fanout. Java sends self first, then known-list players. C# does not yet model known-list membership or self-first per-recipient ordering. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | current C# registry/`WorldVisibility` approximation | Known-List / Visibility | Not Started | Unit Tested | Needs Verification | Discovered dependency. Java known-list membership can include invisible known players; C# test only characterizes same-world/95m visibility filtering. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `BindPointTeleportRuntimeFanoutService`; `BindPointTeleportFanoutPlanService` | Service / Callback Fanout | Partial | Regression Tested | Needs Verification | Action `3` packet shape and C# visible-distance fanout are tested. Live scheduled callback dispatch remains disabled and Java known-list parity is not proven. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Regression Tested | Needs Verification | Action `3` payload shape is asserted in C# tests. No Java golden-byte runtime comparison in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BroadcastFanoutPlanAsync_TeleportCooldownActionThreeUsesSourceIncludedVisibleDistanceFanoutWithoutDispatch` | Characterization / Regression | Java action `3` fanout and current C# registry fanout | Current C# includes source plus same-world player within 95m, excludes 96m and different-world players, and emits action `3` packet shape. | C# approximation only. | Does not verify Java known-list membership, known-but-not-visible recipients, self-first direct send, or Java runtime bytes. |

## Remaining Risks

- Current C# fanout is still a visible-distance approximation, not Java known-list parity.
- Java sends source first through a direct send before known-list iteration; C# registry broadcast has no executable self-first guarantee.
- Java known-list can include invisible known players; C# `WorldVisibility` excludes them.
- Live scheduled callback dispatch, final movement, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list membership, and movement parity remain `Needs Verification`.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 characterization regression test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 known-list membership model, 1 self-first fanout executor, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a non-live known-list-backed fanout plan or expected Java trace model for bind-point broadcasts. It should represent source-first delivery plus known-list-player recipients without using distance-only filtering, and it should remain unwired from `GameServerConnection`.
