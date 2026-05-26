# Phase 6 Bind-Point Teleport Known-List Fanout Execution Plan

Date: May 26, 2026
Unit of Work: UOW-1251
Scope: Compose bind-point known-list fanout metadata into a disabled source-first execution plan.
Source of truth: Java project.

## Summary

UOW-1251 adds `BindPointTeleportKnownListFanoutExecutionPlanService`. It composes the prior fanout plan, known-list membership snapshot, source-first trace, and send-policy metadata into one disabled execution plan.

The plan proves the Java-shaped pieces now fit together, but it still does not send packets.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutExecutionPlanServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 199 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1251

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutExecutionPlanService` | Service / Disabled Execution Plan | Partial | Unit Tested | Needs Verification | Composes packet fanout plan, membership snapshot, source-first trace, and send-policy metadata. No scheduled callback or socket send execution. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | execution plan plus trace/send-policy services | Network Utility / Expected Fanout Composition | Partial | Regression Tested | Needs Verification | Java fanout shape is represented in one disabled plan. No live socket send or Java runtime capture. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | membership service and execution plan service | Known-List Traversal Composition | Partial | Regression Tested | Needs Verification | Membership snapshots feed the execution plan. Live population and runtime ordering remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | send-policy service inside execution plan | Packet Utility / Send Policy Composition | Partial | Regression Tested | Needs Verification | Online/failure policy is composed but no actual send occurs. |

## Remaining Risks

- Execution plan is disabled and metadata-only.
- Live known-list population and live socket execution are missing.
- Java runtime fanout capture has not been run.
- Movement and `GameServerConnection` dispatch remain disabled.

## Next Recommended Unit of Work

Audit live C# world/player state to identify the safest future source for real known-list population, or add a disabled opt-in socket executor boundary that consumes the execution plan but remains unwired from `GameServerConnection`.

## Update After UOW-1252

`BindPointTeleportKnownListFanoutSocketExecutorService` now consumes the execution plan and records a disabled-by-default socket boundary. The enabled path can call `SendPacketToPlayerAsync` in source-first order, but it remains unwired from `GameServerConnection` and is not a live bind-point path.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled execution-plan composition service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live socket send adapter, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
