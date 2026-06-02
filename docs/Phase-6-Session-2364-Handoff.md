# Phase 6 Session 2364 Handoff - Autogroup Start Queue Registration

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2364-Completion.md`
- `docs/Phase-6-Session-2364-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Tests and docs should support concrete parity work, not become standalone evidence/reporting loops.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution.

## Current State

Last completed UOW: UOW-2364, wired `CM_AUTO_GROUP` window `100` to queue registration.

Recent production parity slices:

- Autogroup instance leave cleanup and destroy workflow are wired into delayed teleport leave.
- Open-registration refresh packets after autogroup leave are planned and sent.
- Periodic registration service models Java open/close state transitions, broadcast plans, live dispatch through the online-player registry, exact opening messages, default schedule entries, and close-task intent storage.
- C# has a runtime owner for Java `AutoGroupService.lookingParties` close cleanup and a periodic close overload that invokes it after close broadcasts.
- `CM_AUTO_GROUP` window `100` now parses Java entry request ids and adds allowed player/team registrations to the looking-party queue.

Still not proven or not implemented:

- `CM_AUTO_GROUP` windows `101` through `105` remain deferred.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config disabled behavior is missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` are missing.
- Java `sendSuccessfulRegistration`, queue matching, instance creation, penalties, and quick-entry refill remain missing.
- Java periodic registration cron callback registration and real scheduled close task creation/cancellation remain missing.
- C# config override binding for autogroup schedules and periods remains missing.
- Full forced-exit packet fanout for instance destruction remains missing.
- Java map leave callback parity: `ConquerorAndProtectorService.getInstance().onLeaveMap(player)`.
- Pet position update and same-map spawn parity in `TeleportService.SpawnTask.run`.

## Commits Made

- `0e273dcf2 [Phase 6][UOW-2363] Wire autogroup close queue cleanup`
- `[Phase 6][UOW-2364] Wire autogroup start queue registration`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2364-Completion.md`
- `docs/Phase-6-Session-2364-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupRegistrationGuardPlanServiceTests" --no-restore
```

Result: passed 16, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.startLooking`; Java source review identified entry request ids and queue behavior.

Broad-validation trigger: live connection dispatch was touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered test compiled the affected project/dependencies and passed. No shared packet primitive, scheduler, persistence, or serialization helper changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.autogroup.EntryRequestType` | `Aion.GameServer.Services.AutoGroupEntryRequestType` | Enum | Complete | Unit Tested | Partial Parity | Ids `0`, `1`, `2` and invalid-id null behavior are covered. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `100` dispatches to queue registration. Windows `101`-`105`, config-disabled message, and icon request handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service | Partial | Unit Tested | Partial Parity | Mask lookup, entry request ids, level guard, duplicate same-mask no-op, and queue mutation are modeled. PvP arena availability, cooldowns, canRegisterNew/Quick/Group, success packets, matching, and announcements remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.createMembers(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` member id resolution | Runtime State | Partial | Unit Tested | Partial Parity | Group/alliance member ids are captured from C# runtime snapshots where available. Java filters online team members; C# currently trusts runtime member ids. |

## Next Sequential UOW

Recommended next production scope: wire `CM_AUTO_GROUP` window `101` cancellation into the C# looking-party queue.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup_WritesJavaCancelRegistrationWindowPayload" --no-restore
```

If live `GameServerConnection` cancellation dispatch is changed, add only the edited autogroup connection-dispatch test method if one is introduced.

Behavior under validation: `CM_AUTO_GROUP` window `101` should find the player's queue entry for the mask, remove the party if the player is leader, or remove only the member otherwise, and send `SM_AUTO_GROUP(maskId, 2)` according to Java cancellation semantics. Penalty scheduling can remain documented as pending if not implemented in that unit.

Focused Java/Maven command: not expected unless a targeted Java fixture is added. Use Java source review for `cancelRegistration`.

Broad-validation trigger: live connection dispatch if `GameServerConnection` begins handling window `101`. Start focused; document the trigger before any broad validation.

## Safe Candidates

- Wire `CM_AUTO_GROUP` window `101` cancel-registration behavior.
- Add already-registered system message behavior for duplicate start-looking.
- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for autogroup enable/schedule/period defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Model real delayed close scheduling with `ThreadPoolManager` only after deciding on broad scheduler validation.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
