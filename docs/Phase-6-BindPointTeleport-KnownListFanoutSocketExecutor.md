# Phase 6 Bind-Point Teleport Known-List Fanout Socket Executor

Date: May 26, 2026
Unit of Work: UOW-1252
Scope: Add a disabled-by-default opt-in socket executor boundary for bind-point known-list fanout.
Source of truth: Java project.

## Summary

UOW-1252 adds `BindPointTeleportKnownListFanoutSocketExecutorService`. It consumes the existing disabled execution plan and records the future socket boundary for Java-shaped bind-point fanout. The service does not run unless explicitly enabled and is not wired into `GameServerConnection`.

## Java Source Findings

- `PacketSendUtility.broadcastPacket(player, packet, true)` sends the source first.
- After source self-send, Java calls known-list traversal.
- Known-list recipient traversal is wrapped by `CollectionUtil.forEach`, so recipient exceptions are logged and traversal continues.
- Source self-send is outside `KnownList.forEachPlayer`, so this unit models a source-send exception as stopping traversal.
- Bind-point action `1`, action `2`, scheduled action `3`, and login cooldown all rely on this Java fanout family.

## C# Implementation

- Disabled default returns `DisabledNoSend` and never calls `SendPacketToPlayerAsync`.
- Enabled path sends recipients in execution-plan order.
- Offline-policy recipients are skipped.
- Missing registry returns `MissingRegistry`.
- Known-list recipient exceptions are `FailedAndContinued`.
- Source self-send exceptions are `FailedAndStopped`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutSocketExecutorServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 204 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1252

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutSocketExecutorService` | Network Utility / Disabled Socket Boundary | Partial | Unit Tested | Needs Verification | Opt-in executor preserves source-first ordering and known-list traversal order from metadata. Disabled by default. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `BindPointTeleportKnownListFanoutSocketExecutorService.ExecuteAsync` | Packet Utility / Socket Send Boundary | Partial | Unit Tested | Needs Verification | Enabled path can call registry sends. No Java runtime comparison. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | known-list recipient failure handling | Utility / Exception Policy | Partial | Unit Tested | Needs Verification | Known-list failures continue. Java logging is not executed. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | execution plan plus socket executor recipient loop | Known-List Traversal / Socket Boundary | Partial | Regression Tested | Needs Verification | Uses precomputed membership snapshot order, not live Java-equivalent region membership. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | known-list fanout execution plan plus socket executor boundary | Service / Callback Fanout Boundary | Partial | Regression Tested | Needs Verification | Boundary exists but scheduled callback dispatch remains disabled. |

## Remaining Risks

- Service is not registered or wired.
- Live known-list population is still missing.
- Java runtime fanout has not been captured.
- Movement and `GameServerConnection` dispatch remain disabled.

## Next Recommended Unit of Work

Add a small, test-first `PlayerKnownListMembershipRefreshService` that uses supplied online player candidates and `WorldVisibility` to seed approximate player-player membership metadata, while explicitly documenting that this is not full Java region/known-list parity.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled/opt-in socket executor service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live scheduled callback dispatch path, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
