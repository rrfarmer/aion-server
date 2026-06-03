# Phase 6 Session 2374 Handoff - Autogroup Entry Registration Guards

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2374-Completion.md`
- `docs/Phase-6-Session-2374-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2374, ported entry-mode guard parity for autogroup start-looking registration.

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
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, and periodic too-many-members.

Still not proven or not implemented:

- C# queue matching and live auto-instance creation remain missing.
- Member-loop group-entry checks remain missing: member cooldown, level range, already searching, Harmony arena item validation.
- Harmony/training Harmony fixed group-size branch remains missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.

## Commits Made

- `[Phase 6][UOW-2374] Port autogroup entry registration guards`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2374-Completion.md`
- `docs/Phase-6-Session-2374-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 25, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for the `AutoGroupUtility` entry guard branches. Java source review identified the no-op and system-message behavior.

Broad-validation trigger: live connection dispatch input changed for `CM_AUTO_GROUP` window `100`, but packet primitives/shared serialization were not changed.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled the affected project/dependencies and covered service, packet helper, and live ingress denial behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterNewEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateSoloEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support and team rejection with `STR_MSG_CANT_INSTANCE_NOT_LEADER` are covered. Broader dependencies remain through `AutoGroupService.canRegister` common guard planning. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterQuickEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateSoloEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support and team rejection with Java message id `1400182` are covered, including a live packet-ingress test. Quick-entry refill/lifecycle behavior remains missing. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterGroupEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support, team leader requirement, and periodic too-many-members branch are covered. Member cooldown/level/searching and Harmony arena member checks remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS` id `1400180` and `STR_MSG_CANT_INSTANCE_NOT_LEADER` id `1400182` with packet helper tests. Full Java system-message catalog remains partial. |
| `com.aionemu.gameserver.dataholders.InstanceCooltimeData.getMaxMemberCount(...)` | `Aion.GameServer.Dataholders.InstanceCooltimeTable.GetMaxMemberCount(...)` | Data Lookup | Partial | Unit Tested | Partial Parity | Existing Java race-specific max-member lookup is now consumed by autogroup periodic group-entry guard. Missing cooltime data yields no size denial in this slice. |

## Next Sequential UOW

Next sequential production slice: port the remaining member-loop checks from `AutoGroupUtility.checkGroupRequirements` for group-entry registration.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/PvPArenaService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Expected Java behavior to model:

- For non-leader members in the current team, `checkGroupRequirements` skips the leader.
- If a member has cooldown, is outside the autogroup level range, or is already searching that mask, Java sends `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` to the requester and returns false.
- Harmony arena item checks also send `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)`, but may need a smaller separate UOW if inventory/item runtime dependencies are large.
- The requester's queue should remain unchanged when any member check fails.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added; Java source review should drive the member-denial behavior. Broad-validation trigger: none for service-only planning unless live connection dispatch or shared packet primitives are changed.

## Safe Candidates

- Port Java battleground registration announcement branch.
- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Continue queue matching and live auto-instance creation.
- Port Java quick-entry queue refill after autogroup leave.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
