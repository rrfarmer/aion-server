# Phase 6 Session 2365 Handoff - Autogroup Cancel Queue Registration

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2365-Completion.md`
- `docs/Phase-6-Session-2365-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing focused `dotnet test` command is compile evidence for the affected project and dependencies.

## Current State

Last completed UOW: UOW-2365, wired `CM_AUTO_GROUP` window `101` to queue cancellation.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, live dispatch through the online-player registry, exact opening messages, default schedule entries, and close-task intent storage.
- C# has a runtime owner for Java `AutoGroupService.lookingParties` close cleanup and periodic close cleanup invokes it.
- `CM_AUTO_GROUP` window `100` now parses Java entry request ids and adds allowed player/team registrations to the looking-party queue.
- `CM_AUTO_GROUP` window `101` now removes queued registrations using Java leader/member semantics and sends `SM_AUTO_GROUP(maskId, 2)` cancel-window packets to Java-equivalent recipients.

Still not proven or not implemented:

- `CM_AUTO_GROUP` windows `102` through `105` remain deferred.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config disabled behavior is missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` are missing.
- Java `sendSuccessfulRegistration`, queue matching, instance creation, penalties, member-cancel rematch checks, and quick-entry refill remain missing.
- Java periodic registration cron callback registration and real scheduled close task creation/cancellation remain missing.
- C# config override binding for autogroup schedules and periods remains missing.
- Full forced-exit packet fanout for instance destruction remains missing.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `0e273dcf2 [Phase 6][UOW-2363] Wire autogroup close queue cleanup`
- `eb2ff1592 [Phase 6][UOW-2364] Wire autogroup start queue registration`
- `[Phase 6][UOW-2365] Wire autogroup cancel queue registration`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2365-Completion.md`
- `docs/Phase-6-Session-2365-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

Result: passed 14, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.cancelRegistration`; Java source review identified leader/member/no-op behavior and packet recipients.

Broad-validation trigger: live connection dispatch was touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered test compiled the affected project/dependencies and passed. No shared packet primitive, scheduler, persistence, serialization helper, or data loader changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `101` now dispatches to queue cancellation. Windows `102`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.cancelRegistration(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CancelRegistrationAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Missing search entry no-op, leader whole-party removal, member-only removal, and cancel-window packet recipients are modeled. Java penalties and rematch checks remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.isLeader(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration.LeaderObjectId` | Runtime State | Partial | Unit Tested | Partial Parity | C# stores the registration leader and uses it for cancel semantics. Java object/member lifecycle and online filtering are not fully modeled. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.unregisterMember(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CancelRegistrationAsync(...)` | Runtime State | Partial | Unit Tested | Partial Parity | Non-leader cancellation removes only the player's id and leaves the remaining party queued. Java post-removal rematch behavior remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `2` cancel-registration payload is reused and covered by packet test. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Next Sequential UOW

Next sequential Java branch: inspect `CM_AUTO_GROUP` window `102` `pressEnter(player, instanceMaskId)`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/RegisteredPlayer.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- Any existing C# autogroup instance/registration runtime state discovered during work discovery.

Initial risk: Java `pressEnter` appears to depend on matched auto-instance state and registered autogroup players. If C# has no equivalent runtime state yet, document the blocker and choose one of the safe candidates below instead of inventing a shortcut.

Focused validation recipe if window `102` can be wired:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroup" --no-restore
```

Refine the filter to the exact added/affected test names before running. Avoid full project or solution tests unless a broad trigger is documented.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for `pressEnter`.

Broad-validation trigger: live connection dispatch if `GameServerConnection` begins handling window `102`; scheduler/teleport broad validation only if those shared systems are edited.

## Safe Candidates

- Inspect and, if dependencies exist, wire `CM_AUTO_GROUP` window `102` `pressEnter` behavior.
- Add Java duplicate already-registered system message behavior for duplicate start-looking.
- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for autogroup enable/schedule/period defaults.
- Wire `CM_AUTO_GROUP` window `104` periodic request-icon behavior if existing `PeriodicInstanceRegistrationService` support is sufficient.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
