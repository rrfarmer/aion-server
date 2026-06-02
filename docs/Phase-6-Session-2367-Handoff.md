# Phase 6 Session 2367 Handoff - Autogroup Cancel-Enter Runtime Slice

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2367-Completion.md`
- `docs/Phase-6-Session-2367-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing focused `dotnet test` command is compile evidence for the affected project and dependencies.

## Current State

Last completed UOW: UOW-2367, wired a conservative `CM_AUTO_GROUP` window `103` cancel-enter runtime slice.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, live dispatch through the online-player registry, exact opening messages, default schedule entries, and close-task intent storage.
- C# has a runtime owner for Java `AutoGroupService.lookingParties` close cleanup and periodic close cleanup invokes it.
- `CM_AUTO_GROUP` window `100` now parses Java entry request ids and adds allowed player/team registrations to the looking-party queue.
- `CM_AUTO_GROUP` window `101` now removes queued registrations using Java leader/member semantics and sends cancel-window packets to Java-equivalent recipients.
- `CM_AUTO_GROUP` window `102` now finds registered auto instances by player/mask, removes group/alliance membership, applies base entry cooldown behavior, and sends `SM_AUTO_GROUP(maskId, 5)` when static autogroup data is available.
- `CM_AUTO_GROUP` window `103` now finds registered auto instances by player/mask, unregisters the player, and sends `SM_AUTO_GROUP(maskId, 2)` when static autogroup data is available.

Still not proven or not implemented:

- `CM_AUTO_GROUP` windows `104` and `105` remain deferred.
- C# queue matching and live auto-instance creation remain missing, so runtime registrations are not produced by the start-looking path yet.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config disabled behavior is missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` are missing.
- Java `sendSuccessfulRegistration`, instance creation, penalties, cancel-enter delayed removal, cancel-enter destroy/refill, member-cancel rematch checks, and quick-entry refill remain missing.
- Java periodic registration cron callback registration and real scheduled close task creation/cancellation remain missing.
- C# config override binding for autogroup schedules and periods remains missing.
- Full forced-exit packet fanout for instance destruction remains missing.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `dfcf40b88 [Phase 6][UOW-2366] Wire autogroup press enter runtime`
- `[Phase 6][UOW-2367] Wire autogroup cancel enter runtime`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `docs/Phase-6-Session-2367-Completion.md`
- `docs/Phase-6-Session-2367-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Result: passed 12, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.cancelEnter`; Java source review identified matching/no-op, unregister, penalty/refill, and window `2` packet behavior.

Broad-validation trigger: live connection dispatch was touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered test compiled the affected project/dependencies and passed. No shared packet primitive, scheduler, persistence repository, serialization helper, or data loader changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `103` now dispatches to cancel-enter runtime behavior. Windows `104`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.cancelEnter(...)` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService.CancelEnter(...)` plus `GameServerConnection.HandleAutoGroupAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Registered-player/mask lookup, missing no-op, unregister, and window `2` packet dispatch are modeled. Penalty scheduling and quick-entry/destroy handling remain missing. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance.unregister(...)` | `Aion.GameServer.Services.AutoGroupInstanceRuntimeState.Unregister(...)` | Runtime State | Partial | Unit Tested | Partial Parity | C# removes the player object id from registered runtime state. Java subclass and lifecycle side effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `2` serialization support is reused. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Next Sequential UOW

Next sequential Java branch: inspect `CM_AUTO_GROUP` window `104` request-icon behavior.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/PeriodicInstanceManager.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`

Expected Java behavior to model:

- `CM_AUTO_GROUP` window `104` calls `PeriodicInstanceManager.getInstance().handleRequest(player, instanceMaskId)`.
- Existing C# periodic registration support may already contain most packet planning for request-icon/open registration behavior.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Refine the filter to exact added/affected tests before running. Avoid full project or solution tests unless a broad trigger is documented.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for `PeriodicInstanceManager.handleRequest`.

Broad-validation trigger: live connection dispatch if `GameServerConnection` begins handling window `104`.

## Safe Candidates

- Wire `CM_AUTO_GROUP` window `104` request-icon behavior using existing periodic registration service support.
- Inspect `CM_AUTO_GROUP` window `105` after `104`.
- Add Java duplicate already-registered system message behavior for duplicate start-looking.
- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for autogroup enable/schedule/period defaults.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
