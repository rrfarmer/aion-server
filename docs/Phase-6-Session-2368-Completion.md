# Phase 6 Session 2368 Completion - Wire Autogroup Request-Icon Handling

## Scope

Wired a conservative `CM_AUTO_GROUP` window `104` slice for Java request-icon click handling.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` calls `PeriodicInstanceManager.getInstance().handleRequest(player, instanceMaskId)` for window `104`.
- `PeriodicInstanceManager.handleRequest` sends `SM_AUTO_GROUP(maskId)` only when the mask id is currently open, resolves to an `AutoGroupType`, and the player is inside the template level range.
- Unlike `checkAndSendOpenRegistrations`, `handleRequest` does not check portal cooldown.
- Missing open registration, missing autogroup template, and out-of-range players are no-ops.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `PeriodicInstanceRegistrationService.CreateRequestPacket(...)` for Java `PeriodicInstanceManager.handleRequest` behavior.
- Wired `GameServerConnection.HandleAutoGroupAsync(...)` window `104` to send the Java-equivalent default `SmAutoGroup(autoGroup)` request-entry packet when the service returns one.
- Passed the shared `PeriodicInstanceRegistrationService` from `GameClientSocketServer` into each live `GameServerConnection`, so request clicks use the same open-registration state as scheduler/open-close flows.
- Added focused tests for open/level-range request packet behavior and closed/unknown/out-of-range no-op behavior.

Known limitations:

- No direct packet-ingress test was added for `CM_AUTO_GROUP` window `104`; service behavior and packet shape are covered, and live dispatch compiled through the focused command.
- Java `AutoGroupConfig.AUTO_GROUP_ENABLE` disabled message behavior remains missing.
- Real cron callbacks and scheduled close task creation/cancellation remain partial.

## Validation Decision

- Changed surface: live connection dispatch plus periodic registration service behavior and existing `SM_AUTO_GROUP` request-window packet shape.
- Specific behavior/contract: `CM_AUTO_GROUP` window `104` should send default window `0` `SM_AUTO_GROUP` only for an open registration whose template exists and whose level range admits the player; portal cooldown must not block this click branch.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `PeriodicInstanceManager.handleRequest`; Java source review identified the open-mask, type lookup, level-range, no-cooldown, and default-window packet behavior.
- Broad-validation trigger: live connection dispatch was touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled the affected game-server project/dependencies and directly covered service behavior plus existing `SmAutoGroup` request-window serialization. No shared packet primitive, persistence repository, scheduler primitive, serialization helper, or data loader changed.
- Why this scope is sufficient: the Java-derived request branch is tested at the service boundary and the selected packet shape is covered by existing packet tests; remaining untested live ingress risk is documented.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `104` now dispatches to periodic request-icon handling. Window `105` and config-disabled message behavior remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.handleRequest(...)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateRequestPacket(...)` plus `GameServerConnection.HandleAutoGroupAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Open-mask check, missing-template no-op, level range, no cooldown check, and default `SM_AUTO_GROUP` window `0` packet intent are modeled. Java singleton scheduling and close task runtime remain partial elsewhere. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `0` request-entry serialization is reused for window `104` handling. Other windows remain only partially covered. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.CreateRequestPacket_SendsDefaultRequestWindowForOpenLevelRangeLikeJavaHandleRequest` | Unit | Java source review | Open registration plus in-range player returns default request-window packet and ignores portal cooldown. | Focused service test. | Does not execute live client packet ingress. |
| `PeriodicInstanceRegistrationServiceTests.CreateRequestPacket_ClosedUnknownOrOutOfLevelRangeIsNoOpLikeJavaHandleRequest` | Unit | Java source review | Closed mask, missing static data, low level, and high level no-op. | Focused service test. | Does not verify Java exception behavior for impossible `SM_AUTO_GROUP(int)` constructor paths because C# safely returns null when static data is absent. |
| Existing `GamePacketTests.SmAutoGroup_WritesJavaWindowZeroPayload` | Unit | Java source review | Default `SM_AUTO_GROUP(maskId)` window `0` payload shape remains covered. | Focused packet test. | Not a Java-generated golden file. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` window `105` remains deferred.
- Java autogroup disabled config message remains missing.
- C# queue matching and live auto-instance creation remain missing.
- Java success registration packet fanout, quick-entry refill, penalties, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2368] Wire autogroup request icon handling
```
