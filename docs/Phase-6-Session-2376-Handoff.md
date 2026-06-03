# Phase 6 Session 2376 Handoff - Autogroup Harmony Entry Guards

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2376-Completion.md`
- `docs/Phase-6-Session-2376-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Prefer production/game parity units. Use focused validation by default; full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger before execution. A passing filtered `dotnet test` command is compile evidence for the affected project/dependencies.

## Current State

Last completed UOW: UOW-2376, ported Harmony-specific autogroup group-entry guards.

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
- `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and initial `canRegisterGroupEntry` guard parity for template support, team/leader checks, periodic too-many-members, non-Harmony member denial checks, Harmony fixed-size, and Harmony missing-ticket member/requester denials.

Still not proven or not implemented:

- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Java periodic registration cron callbacks and real scheduled close task creation/cancellation remain missing.
- `AutoGroupConfig` schedule, period, start time, and announce settings remain partial or missing.
- Broader PvP arena availability and non-Harmony item checks remain partial.

## Commits Made

- `[Phase 6][UOW-2376] Port autogroup harmony entry guards`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupRegistrationGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2376-Completion.md`
- `docs/Phase-6-Session-2376-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 31, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Run after this handoff is created and before commit.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `AutoGroupUtility.checkGroupRequirements` Harmony branches. Java source review identified branch order, mask ids, ticket id, and message ids.

Broad-validation trigger: live connection dispatch changed for `CM_AUTO_GROUP` window `100`.

Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered service behavior, packet helper coverage, and a live ingress missing-ticket denial path.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.checkGroupRequirements(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupEntryGuard(...)` / `CreateGroupMemberRequirementGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Harmony/training Harmony fixed team-size branch and Harmony missing-ticket member branch are covered. Full autogroup lifecycle and queue matching remain missing. |
| `com.aionemu.gameserver.services.instance.PvPArenaService.checkItem(...)` | `Aion.GameServer.Services.PvPArenaAvailabilityPlanService` / `AutoGroupLookingPartyRegistrationService.HasRequiredHarmonyTicket(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Harmony ticket id `186000184` is reused and inventory-count behavior is tested for group-entry. FFA/solo/glory item checks and time availability remain separate partial behavior. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Metadata | Partial | Unit Tested | Partial Parity | Added Java mask helpers for Harmony and training Harmony. Other Java enum-specific helpers remain partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Existing `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` id `1400219`, `STR_MSG_CANT_INSTANCE_ENTER_MEMBER` id `1400187`, and too-many-members id `1400180` are exercised by focused tests. Full catalog remains partial. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` / `IGameClientConnectionRegistry` | Dispatch | Partial | Unit Tested | Partial Parity | Missing-ticket branch now sends member-facing and requester-facing denial packets through the live ingress path when the member is online. Offline/no-registry delivery remains partial. |

## Next Sequential UOW

Next sequential production slice: port the Java battleground registration announcement branch, then continue toward queue matching/live auto-instance creation.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupLookingPartyRegistrationService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupLookingPartyRegistrationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Expected Java behavior to model:

- `AutoGroupService.startLooking` has a battleground-specific announcement branch for some autogroup types after successful registration.
- Identify exact Java branch condition, message id, recipient scope, and ordering relative to successful-registration packets before implementing.
- Keep this as a focused announcement UOW if possible; defer queue matching and instance creation until the announcement branch is understood.

Focused validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Java/Maven is not expected unless a targeted Java fixture is added; Java source review should drive announcement behavior. Broad-validation trigger: live connection dispatch if packet sending changes there; otherwise none for service-only planning.

## Safe Candidates

- Add C# config option binding for remaining autogroup schedule/period/start/announce defaults.
- Continue queue matching and live auto-instance creation.
- Port Java quick-entry queue refill after autogroup leave.
- Port remaining PvP arena availability/common guard branches.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
