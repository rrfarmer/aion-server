# Phase 6 Session 2370 Handoff - Autogroup Window 105 No-Op

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2370-Completion.md`
- `docs/Phase-6-Session-2370-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2370, modeled `CM_AUTO_GROUP` window `105` as an explicit Java no-op.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- C# queue matching and live auto-instance creation remain missing, so runtime registrations are not produced by the start-looking path yet.
- Java duplicate already-registered message behavior for start-looking remains missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, `canRegisterGroupEntry`, and `sendSuccessfulRegistration` remain missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2370] Model autogroup window 105 no-op`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2370-Completion.md`
- `docs/Phase-6-Session-2370-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `CM_AUTO_GROUP.runImpl`; Java source review and repository search identified the no-op branch.

Broad-validation trigger: live connection dispatch was touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed. No shared packet primitive, scheduler primitive, persistence repository, serialization helper, data loader, or common model/state changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested | Partial Parity | Window `105` now has an explicit Java no-op branch in C#. Overall autogroup dispatch remains partial because successful-registration fanout, duplicate registration messaging, queue matching, and full lifecycle remain incomplete. |

## Next Sequential UOW

Next sequential production slice: add Java duplicate already-registered behavior for `CM_AUTO_GROUP` window `100` start-looking.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs` or `GamePacketTests` if packet shape evidence is needed

Expected Java behavior to model:

- `AutoGroupService.startLooking` checks existing queued parties under the mask.
- If `getSearchEntry(player.getObjectId(), lfps)` returns an existing `LookingForParty`, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED(agt.getTemplate().getInstanceMapId())` to the player and returns.
- C# already reports `AutoGroupStartLookingStatus.AlreadyRegistered`, but live dispatch currently does not send the Java system message.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Refine the filter to exact added/affected tests before running. Java/Maven is not expected unless a targeted Java fixture is added; source review should be sufficient for the duplicate-registration branch. Broad-validation trigger: live connection dispatch if packet sending is wired there; otherwise none for service-only planning.

## Safe Candidates

- Port `AutoGroupUtility.sendSuccessfulRegistration` packet fanout after queue registration.
- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Continue `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` guard parity.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
