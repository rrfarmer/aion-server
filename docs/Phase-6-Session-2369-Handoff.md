# Phase 6 Session 2369 Handoff - Autogroup Disabled Config Guard

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2369-Completion.md`
- `docs/Phase-6-Session-2369-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2369, ported the Java disabled autogroup guard for `CM_AUTO_GROUP`.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.

Still not proven or not implemented:

- `CM_AUTO_GROUP` window `105` is a Java no-op in this tree, with only a commented-out old `failedEnterDredgion` call.
- C# queue matching and live auto-instance creation remain missing, so runtime registrations are not produced by the start-looking path yet.
- Java duplicate already-registered message behavior for start-looking remains missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, `canRegisterGroupEntry`, and `sendSuccessfulRegistration` remain missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2369] Gate autogroup client packets by config`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2369-Completion.md`
- `docs/Phase-6-Session-2369-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GamePacketTests.SmMessage" --no-restore
```

Result: passed 8, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `CM_AUTO_GROUP.runImpl`; Java source review identified the exact disabled guard and message packet behavior.

Broad-validation trigger: live connection dispatch and config binding were touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed. No shared packet primitive, scheduler primitive, persistence repository, serialization helper, or data loader changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` | `Aion.GameServer.Configuration.GameServerAutoGroupOptions` | Config | Partial | Unit Tested | Partial Parity | `AUTO_GROUP_ENABLE` is bound from `gameserver.autogroup.enable`; schedule, period, start time, and announce settings remain partial or missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested | Partial Parity | Disabled guard now sends `SmMessage` and returns before window dispatch. Windows `100`-`104` remain partial; window `105` is a Java no-op in this tree. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendMessage(Player,String)` / `SM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage(string)` | Packet/Utility | Partial | Unit Tested | Partial Parity | Reuses existing golden-yellow system chat shape for the disabled autogroup message. |

## Next Sequential UOW

Next sequential Java branch: confirm/model `CM_AUTO_GROUP` window `105` explicit no-op, or move to the next substantive autogroup behavior because Java has no side effect for `105` in this tree.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- Search for old/deferred `failedEnterDredgion` behavior only to confirm no active service behavior remains.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- Existing autogroup runtime/packet tests if an explicit no-op breadcrumb is added.

Expected Java behavior to model:

- Window `105` currently contains only a commented-out `DredgionRegService.getInstance().failedEnterDredgion(player);` line and falls through with no side effect.

Focused validation recipe for an explicit no-op/breadcrumb UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmAutoGroup" --no-restore
```

Refine the filter to exact added/affected tests before running. Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch only if window `105` wiring changes.

## Safe Candidates

- Confirm and document/wire explicit `CM_AUTO_GROUP` window `105` no-op behavior.
- Add Java duplicate already-registered system message behavior for duplicate start-looking.
- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
