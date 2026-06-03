# Phase 6 Session 2369 Completion - Gate Autogroup Client Packets By Config

## Scope

Ported the Java `AutoGroupConfig.AUTO_GROUP_ENABLE` guard for `CM_AUTO_GROUP`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/configs/main/AutoGroupConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_MESSAGE.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` checks `AutoGroupConfig.AUTO_GROUP_ENABLE` before window dispatch.
- When disabled, Java sends `PacketSendUtility.sendMessage(player, "Auto Group is disabled")` and returns.
- `PacketSendUtility.sendMessage(Player, String)` sends `SM_MESSAGE` with sender id `0`, no sender name, and `ChatType.GOLDEN_YELLOW`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `GameServerOptions.AutoGroup.Enabled` and Java config binding for `gameserver.autogroup.enable`, defaulting to `true`.
- Added the disabled guard to `GameServerConnection.HandleAutoGroupAsync(...)`, sending `SmMessage("Auto Group is disabled")` and returning before all autogroup window handling.
- Extended `GameServerOptionsTests` to cover the default and Java config override.
- Added a live `GameServerConnection` packet-ingress test proving disabled autogroup sends the Java text message and stops window dispatch.

Known limitations:

- Only `AUTO_GROUP_ENABLE` was ported from `AutoGroupConfig`; schedule, period, start time, and announce settings remain partial or missing.
- The test verifies the Java text and packet type through C# packet ingress, not a Java-generated golden packet.

## Validation Decision

- Changed surface: live connection dispatch, config binding, and `SM_MESSAGE` text packet reuse.
- Specific behavior/contract: all `CM_AUTO_GROUP` windows are blocked when `gameserver.autogroup.enable=false`; the player receives golden-yellow system chat text `Auto Group is disabled`; no window-specific branch runs.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GamePacketTests.SmMessage" --no-restore
```

Result: passed 8, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl`; Java source review identified the exact disabled guard and message packet behavior.
- Broad-validation trigger: live connection dispatch and config binding were touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered live dispatch, config loading, and the reused `SmMessage` packet shape. No shared packet primitive, persistence repository, scheduler primitive, serialization helper, or data loader changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` | `Aion.GameServer.Configuration.GameServerAutoGroupOptions` | Config | Partial | Unit Tested | Partial Parity | `AUTO_GROUP_ENABLE` is bound from `gameserver.autogroup.enable`; schedule, period, start time, and announce settings remain partial or missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested | Partial Parity | Disabled guard now sends `SmMessage` and returns before window dispatch. Windows `100`-`104` remain partial; window `105` is a Java no-op in this tree. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendMessage(Player,String)` / `SM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage(string)` | Packet/Utility | Partial | Unit Tested | Partial Parity | Reuses existing golden-yellow system chat shape for the disabled autogroup message. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupDisabledSendsJavaTextMessageAndStopsWindowDispatch` | Unit | Java source review | `CM_AUTO_GROUP` ingress sends `Auto Group is disabled` and returns before window dispatch when the config is disabled. | Live C# packet-ingress test using `SmMessage` observer. | Not a Java-generated golden packet. |
| `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit | Java source review | `gameserver.autogroup.enable` defaults to enabled. | Config loader test. | Only covers the enable flag. |
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesOverrides` | Unit | Java source review | `gameserver.autogroup.enable=false` maps to `AutoGroup.Enabled=false`. | Config loader test. | Other `AutoGroupConfig` settings remain outside this UOW. |
| Existing `GamePacketTests.SmMessage_WritesGoldenYellowSystemChatPayload` | Unit | Java source review | `SmMessage(string)` preserves Java `SM_MESSAGE` golden-yellow text packet shape. | Focused packet test. | Not a runtime Java golden file. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` window `105` remains an explicit Java no-op/deferred branch.
- Java duplicate already-registered system message behavior for duplicate start-looking remains missing.
- `AutoGroupUtility.sendSuccessfulRegistration`, quick-entry refill, penalties, and full auto-instance lifecycle remain missing.
- C# queue matching and live auto-instance creation remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2369] Gate autogroup client packets by config
```
