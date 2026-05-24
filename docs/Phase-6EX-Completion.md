# Phase 6EX Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EW and covers Session 642.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 102 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1116 tests.

## Recent Work Completed

### Session 642 - Alliance Change Group Invalid Target Group

- Source-read Java `ChangeMemberGroupEvent.moveMemberToGroup`, `PlayerAlliance.getAllianceGroup`, `PlayerAllianceGroup.onRemoveMember`, and C# `PlayerAllianceRuntime.ChangeMemberGroup`.
- Aligned `ALLIANCE_CHANGE_GROUP` command `27` invalid target-group behavior with Java's source ordering: remove the first member from the old subgroup before the missing target group throws.
- Added `PlayerAllianceMember.ClearAllianceGroupReference()` so C# can model Java's null `PlayerAllianceGroup` reference without removing the player from the alliance.
- Changed the C# invalid group exception message to `No such alliance group X`. Intentional difference: Java throws `NullPointerException` through `Objects.requireNonNull`; C# throws `InvalidOperationException`.
- Added parsed-command regression coverage for invalid group id `1999`, confirming no packets are sent, alliance membership remains, old subgroup membership is removed, and the moved member remains attached to alliance `88001` with subgroup id `0`.
- Updated runtime-level group-change coverage for the same side-effectful exception branch.
- Commit: this handoff should be included with the unit commit.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Command id `27` now covers invalid target-group id after service authorization. Encoded packet/golden/client behavior remains unverified. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_CHANGE_GROUP` | Command code `27` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target subgroup removes the moved member from its old subgroup before throwing. Valid move and swap behavior remain source-derived. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.changeMemberGroup` | `Aion.GameServer.Services.PlayerAllianceGroupChangeServicePlanner.CreateChangeMemberGroupPlan` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Authorized event path now includes invalid target-group source ordering. Java static alliance registry and event queue behavior are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeMemberGroupEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime.ChangeMemberGroup` / `PlayerAllianceMemberGroupChangePlanner` | Event Runtime / Planner | Partial | Regression Tested | Needs Verification | C# clears the first member's subgroup before throwing `No such alliance group X`. Java event lock/threading behavior remains unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.getAllianceGroup` | `PlayerAllianceDescriptor.AllianceGroupIds` validation in `PlayerAllianceRuntime.ChangeMemberGroup` | Team State Helper | Partial | Regression Tested | Intentional Difference | Java throws `NullPointerException`; C# throws `InvalidOperationException` with the same message after preserving Java mutation ordering. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceGroup` | `Aion.GameServer.Model.GameObjects.PlayerAllianceMember.AllianceGroupId` / `ClearAllianceGroupReference` | Team State | Partial | Regression Tested | Needs Verification | C# uses subgroup id `0` to approximate Java's null group reference because no separate C# group object exists. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceMember` | `Aion.GameServer.Model.GameObjects.PlayerAllianceMember` | Team Member | Partial | Regression Tested | Needs Verification | `ClearAllianceGroupReference` preserves alliance id while clearing subgroup state. Java wrapper identity and serialization remain unverified. |
| `com.aionemu.gameserver.model.team.GeneralTeam.removeMember` | `PlayerAllianceMember.ClearAllianceGroupReference` plus runtime projection refresh | Team Utility | Partial | Regression Tested | Needs Verification | Java dependency discovered through `PlayerAllianceGroup.removeMember`; generic Java collection mechanics are not fully ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` | Server Packet | Partial | Regression Tested | Needs Verification | Tests assert no member-info packets are sent when invalid group lookup throws. Valid packet bytes are not Java-golden compared. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertions | Runtime Dependency | Partial | Regression Tested | Needs Verification | Side-effectful exception path sends no packets in regression tests. Live socket behavior remains unverified. |

## Tests Added Or Updated

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceChangeGroupInvalidTargetGroupRemovesOldGroupBeforeThrowLikeJava`
  - Validates command `27` with target group `1999` throws `InvalidOperationException("No such alliance group 1999")`, preserves alliance membership, removes the moved member from group `1000`, keeps the member in alliance `88001` with subgroup id `0`, and sends no packets.
- `PlayerAllianceRuntimeTests.ChangeMemberGroup_ReturnsNullWhenEventMemberLeftBeforeHandling`
  - Updated to assert invalid target-group side effects and the Java-derived message at the runtime boundary.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 deeper `CM_PLAYER_STATUS_INFO` / `ALLIANCE_CHANGE_GROUP` invalid target-group exception path plus 1 new helper and 2 regression assertions.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: full generic team-command dispatch, Java static alliance registry, Java event queue/lock comparison, Java group object identity/runtime comparison, encoded opcode/frame golden validation, packet capture comparison, packet processor exception/log comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Command `27` invalid-group behavior is closer to source, but live/runtime parity gaps remain.

## Remaining Risks

- The C# `AllianceGroupId = 0` representation is an approximation of Java's null `PlayerAllianceGroup` reference.
- Java exception type differs intentionally for invalid target groups; only message, mutation ordering, and no-send behavior are mirrored.
- C# still uses manual branch dispatch and a dedicated group-change service rather than a full `PlayerTeamCommandService` equivalent.
- Java static alliance registry, event queue, lock, group object identity, group removal/add ordering, object iteration ordering, and threading behavior remain source-derived only.
- Java packet bytes, encrypted opcode/frame validation, packet capture comparison, packet processor exception/log comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Begin a minimal league runtime bridge plan now that the `CM_PLAYER_STATUS_INFO` league command prerequisite gates for ids `29`, `30`, `31`, and `32` are bounded.
2. Or continue auditing remaining `CM_PLAYER_STATUS_INFO` alliance command edge cases for event ordering and no-send behavior before widening the league model.
3. Keep real league mutation fanout narrow: model league state, leader/alliance membership, and one command path with packet/no-send assertions before adding the full event surface.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EW-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
