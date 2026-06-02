# Phase 6 Session 2357 Completion - Refresh Open AutoGroup Registrations

## Scope

Ported the Java `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)` slice used after autogroup instance leave.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`

Java behavior used:

- `AutoGroupService.onLeaveInstance(player)` always calls `PeriodicInstanceManager.checkAndSendOpenRegistrations(player)` when autogroup is enabled.
- The periodic manager iterates opened mask IDs.
- It sends `SM_AUTO_GROUP(maskId, WND_ENTRY_ICON, false)` only when the player's level is inside the autogroup template range and portal cooldown does not disable the target instance map.
- `SM_AUTO_GROUP` entry-icon payload writes mask id, window id `6`, map id, message id, title id, open/close flag, trailing zero byte, and name string.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `PeriodicInstanceRegistrationService` with opened-registration state and Java-like refresh packet planning.
- Extended `AutoGroupInstanceLeaveRuntimeService` to attach open-registration refresh packets when its Java leave plan says to check open registrations.
- Updated delayed teleport leave handling to send those refresh packets after autogroup cleanup.
- Registered the periodic registration service in production DI and wired it through the shared autogroup leave runtime using current static data and portal cooldowns.
- Added packet test coverage for Java `SM_AUTO_GROUP` entry-icon open/close payloads.

Known limitations:

- Cron scheduling and timed registration open/close are not wired yet.
- `PeriodicInstanceManager.openRegistration`, close broadcast, and system opening messages are not live yet.
- Java quick-entry refill remains unwired.
- C# opened-registration set is available but initially empty until future registration scheduling/request code opens masks.

## Validation Decision

- Changed surface: production service plus packet shape and live leave packet emission.
- Specific behavior/contract: Java open-registration refresh after autogroup leave emits `SM_AUTO_GROUP` entry-icon packets only for opened masks where level and cooldown checks pass.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload" --no-restore
```

Result: passed 7, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for the periodic registration refresh path; Java source review and focused C# packet/runtime tests were used as evidence.
- Broad-validation trigger: packet shape and live leave packet emission.
- Broad .NET decision: skipped full project/solution validation after focused packet/runtime tests passed and compiled the affected project/dependencies.
- Why this scope is sufficient: the packet test covers the changed `SM_AUTO_GROUP` entry-icon shape, the periodic service test covers Java level/cooldown filtering, and the autogroup runtime test covers propagation from leave planning to refresh packets.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.checkAndSendOpenRegistrations(Player)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateOpenRegistrationPackets(...)` | Service | Partial | Unit Tested | Partial Parity | Open-registration refresh packet planning is ported for level and cooldown checks. Cron scheduling, open/close broadcasts, and system messages remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Entry-icon open/close payload is covered. Existing window-zero payload remains covered. Other windows still rely on prior implementation coverage. |
| `com.aionemu.gameserver.services.AutoGroupService.onLeaveInstance(Player)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.OnLeaveInstance(...)` | Live Runtime Adapter | Partial | Regression Tested | Partial Parity | Runtime now returns open-registration refresh packets when Java would call `checkAndSendOpenRegistrations`. Quick-entry refill remains missing. |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendInstanceLeaveMessageIfNeededAsync(...)` | Live Adapter | Partial | Regression Tested | Partial Parity | Delayed teleport leave now sends autogroup refresh packets returned by the runtime before teleport completion. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.CreateOpenRegistrationPackets_FiltersByLevelAndPortalCooldownLikeJavaPeriodicInstanceManager` | Unit | Java source review | Open-registration refresh sends packets only for open mask IDs where level/cooldown checks pass. | Focused service test plus Java source review. | Does not cover cron scheduling or close broadcasts. |
| `AutoGroupInstanceLeaveRuntimeServiceTests.OnLeaveInstance_MissingOrUnregisteredInstanceOnlyPlansOpenRegistrationRefresh` | Unit | Java source review | Autogroup leave result carries refresh packets when Java would call the periodic manager. | Focused runtime test. | Does not verify live socket bytes. |
| `GamePacketTests.SmAutoGroup_WritesJavaEntryIconOpenAndClosePayload` | Unit | Java source review | Entry-icon open/close payload matches Java `SM_AUTO_GROUP.writeImpl`. | Focused packet serialization test. | Other window ids are not newly tested here. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron scheduling and opening/closing broadcasts.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2357] Refresh open autogroup registrations
```
