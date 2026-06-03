# Phase 6 Session 2368 Handoff - Autogroup Request-Icon Handling

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2368-Completion.md`
- `docs/Phase-6-Session-2368-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2368, wired `CM_AUTO_GROUP` window `104` request-icon click handling.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.

Still not proven or not implemented:

- `CM_AUTO_GROUP` window `105` remains deferred; Java currently comments out the old failed-enter service call.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` disabled behavior is missing.
- C# queue matching and live auto-instance creation remain missing, so runtime registrations are not produced by the start-looking path yet.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, `canRegisterGroupEntry`, and `sendSuccessfulRegistration` remain missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.

## Commits Made

- `[Phase 6][UOW-2368] Wire autogroup request icon handling`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/PeriodicInstanceRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PeriodicInstanceRegistrationServiceTests.cs`
- `docs/Phase-6-Session-2368-Completion.md`
- `docs/Phase-6-Session-2368-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PeriodicInstanceRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `CM_AUTO_GROUP.runImpl` or `PeriodicInstanceManager.handleRequest`; Java source review identified the exact branch behavior.

Broad-validation trigger: live connection dispatch was touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed. No shared packet primitive, scheduler primitive, persistence repository, serialization helper, or data loader changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `104` now dispatches to periodic request-icon handling. Window `105` and config-disabled message behavior remain missing. |
| `com.aionemu.gameserver.services.instance.PeriodicInstanceManager.handleRequest(...)` | `Aion.GameServer.Services.PeriodicInstanceRegistrationService.CreateRequestPacket(...)` plus `GameServerConnection.HandleAutoGroupAsync(...)` | Service | Partial | Unit Tested | Partial Parity | Open-mask check, missing-template no-op, level range, no cooldown check, and default `SM_AUTO_GROUP` window `0` packet intent are modeled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `0` serialization support is reused. Other `SM_AUTO_GROUP` windows remain outside this UOW. |

## Next Sequential UOW

Next sequential Java branch: inspect `CM_AUTO_GROUP` window `105`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- Search for the old/deferred `failedEnterDredgion` behavior and any related service remnants.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- Existing autogroup runtime/packet tests if any behavior is present.

Expected Java behavior to model:

- Window `105` currently contains only a commented-out `DredgionRegService.getInstance().failedEnterDredgion(player);` line and falls through with no side effect. Confirm whether any newer Java branch or related service still expects behavior before deciding whether C# should explicitly no-op with a breadcrumb.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.SmAutoGroup|FullyQualifiedName~PeriodicInstanceRegistrationServiceTests" --no-restore
```

Refine the filter to exact added/affected tests before running. Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch only if window `105` wiring changes.

## Safe Candidates

- Confirm and document/wire explicit `CM_AUTO_GROUP` window `105` no-op behavior.
- Add Java duplicate already-registered system message behavior for duplicate start-looking.
- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for autogroup enable/schedule/period defaults.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
