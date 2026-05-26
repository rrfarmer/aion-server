# Phase 6 Bind-Point Teleport Known-List Fanout Send Policy

Date: May 26, 2026
Unit of Work: UOW-1250
Scope: Add non-live metadata for Java known-list fanout online gating and per-recipient continuation.
Source of truth: Java project.

## Summary

UOW-1250 adds `BindPointTeleportKnownListFanoutSendPolicyService`. It consumes the existing source-first known-list trace and projects Java send-time policy without sending packets.

## Java Source Findings

- `PacketSendUtility.broadcastPacket(player, packet, true)` sends the source first, then known-list players.
- `PacketSendUtility.sendPacket(player, packet)` sends only when `player.isOnline()` is true.
- `Player.isOnline()` checks whether `getClientConnection()` is non-null.
- `KnownList.forEachPlayer` delegates through `KnownList.forEach`.
- `KnownList.forEach` uses `CollectionUtil.forEach`, which logs recipient callback exceptions and continues.

## C# Implementation

- `BindPointTeleportKnownListFanoutSendPolicyService.CreatePolicy` projects per-recipient send status.
- Online recipients are marked `WouldSend`.
- Offline recipients are marked `SkippedOffline`.
- Supplied failing recipients are marked `FailedAndContinued`.
- The result records Java send and iteration method breadcrumbs.
- The service is not live and does not call `IGameClientConnectionRegistry` or `SendPacketAsync`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutSendPolicyServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 196 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1250

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutSendPolicyService` | Packet Utility / Send Policy | Partial | Unit Tested | Needs Verification | Models online-gated send policy only. No socket send or Java runtime comparison. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.isOnline` | supplied online-player facts in `CreatePolicy` | Runtime State / Online Gate | Partial | Unit Tested | Needs Verification | Online facts are supplied by tests/callers, not read from live connection state. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | `FailedAndContinued` recipient send status | Utility / Exception Policy | Partial | Unit Tested | Needs Verification | Models log-and-continue behavior as metadata. Java logging is not executed. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `BindPointTeleportKnownListFanoutTraceService`; `BindPointTeleportKnownListFanoutSendPolicyService` | Known-List Traversal / Send Policy | Partial | Regression Tested | Needs Verification | Trace order and send policy are represented. Runtime ordering and live known-list population are unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | source-first trace plus send-policy metadata | Network Utility / Expected Fanout Policy | Partial | Regression Tested | Needs Verification | Expected fanout policy is represented; live executor and socket sends remain disabled. |

## Remaining Risks

- Send policy is metadata only.
- Online state is supplied, not observed from live player connections.
- Java logging and exception paths are not executed.
- Live known-list population, socket sends, movement, `GameServerConnection`, and Java runtime capture remain missing.

## Next Recommended Unit of Work

Add a disabled source-first known-list fanout executor composition that consumes membership snapshots and send-policy projections without actually sending packets.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 metadata send-policy service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 disabled source-first fanout executor, 1 live socket send adapter, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
