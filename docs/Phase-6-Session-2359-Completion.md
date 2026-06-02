# Phase 6 Session 2359 Completion - Dispatch Periodic Registration Broadcasts

## Scope

Added a live dispatch adapter for Java periodic registration open/close broadcasts using the UOW-2358 broadcast plans.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/world/World.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`

Java behavior used:

- Open/close broadcast work is based on the current online player set from `World.forEachPlayer`.
- Each eligible player receives the planned `SM_AUTO_GROUP` packet, and open also sends the opening system message when present.
- Close broadcasts happen before `AutoGroupService.stopRegistrationsByMaskId(maskId)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `OpenRegistrationAndBroadcastAsync(...)` to collect online players from `IGameClientConnectionRegistry`, create the Java-shaped open plan, and send each planned packet to its selected player.
- Added `CloseRegistrationAndBroadcastAsync(...)` to collect online players, create the close plan, dispatch close packets, and then invoke an optional stop-registration callback.
- Added dispatch result metadata for sent-packet count and whether the stop-registration callback ran.
- Added focused tests for level-filtered live dispatch and Java close ordering.

Known limitations:

- Cron start expressions and timed close scheduling remain missing.
- Production callers for scheduled periodic registrations are not wired yet.
- Exact Java scheduled opening system-message helper methods remain missing.
- The stop-registration callback is available but not yet wired to C# autogroup queue state.

## Validation Decision

- Changed surface: production live fanout adapter over a non-live service.
- Specific behavior/contract: dispatch should send each packet in the Java-derived per-player plan through `SendPacketToPlayerAsync`, and close should send close packets before invoking the stop-registration callback.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for this path; Java source review plus focused C# dispatch tests were used as evidence.
- Broad-validation trigger: live fanout adapter was added, but the edited surface is isolated to `SendPacketToPlayerAsync` dispatch and covered by a fake registry.
- Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.
- Why this scope is sufficient: the edited service and its dispatch boundary are exercised directly; no packet primitive, persistence, scheduler, or shared socket implementation changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.openRegistration(SM_SYSTEM_MESSAGE,int,long)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.OpenRegistrationAndBroadcastAsync(...)` | Service Adapter | Partial | Unit Tested | Partial Parity | Online-player collection and packet dispatch are covered. Cron scheduling, close task storage, and exact opening-message factory helpers remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CloseRegistrationAndBroadcastAsync(...)` | Service Adapter | Partial | Unit Tested | Partial Parity | Close packet dispatch and broadcast-before-stop callback ordering are covered. Scheduled task cancellation and concrete autogroup stop wiring remain missing. |
| `com.aionemu.gameserver.world.World.forEachPlayer(...)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.ForEachOnlinePlayer(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Used to snapshot online players for periodic registration fanout. Existing registry implementation was not changed in this UOW. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` | Runtime Adapter | Partial | Unit Tested | Partial Parity | Dispatch call ordering is covered with a focused fake registry. Real socket bytes are covered indirectly by existing packet serialization tests. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PeriodicInstanceRegistrationServiceTests.OpenRegistrationAndBroadcastAsync_SendsPlannedPacketsToOnlineLevelRangeLikeJavaWorldFanout` | Unit | Java source review | Online-player fanout sends entry-icon and opening-message packets only to level-eligible players. | Focused service dispatch test. | Does not exercise real sockets. |
| `PeriodicInstanceRegistrationServiceTests.CloseRegistrationAndBroadcastAsync_SendsClosePacketsBeforeStopRegistrationsLikeJava` | Unit | Java source review | Close dispatch sends the close packet before invoking the stop-registration callback. | Focused ordering test. | Stop callback is not yet wired to autogroup queue state. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Java periodic registration cron scheduling and close-task cancellation.
- Exact scheduled opening system-message helper methods.
- Concrete stop-registration wiring into C# autogroup queue state.
- Java quick-entry queue refill after autogroup leave.
- Full forced-exit packet fanout for instance destruction.
- `ConquerorAndProtectorService.onLeaveMap` parity.
- Pet position update and same-map spawn behavior in delayed teleport completion.

## Commit

Commit message:

```text
[Phase 6][UOW-2359] Dispatch periodic registration broadcasts
```
