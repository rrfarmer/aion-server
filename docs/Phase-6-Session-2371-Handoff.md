# Phase 6 Session 2371 Handoff - Autogroup Duplicate Registration Message

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2371-Completion.md`
- `docs/Phase-6-Session-2371-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2371, ported the duplicate start-looking system message for `CM_AUTO_GROUP` window `100`.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- Java `AutoGroupUtility.sendSuccessfulRegistration(...)` fanout after a successful registration remains missing.
- C# queue matching and live auto-instance creation remain missing, so runtime registrations are not produced by the start-looking path yet.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` remain partial/missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2371] Send autogroup duplicate registration message`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2371-Completion.md`
- `docs/Phase-6-Session-2371-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupService.startLooking`; Java source review identified the exact duplicate branch and message packet behavior.

Broad-validation trigger: live connection dispatch and packet helper were touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed. No shared packet primitive, scheduler primitive, persistence repository, serialization helper, data loader, or common model/state changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` plus `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Service/Packet Dispatch | Partial | Unit Tested | Partial Parity | Duplicate queued registration now sends Java `STR_MSG_CANT_INSTANCE_ALREADY_REGISTERED` from live dispatch. Successful-registration fanout, queue matching, announcements, and full lifecycle remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added `CantInstanceAlreadyRegistered(int)` id `1400181` with invariant world-id parameter. Not a Java-generated golden packet. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player, AionServerPacket)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendPacketAsync(...)` | Packet Utility/Dispatch | Partial | Unit Tested | Partial Parity | Duplicate autogroup branch sends directly to the active connection. Broader utility parity remains partial. |

## Next Sequential UOW

Next sequential production slice: port `AutoGroupUtility.sendSuccessfulRegistration(...)` packet fanout after successful start-looking registration.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Expected Java behavior to model:

- `AutoGroupUtility.sendSuccessfulRegistration(lfp, leaderName, agt, maskId)` iterates queued member object ids and only sends to online players.
- If `agt.isPeriodicInstance()`, it first sends `new SM_AUTO_GROUP(maskId, 6, true)`.
- Then it sends `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_REGISTER_SUCCESS()`.
- Then it sends `new SM_AUTO_GROUP(maskId, 1, lfp.getEntryRequestType().getId(), leaderName)`.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmAutoGroup|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Refine the filter to exact added/affected tests before running. Java/Maven is not expected unless a targeted Java fixture is added; source review should be sufficient for this packet fanout branch. Broad-validation trigger: live connection dispatch if packet sending is wired there; otherwise none for service-only planning.

## Safe Candidates

- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Continue `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` guard parity.
- Model Java battleground registration announcement branch after the successful-registration fanout exists.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
