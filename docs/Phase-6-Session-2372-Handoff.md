# Phase 6 Session 2372 Handoff - Autogroup Successful Registration Fanout

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2372-Completion.md`
- `docs/Phase-6-Session-2372-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2372, ported the successful registration fanout for `CM_AUTO_GROUP` window `100`.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- Multi-member success fanout uses the registry path but lacks a dedicated test.
- C# queue matching and live auto-instance creation remain missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` remain partial/missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2372] Send autogroup successful registration fanout`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2372-Completion.md`
- `docs/Phase-6-Session-2372-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmAutoGroup|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 23, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupUtility.sendSuccessfulRegistration`; Java source review identified the packet order and message ids.

Broad-validation trigger: live connection dispatch and packet helpers were touched.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed. No shared packet primitive, scheduler primitive, persistence repository, serialization helper, data loader, or common model/state changed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendSuccessfulRegistration(...)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendAutoGroupSuccessfulRegistrationAsync(...)` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Packet order is modeled for successful registration. Multi-member registry fanout is implemented but not separately tested; online/offline behavior remains partial. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType.isPeriodicInstance()` | `Aion.GameServer.Dataholders.AutoGroupSummary.IsPeriodicInstance` | Model/Metadata | Partial | Unit Tested indirectly | Partial Parity | Java periodic mask set `{1,2,3,107,108,109,111}` is modeled for fanout. Other `AutoGroupType` methods remain partial/missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Packet | Partial | Unit Tested | Partial Parity | Existing window `6` support is reused; window `1` waiting payload with request type and leader name is now covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet | Partial | Unit Tested | Partial Parity | Added `InstanceRegisterSuccess()` id `1400194`. Not a Java-generated golden packet. |

## Next Sequential UOW

Next sequential safe slice: add a dedicated multi-member/online filtering test for successful registration fanout, or continue into `AutoGroupUtility.canRegister*` guard parity.

Java artifacts to inspect for multi-member fanout:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/world/World.java`

C# artifacts likely involved:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`

Expected Java behavior to model:

- `AutoGroupUtility.sendSuccessfulRegistration` iterates every queued member id.
- It only sends to players returned by `World.getInstance().getPlayer(objectId)`.
- Packet order per online player is unchanged from UOW-2372.

Focused validation recipe for multi-member fanout:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added; source review should be sufficient for the online filtering branch. Broad-validation trigger: none if this is test-only; live connection dispatch trigger applies if production code changes.

Safe candidate after that: port `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` guard parity.

## Safe Candidates

- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Port Java quick-entry queue refill after autogroup leave.
- Model Java battleground registration announcement branch.
- Continue queue matching and live auto-instance creation.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
