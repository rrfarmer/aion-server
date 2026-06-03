# Phase 6 Session 2374 Completion - Port Autogroup Entry Registration Guards

## Scope

Ported the first Java entry-mode guard slice from `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` into C# start-looking registration.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroup.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- Common `AutoGroupService.canRegister` guards run before entry-mode-specific guards.
- New-entry and quick-entry return false silently when the template flag is disabled.
- New-entry and quick-entry reject any player currently in a team with `STR_MSG_CANT_INSTANCE_NOT_LEADER` id `1400182`.
- Group-entry returns false silently when the template does not support group registration.
- Group-entry requires a current team whose leader is the requesting player and sends `STR_MSG_CANT_INSTANCE_NOT_LEADER` id `1400182` otherwise.
- Periodic group-entry rejects oversized teams using `INSTANCE_COOLTIME_DATA.getMaxMemberCount(instanceMapId, race)` and `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(maxMemberPerTeam, mapId)` id `1400180`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Extended `AutoGroupLookingPartyRegistrationService.StartLooking` with entry-mode guard planning after the existing common guard and before queue mutation.
- Added C# guard statuses for unsupported entry, not-leader, and too-many-members outcomes.
- Passed static `InstanceCooltimeTable` into live `CM_AUTO_GROUP` window `100` handling so periodic group-entry can use Java max-member data.
- Added `SmSystemMessage.CantInstanceTooManyMembers(...)` and `SmSystemMessage.CantInstanceNotLeader()`.
- Updated tests to stop treating solo `GroupEntry` as a successful registration path.
- Added focused service, packet, and live ingress tests for Java entry guard behavior.

Known limitations:

- Member-loop checks in `AutoGroupUtility.checkGroupRequirements` remain incomplete: member cooldown, level range, already searching, and Harmony arena item checks are not yet ported.
- Harmony/training Harmony fixed max size of 3 is not covered in this UOW.
- C# group/alliance membership is modeled through runtime snapshots; broader Java `TemporaryPlayerTeam` semantics remain partial.

## Validation Decision

- Changed surface: production service guard logic, system-message packet helpers, and live `CM_AUTO_GROUP` dispatch inputs.
- Specific behavior/contract: Java entry-mode registration guards block unsupported modes, team/leader violations, and periodic oversized teams before queue mutation while emitting Java message ids `1400180` and `1400182` where applicable.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 25, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for these `AutoGroupUtility` guard branches; Java source review drove the expected denial/no-op behavior.
- Broad-validation trigger: live connection dispatch input changed for `CM_AUTO_GROUP` window `100`, but packet primitives/shared serialization were not changed.
- Broad .NET decision: skipped full project/solution validation because the focused command compiled the affected project/dependencies and covered the edited service, packet message helpers, and a live ingress denial path.
- Why this scope is sufficient: the edited behavior is local to start-looking registration guards and denial packets; focused tests assert queue non-mutation, Java message ids/parameters, and live dispatch packet emission.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterNewEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateSoloEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support and team rejection with `STR_MSG_CANT_INSTANCE_NOT_LEADER` are covered. Broader dependencies remain through `AutoGroupService.canRegister` common guard planning. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterQuickEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateSoloEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support and team rejection with Java message id `1400182` are covered, including a live packet-ingress test. Quick-entry refill/lifecycle behavior remains missing. |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.canRegisterGroupEntry(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupEntryGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Template support, team leader requirement, and periodic too-many-members branch are covered. Member cooldown/level/searching and Harmony arena member checks remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS` id `1400180` and `STR_MSG_CANT_INSTANCE_NOT_LEADER` id `1400182` with packet helper tests. Full Java system-message catalog remains partial. |
| `com.aionemu.gameserver.dataholders.InstanceCooltimeData.getMaxMemberCount(...)` | `Aion.GameServer.Dataholders.InstanceCooltimeTable.GetMaxMemberCount(...)` | Data Lookup | Partial | Unit Tested | Partial Parity | Existing Java race-specific max-member lookup is now consumed by autogroup periodic group-entry guard. Missing cooltime data yields no size denial in this slice. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_NewOrQuickEntryRejectsTeamPlayerLikeJavaNotLeader` | Unit | Java source review | Quick-entry rejects a team player before queue mutation and plans message id `1400182`. | Focused C# service test from `AutoGroupUtility.canRegisterQuickEntry`. | New-entry path shares the same helper but is not separately asserted. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_UnsupportedEntryFlagIsSilentNoOpLikeJavaTemplateFlag` | Unit | Java source review | Unsupported template entry flag blocks registration without a denial message. | Focused C# service test from Java template flag branches. | Covers quick-entry flag; group/new flags share analogous behavior. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_GroupEntryRejectsSoloOrNonLeaderLikeJavaNotLeader` | Unit | Java source review | Group-entry rejects no-team and non-leader requests with message id `1400182`. | Focused C# service test from `checkGroupRequirements`. | Alliance leader semantics remain lightly modeled. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_PeriodicGroupEntryRejectsTooManyMembersLikeJavaMaxMemberCount` | Unit | Java source review | Periodic group-entry uses Java max-member lookup and message id `1400180` parameters. | Focused C# service test using `InstanceCooltimeTable`. | Harmony arena fixed-size branch remains unported. |
| `GameServerConnectionAutoGroupTests.ProcessPacketAsync_AutoGroupQuickEntryTeamPlayerSendsJavaNotLeaderMessage` | Unit | Java source review | Live `CM_AUTO_GROUP` window `100` quick-entry sends Java not-leader packet. | Focused live ingress test. | Does not cover all denial branches through live dispatch. |
| `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Unit | Java source review | New message helpers serialize ids/parameters for `1400180` and `1400182`. | Focused packet helper assertions. | Full catalog remains partial. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup registration guard parity advanced, but lifecycle parity remains partial.

## Remaining Gaps

- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Member-loop checks for cooldown, level range, already searching, and PvP arena item validation remain missing.
- Harmony/training Harmony group-size branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2374] Port autogroup entry registration guards
```
