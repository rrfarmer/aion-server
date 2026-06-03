# Phase 6 Session 2373 Handoff - Autogroup Multi-Member Fanout Evidence

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2373-Completion.md`
- `docs/Phase-6-Session-2373-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2373, added registry-backed multi-member fanout evidence for successful autogroup registration.

Recent production parity slices:

- `CM_AUTO_GROUP` window `100` start-looking queue registration.
- `CM_AUTO_GROUP` window `100` duplicate already-registered system message.
- `CM_AUTO_GROUP` window `100` successful registration fanout.
- Multi-member online filtering evidence for successful registration fanout.
- `CM_AUTO_GROUP` window `101` cancel queued registration.
- `CM_AUTO_GROUP` window `102` press-enter runtime handling.
- `CM_AUTO_GROUP` window `103` cancel-enter runtime unregister.
- `CM_AUTO_GROUP` window `104` periodic request-icon click handling.
- `CM_AUTO_GROUP` top-level disabled config guard using `gameserver.autogroup.enable`.
- `CM_AUTO_GROUP` window `105` explicit no-op.

Still not proven or not implemented:

- C# queue matching and live auto-instance creation remain missing.
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` remain partial/missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2373] Cover autogroup multi-member fanout`

## Files Changed In Last UOW

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2373-Completion.md`
- `docs/Phase-6-Session-2373-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests" --no-restore
```

Result: passed 18, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene should be rerun after this handoff is committed if more docs are edited:

```powershell
git diff --check
```

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupUtility.sendSuccessfulRegistration`; Java source review identified online-player filtering.

Broad-validation trigger: none; last UOW was test-only.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and passed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.sendSuccessfulRegistration(...)` | `Aion.GameServer.Network.Aion.GameServerConnection.SendAutoGroupSuccessfulRegistrationAsync(...)` | Utility/Dispatch | Partial | Unit Tested | Partial Parity | Multi-member online filtering and packet order are now covered through the registry path. Queue matching, instance creation, and broader autogroup lifecycle remain incomplete. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistration` | Model | Partial | Unit Tested | Partial Parity | Member object-id list drives fanout order in the C# test. Full Java `LookingForParty` timing and comparison behavior remain partial/missing. |
| `com.aionemu.gameserver.world.World.getPlayer(int)` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync(...)` | Runtime Lookup/Dispatch | Partial | Unit Tested | Partial Parity | Registry online set models Java online-player filtering for this fanout branch. Broader world lookup parity remains partial. |

## Next Sequential UOW

Next sequential production slice: port `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` guard parity for start-looking.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Expected Java behavior to model:

- New-entry and quick-entry requests require template support and reject players currently in a team with `STR_MSG_CANT_INSTANCE_NOT_LEADER` id `1400182`.
- Group-entry requests require template support and a current team whose leader is the requesting player.
- Periodic group-entry requests reject teams larger than `INSTANCE_COOLTIME_DATA.getMaxMemberCount(instanceMapId, race)` with `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(maxMemberPerTeam, mapId)` id `1400180`.
- Member cooldown/level/searching checks reject through `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` and should be scoped carefully in a later or expanded UOW.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added; Java source review should drive the guard-message behavior. Broad-validation trigger: live connection dispatch if packet sending changes there; otherwise none for service-only planning.

## Safe Candidates

- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Port Java battleground registration announcement branch.
- Continue queue matching and live auto-instance creation.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
