# Phase 6EW Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EV and covers Session 641.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 101 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1115 tests.

## Recent Work Completed

### Session 641 - Alliance Change Group Missing-Member No-Ops

- Source-read Java `PlayerAllianceService.changeMemberGroup`, `ChangeMemberGroupEvent`, `PlayerAlliance.getAllianceGroup`, and the C# alliance group-change planner/runtime path.
- Added parsed-command regression coverage for `ALLIANCE_CHANGE_GROUP` command `27` when the first selected member is missing, matching Java `ChangeMemberGroupEvent.handleEvent` returning without packet fanout or group mutation.
- Added parsed-command regression coverage for command `27` when the second selected member in a swap is missing, matching Java returning before either member is moved.
- No production code changed in this unit; this is regression coverage for an already-modeled source-derived no-op path.
- Deferred invalid target-group id alignment because Java removes the first member from its old group before `PlayerAlliance.getAllianceGroup` throws `No such alliance group X`; that side-effectful exception path needs a more careful production design and possibly Java runtime confirmation.
- Commit: this handoff is included in `Cover alliance change group noops`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `27` has explicit missing-first-member and missing-second-member no-op coverage. Invalid target-group id side effects remain deferred. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_CHANGE_GROUP` | Command code `27` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Missing selected/swap members leave alliance group state unchanged and send no packets. Valid move behavior remains source-derived and not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.changeMemberGroup` | `Aion.GameServer.Services.PlayerAllianceGroupChangeServicePlanner.CreateChangeMemberGroupPlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Service-level not-alliance and not-authorized message branches were covered earlier; this unit covers event skip behavior after service authorization. Java static alliance registry and event queue behavior are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeMemberGroupEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime.ChangeMemberGroup` / `PlayerAllianceMemberGroupChangePlanner` | Event Runtime / Planner | Partial | Regression Tested | Needs Verification | C# returns null/no fanout when first or second member is absent, matching Java's early return comments. Invalid target-group exception path differs and is deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Model.GameObjects.PlayerAllianceDescriptor` / `PlayerAllianceRuntime` group storage | Team State | Partial | Regression Tested | Needs Verification | Group membership remains unchanged for missing first/second member paths. Java `getAllianceGroup` invalid id behavior is not yet ported exactly. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceGroup` | `Aion.GameServer.Model.GameObjects.PlayerAllianceMember.AllianceGroupId` / runtime group membership projections | Team State | Partial | Regression Tested | Needs Verification | C# tracks group id per alliance member rather than a separate group object. Missing-member no-op behavior is covered; Java group object removal/add ordering remains source-derived. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceMember` | `Aion.GameServer.Model.GameObjects.PlayerAllianceMember` | Team Member | Partial | Regression Tested | Needs Verification | Missing-member paths preserve existing member group ids. Serialization, Java object-wrapper identity, and group reference behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` | Server Packet | Partial | Regression Tested | Needs Verification | Tests assert no member-info packets are sent for missing first/second member paths. Valid packet bytes are not Java-golden compared. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertions | Runtime Dependency | Partial | Regression Tested | Needs Verification | Regressions confirm event-skip paths send no packets. Live socket behavior remains unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceChangeGroupMissingFirstMemberNoopsLikeJava`
  - Validates command `27` with selected member id `1999` leaves current group membership unchanged and sends no packets.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceChangeGroupMissingSecondMemberNoopsLikeJava`
  - Validates command `27` swap with second member id `1999` leaves current group membership unchanged and sends no packets.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 9
- Total artifacts ported or partially modeled in this handoff window: 0 new production artifacts; 1 deeper `CM_PLAYER_STATUS_INFO` / `ALLIANCE_CHANGE_GROUP` missing-member no-op path gained regression coverage.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: invalid target-group side-effectful exception path, full generic team-command dispatch, Java static alliance registry, Java event queue/lock comparison, encoded opcode/frame golden validation, packet capture comparison, and client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Alliance group-change no-op behavior is better covered, but production/live validation gaps remain.

## Remaining Risks

- Invalid target alliance-group id remains deferred because Java's throw occurs after removing the first member from its old group; C# currently pre-checks and throws a different message without that side effect.
- C# still uses manual branch dispatch and a dedicated group-change service rather than a full `PlayerTeamCommandService` equivalent.
- Java static alliance registry, event queue, lock, group object identity, group removal/add ordering, object iteration ordering, and threading behavior remain source-derived only.
- Java packet bytes, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.
- Reflection, precision/rounding, and date/time handling are not involved in this unit. Serialization parity is limited to handler reachability/state and no-send behavior; no Java golden bytes were compared.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Source-read and decide how to model the side-effectful invalid alliance-group id path for command `27`.
2. Or move laterally to a different Phase 6 core gap such as beginning a minimal league runtime bridge now that league command prerequisites are bounded.
3. Keep real league mutations out until a focused league model and event fanout plan are in place.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EV-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
