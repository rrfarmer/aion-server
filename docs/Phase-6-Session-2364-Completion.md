# Phase 6 Session 2364 Completion - Wire Autogroup Start-Looking Queue Registration

## Scope

Wired the narrow `CM_AUTO_GROUP` window `100` path into C# autogroup looking-party queue registration.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/EntryRequestType.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/LookingForParty.java`
- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`

Java behavior used:

- `CM_AUTO_GROUP.runImpl` only starts looking on window `100`.
- Entry request ids map as `0 = NEW_GROUP_ENTRY`, `1 = QUICK_GROUP_ENTRY`, `2 = GROUP_ENTRY`; invalid ids return without side effects.
- `AutoGroupService.startLooking` resolves the autogroup mask, runs common registration guards, rejects duplicate searching players for the same mask, then adds a `LookingForParty`.
- `LookingForParty.createMembers` includes current team online members; C# now uses existing group/alliance runtime member ids where available.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added C# `AutoGroupEntryRequestType` and Java id parser.
- Added `AutoGroupLookingPartyRegistrationService.StartLooking(...)`.
- Added duplicate-search detection and `IsSearching(...)`.
- Added group/alliance member id capture for queue registrations.
- Wired `GameServerConnection` `CmAutoGroup` window `100` to the looking-party queue service.
- Passed the singleton looking-party service through `GameClientSocketServer` into new game connections.
- Added focused tests for entry request ids, valid queue registration, group member capture, duplicate no-op, guard failure, and missing autogroup no-op.

Known limitations:

- C# autogroup enable/disable config binding is not present, so Java `AUTO_GROUP_ENABLE` message behavior is not live.
- PvP arena availability and cooldown checks are not wired into `StartLooking`; the current guard uses the already modeled level guard and documents the remaining gap.
- Java `sendSuccessfulRegistration` packet fanout is not sent yet.
- Queue matching, instance creation, penalties, quick-entry refill, and windows `101` through `105` remain missing.

## Validation Decision

- Changed surface: live connection dispatch plus production runtime state.
- Specific behavior/contract: `CM_AUTO_GROUP` window `100` should parse Java entry request ids and add allowed player/team registrations to the looking-party queue after common guard checks; invalid/missing/blocked cases must not mutate queue state.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupRegistrationGuardPlanServiceTests" --no-restore
```

Result: passed 16, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git emitted CRLF working-copy warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `CM_AUTO_GROUP.runImpl` or `AutoGroupService.startLooking`; Java source review identified the source-of-truth ids and guard/queue behavior.
- Broad-validation trigger: live connection dispatch was touched.
- Broad .NET decision: skipped full project/solution validation because the focused filtered command compiled `GameServerConnection`, `GameClientSocketServer`, the queue service, and directly related tests; no shared packet primitive, scheduler, persistence, or serialization helper changed.
- Why this scope is sufficient: the new queue mutation contract and guard/id behavior are directly asserted, while the live connection wiring is compiled and the remaining untested live send/matching branches are explicitly not implemented.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.autogroup.EntryRequestType` | `Aion.GameServer.Services.AutoGroupEntryRequestType` | Enum | Complete | Unit Tested | Partial Parity | Ids `0`, `1`, `2` and invalid-id null behavior are covered. Java enum names are represented by C# names; no serialization beyond parser is used. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleAutoGroupAsync(...)` | Packet Dispatch | Partial | Unit Tested indirectly | Partial Parity | Window `100` now dispatches to queue registration. Windows `101`-`105`, config-disabled message, and request icon handling remain missing. |
| `com.aionemu.gameserver.services.AutoGroupService.startLooking(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.StartLooking(...)` | Service | Partial | Unit Tested | Partial Parity | Mask lookup, entry request ids, level guard, duplicate same-mask no-op, and queue mutation are modeled. PvP arena availability, cooldowns, canRegisterNew/Quick/Group, success packets, matching, and announcements remain missing. |
| `com.aionemu.gameserver.model.autogroup.LookingForParty.createMembers(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService` member id resolution | Runtime State | Partial | Unit Tested | Partial Parity | Group/alliance member ids are captured from C# runtime snapshots where available. Java filters online team members; C# currently trusts runtime member ids. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.EntryRequestTypeParser_MatchesJavaEntryRequestTypeIds` | Unit | Java source review | Entry request ids `0`, `1`, `2`, and invalid id null behavior. | Focused parser test. | None for ids. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_RegistersSoloPlayerForValidJavaWindowHundredRequest` | Unit | Java source review | Valid solo registration mutates queue. | Focused state test. | No success packets/matching. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_RegistersCurrentGroupMembersLikeJavaCreateMembers` | Unit | Java source review | Team member ids are included for group registrations. | Focused state test. | C# does not filter online members yet. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_DuplicateMemberRegistrationIsNoOpLikeJavaAlreadyRegisteredBranch` | Unit | Java source review | Same player duplicate registration for same mask is no-op. | Focused state test. | Does not send Java already-registered system message yet. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_LevelGuardBlocksBeforeQueueMutationLikeJavaCanRegister` | Unit | Java source review | Level guard blocks queue mutation and exposes denial message. | Focused guard/state test. | Other Java guards remain missing. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_MissingAutoGroupIsNoOpLikeJavaNullTypeBranch` | Unit | Java source review | Missing autogroup mask returns no-op and does not mutate queue. | Focused state test. | None for this branch. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- `CM_AUTO_GROUP` windows `101` through `105` remain deferred.
- `AutoGroupConfig.AUTO_GROUP_ENABLE` config behavior remains missing in C#.
- Java `AutoGroupUtility.canRegisterNewEntry`, `canRegisterQuickEntry`, and `canRegisterGroupEntry` are not ported.
- Java success registration packet fanout, queue matching, instance creation, penalties, and quick-entry refill remain missing.
- Java periodic registration cron callbacks and real scheduled close task handles remain missing.

## Commit

Commit message:

```text
[Phase 6][UOW-2364] Wire autogroup start queue registration
```
