# Phase 6 Session 2375 Handoff - Autogroup Member Entry Denials

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2375-Completion.md`
- `docs/Phase-6-Session-2375-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2375, ported the non-Harmony member-loop denial checks for autogroup group-entry registration.

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
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, and non-Harmony member denial checks.

Still not proven or not implemented:

- Harmony arena item validation branch remains missing.
- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2375] Port autogroup member entry denials`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2375-Completion.md`
- `docs/Phase-6-Session-2375-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 22, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupUtility.checkGroupRequirements`; Java source review identified the member-denial branch and message id.

Broad-validation trigger: none. Runtime accessors are narrow snapshot reads, and no live dispatch, shared packet primitives, persistence, scheduler, or serialization primitives changed.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and covered service and packet helper behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.checkGroupRequirements(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupMemberRequirementGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Member cooldown, level-range, and already-searching denial branches are covered. Harmony item validation and id-only fallback member facts remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.isSearching(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.IsSearching(...)` | Service Lookup | Partial | Unit Tested | Partial Parity | Member searching check is now consumed by group-entry guard and tested through an existing queued member. Full Java queue matching/lifecycle remains missing. |
| `com.aionemu.gameserver.model.team.GeneralTeam.getMembers()` | `Aion.GameServer.Services.PlayerGroupRuntime.GetMemberPlayers(...)` / `Aion.GameServer.Services.PlayerAllianceRuntime.GetMemberPlayers(...)` | Runtime Snapshot | Partial | Unit Tested | Partial Parity | C# runtimes now expose current `Player` objects for member requirement checks. Broader Java team collection semantics and offline replacement behavior remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.PortalCooldownList.isPortalUseDisabled(...)` | `Aion.GameServer.Services.PlayerPortalCooldownService.IsPortalUseDisabled(...)` | Cooldown Lookup | Partial | Unit Tested | Partial Parity | Existing cooldown logic is now consumed by autogroup member checks. Missing `InstanceCooltimeTable` data skips this C# branch and remains a documented partial behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_MSG_CANT_INSTANCE_ENTER_MEMBER` id `1400187` with packet helper test. Full Java system-message catalog remains partial. |

## Next Sequential UOW

Next sequential production slice: port the Harmony arena item-validation branch from `AutoGroupUtility.checkGroupRequirements`, or if inventory/PvP dependencies are too large after discovery, port the Java battleground registration announcement branch.

Java artifacts to inspect for Harmony branch:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PvPArenaService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Expected Java behavior to model:

- For Harmony arena group-entry, Java checks `PvPArenaService.checkItem(member, agt)` for non-leader members.
- If a member lacks the required item, Java sends `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM()` to that member and `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` to the requester, then returns false.
- This may require modeling an item-availability fact or a narrow PvP arena item planner rather than full inventory runtime.

Focused validation recipe if Harmony item branch is scoped:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added; Java source review should drive the item-denial behavior. Broad-validation trigger: none for service-only planning unless live connection dispatch, inventory runtime, or shared packet primitives are changed.

## Safe Candidates

- Port Java battleground registration announcement branch.
- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Continue queue matching and live auto-instance creation.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
