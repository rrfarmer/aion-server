# Phase 6 Session 2377 Handoff - Autogroup Battleground Registration Announcements

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2377-Completion.md`
- `docs/Phase-6-Session-2377-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2377, ported the autogroup battleground registration announcement branch.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `100` battleground registration announcement after successful periodic group registration.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- C# queue matching and live auto-instance creation remain missing.
- Java `checkInstancesForOpenQuickEntries(lfp, maskId)` remains missing.
- Java `checkQueueForNewMatches(maskId)` remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, and start time settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2377] Port autogroup battleground announcements`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2377-Completion.md`
- `docs/Phase-6-Session-2377-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 34, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupService.startLooking` announcement branch. Java source review identified branch condition, message construction, chat type, recipient predicate, and branch order.

Broad-validation trigger: live connection dispatch changed for `CM_AUTO_GROUP` window `100`.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered service behavior plus live ingress dispatch.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Service/Dispatch | Partial | Unit Tested | Partial Parity | Battleground registration announcement branch is covered. Queue matching and auto-instance creation remain missing. |
| `com.aionemu.gameserver.configs.main.AutoGroupConfig` | `Aion.GameServer.Configuration.GameServerAutoGroupOptions` | Config | Partial | Unit Tested | Partial Parity | Added `AnnounceBattlegroundRegistrations` with Java default `false`. Schedule/period/start-time settings remain partial. |
| `com.aionemu.gameserver.model.Race` | `AutoGroupLookingPartyRegistrationService.GetRaceL10n(...)` | Metadata Helper | Partial | Unit Tested | Partial Parity | ELYOS and ASMODIANS client-string ids are used for announcement text. Full Race enum is not ported here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Packet | Partial | Unit Tested | Partial Parity | Existing manual constructor supports sender id `0`, null sender name, and chat type `36`; focused live test serializes and reads it. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld(...)` | `IGameClientConnectionRegistry.BroadcastToWorldAsync(...)` | Dispatch | Partial | Unit Tested | Partial Parity | Live test validates opposite-race and level-range filtering. Registry-less delivery remains partial. |

## Next Sequential UOW

Next sequential production slice: port queue follow-up behavior after successful registration.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/instance/InstanceEngine.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`

Expected Java behavior to model:

- After successful registration and optional announcement, Java calls `checkInstancesForOpenQuickEntries(lfp, maskId)`.
- If no open quick-entry instance accepts the party, Java calls `checkQueueForNewMatches(maskId)`.
- Identify the smallest next production slice first. A good candidate is a planning-only representation of the branch decision and queue grouping prerequisites before live instance creation.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added. Broad-validation trigger: live connection dispatch or shared instance runtime changes; otherwise keep validation focused.

## Safe Candidates

- Add queue matching planning helpers without creating live instances yet.
- Add open quick-entry planning around existing auto-instance runtime facts, if a narrow seam exists.
- Add C# config option binding for remaining autogroup schedule/period/start defaults.
- Continue toward live auto-instance creation once queue matching is understood.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
