# Phase 6 Session 2363 Handoff - Autogroup Close Queue Cleanup

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2363-Completion.md`
- `docs/Phase-6-Session-2363-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2363, wired C# autogroup looking-party close cleanup.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, live dispatch through the online-player registry, exact opening messages, default schedule entries, and close-task intent storage.
- C# now has a runtime owner for Java `AutoGroupService.lookingParties` close cleanup and a periodic close overload that invokes it after close broadcasts.

Still not proven or not implemented:

- `CM_AUTO_GROUP` window `100` does not yet populate the C# looking-party queue.
- `CM_AUTO_GROUP` windows `101` through `105` remain deferred.
- Java `AutoGroupService.startLooking`, matching, penalties, enter/cancel-enter, and quick-entry refill remain missing.
- Java periodic registration cron callback registration and real scheduled close task creation/cancellation remain missing.
- C# config override binding for autogroup schedules and periods remains missing.
- Full forced-exit packet fanout for instance destruction remains missing.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `3d23aefa5 [Phase 6][UOW-2361] Model periodic registration schedules`
- `19ad56d3c [Phase 6][UOW-2362] Model periodic close task intent`
- `[Phase 6][UOW-2363] Wire autogroup close queue cleanup`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2363-Completion.md`
- `docs/Phase-6-Session-2363-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests.CloseRegistrationAndBroadcastAsync_StopsLookingPartyRegistrationsAfterCloseBroadcastLikeJava|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `AutoGroupService.stopRegistrationsByMaskId`; Java source review identified the source-of-truth queue removal and packet loop.

Broad-validation trigger: live runtime state and packet dispatch were touched, but focused validation covered the isolated new service, adjacent periodic close path, and packet payload.

Broad .NET decision: skipped full project/solution validation after the focused test compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.lookingParties` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` | Runtime State | Partial | Unit Tested | Partial Parity | Mask-bucket storage and close cleanup are modeled. Start/cancel/match/penalty/quick-entry behavior remains missing. |
| `com.aionemu.gameserver.services.AutoGroupService.stopRegistrationsByMaskId(int)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StopRegistrationsByMaskIdAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Removes the mask bucket and sends window `2` packets to online queued members. C# skips packets when static autogroup data is missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.closeRegistration(int)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CloseRegistrationAndBroadcastAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Close broadcast now has a typed path to invoke looking-party cleanup after close packets. Real scheduled close task remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Window `2` payload shape is covered. Other Java windows remain covered only where prior tests exist. |

## Next Sequential UOW

Recommended next production scope: wire `CM_AUTO_GROUP` window `100` into C# looking-party registration for the narrowest Java-equivalent start-looking path.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/EntryRequestType.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- likely a focused `GameServerConnection` autogroup dispatch test if live packet handling is enabled.

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupRegistrationGuardPlanServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

If live `GameServerConnection` dispatch is changed, add only the edited autogroup connection-dispatch test class or method to the filter.

Behavior under validation: `CM_AUTO_GROUP` window `100` should follow Java `startLooking` guard ordering and add the player/team member ids to the C# looking-party queue only after registration is allowed.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for `CM_AUTO_GROUP.runImpl` and `AutoGroupService.startLooking` guard/queue behavior.

Broad-validation trigger: live connection dispatch if `GameServerConnection` begins handling `CmAutoGroup`. Start focused; document the trigger before any broad validation.

## Safe Candidates

- Wire `CM_AUTO_GROUP` window `100` into looking-party queue registration with Java guard ordering.
- Add `CM_AUTO_GROUP` window `101` cancel-registration behavior once queue population exists.
- Port `EntryRequestType` ids and validation if no C# equivalent exists.
- Add C# config option binding for autogroup schedule/period defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Model real delayed close scheduling with `ThreadPoolManager` only after deciding on broad scheduler validation.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
