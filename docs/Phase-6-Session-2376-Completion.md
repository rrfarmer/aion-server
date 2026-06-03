# Phase 6 Session 2376 Completion - Port Autogroup Harmony Entry Guards

## Scope

Ported the Harmony-specific group-entry checks from `AutoGroupUtility.checkGroupRequirements`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/instance/PvPArenaService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- Harmony and training Harmony group-entry reject teams larger than 3 with `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(3, mapId)`.
- For Harmony arena member checks, Java skips the leader, calls `PvPArenaService.checkItem(member, agt)`, and requires item `186000184`.
- If a non-leader member lacks the Harmony ticket, Java sends `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM()` to that member, sends `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` to the requester, and returns false before queue mutation.
- Training Harmony does not run the `agt.isHarmonyArena()` item check.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupSummary.IsHarmonyArena` and `IsTrainingHarmonyArena` Java mask-id helpers.
- Added `AutoGroupRegistrationGuardPlanStatus.BlockedHarmonyMemberMissingItem`.
- Added recipient-specific `AutoGroupMemberDenialIntent` on guard plans.
- Extended group-entry guard logic with Harmony fixed-size and missing-ticket denial branches.
- Reused `PvPArenaAvailabilityPlanService.HarmonyArenaTicketItemId` for Java ticket id `186000184`.
- Updated live `CM_AUTO_GROUP` window `100` handling to send member-denial intents through the connection registry before requester denial messages.
- Added focused service tests for Harmony fixed-size and missing-ticket branches.
- Added a live ingress test for Java's two-recipient missing-ticket packet behavior.

Known limitations:

- This UOW models the Harmony item branch through current C# inventory facts only; full Java inventory/storage semantics remain partial.
- If the missing-ticket member is offline or no connection registry is available, the C# live path cannot send the member-facing packet. The requester denial still occurs.
- Broader PvP arena time availability and non-Harmony solo/FFA/glory item checks remain outside this group-entry UOW.

## Validation Decision

- Changed surface: production autogroup service guard logic, autogroup static metadata helpers, guard-plan packet intents, and live `CM_AUTO_GROUP` dispatch of member-denial packets.
- Specific behavior/contract: Harmony group-entry should reject teams over 3; a Harmony member missing ticket `186000184` should receive `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` id `1400219`, the requester should receive `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` id `1400187`, and no queue mutation should occur.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 31, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for `AutoGroupUtility.checkGroupRequirements` Harmony branches; Java source review identified mask ids, item id, message ids, and branch order.
- Broad-validation trigger: live connection dispatch changed for `CM_AUTO_GROUP` window `100`.
- Broad .NET decision: skipped full project/solution validation after the focused filtered command compiled affected projects and covered service behavior, packet helper coverage, and a live ingress missing-ticket denial path.
- Why this scope is sufficient: the edited behavior is local to autogroup start-looking guard planning and the member/requester denial packets; focused tests assert Java ids, parameters, recipient split, and queue non-mutation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.checkGroupRequirements(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupEntryGuard(...)` / `CreateGroupMemberRequirementGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Harmony/training Harmony fixed team-size branch and Harmony missing-ticket member branch are covered. Full autogroup lifecycle and queue matching remain missing. |
| `com.aionemu.gameserver.services.instance.PvPArenaService.checkItem(...)` | `Aion.GameServer.Services.PvPArenaAvailabilityPlanService` / `AutoGroupLookingPartyRegistrationService.HasRequiredHarmonyTicket(...)` | Service/Planner | Partial | Unit Tested | Partial Parity | Harmony ticket id `186000184` is reused and inventory-count behavior is tested for group-entry. FFA/solo/glory item checks and time availability remain separate partial behavior. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Metadata | Partial | Unit Tested | Partial Parity | Added Java mask helpers for Harmony and training Harmony. Other Java enum-specific helpers remain partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Existing `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` id `1400219`, `STR_MSG_CANT_INSTANCE_ENTER_MEMBER` id `1400187`, and too-many-members id `1400180` are exercised by focused tests. Full catalog remains partial. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` / `IGameClientConnectionRegistry` | Dispatch | Partial | Unit Tested | Partial Parity | Missing-ticket branch now sends member-facing and requester-facing denial packets through the live ingress path when the member is online. Offline/no-registry delivery remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_HarmonyGroupEntryRejectsTooManyMembersLikeJavaFixedSize` | Unit | Java source review | Harmony group-entry rejects teams larger than 3 with id `1400180` and no queue mutation. | Focused C# service test. | Training Harmony shares the same helper but is not separately asserted. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_HarmonyGroupEntryRejectsMemberMissingTicketLikeJava` | Unit | Java source review | Missing Harmony ticket blocks requester, plans member id `1400219`, requester id `1400187`, and no queue mutation. | Focused C# service test using inventory facts. | Does not compare against Java runtime inventory. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupHarmonyMissingTicketSendsJavaMemberAndRequesterMessages` | Unit | Java source review | Live `CM_AUTO_GROUP` window `100` sends member and requester denial packets to separate recipients. | Focused live ingress test with registry-backed online member. | Offline member delivery behavior remains partial. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup group-entry guard parity advanced, but lifecycle parity remains partial.

## Remaining Gaps

- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.
- Broader PvP arena time availability and solo/FFA/glory item checks remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2376] Port autogroup harmony entry guards
```
