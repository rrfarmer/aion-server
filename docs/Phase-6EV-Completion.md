# Phase 6EV Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6EU and covers Session 640.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerStatusInfo|PlayerAllianceRuntime|PlayerAllianceMemberInfo|BaseLeavePlanner"`
  - Result: Passed, 99 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1113 tests.

## Recent Work Completed

### Session 640 - Invalid Team Member Boundary

- Source-read Java `PlayerTeamCommandService.findMember` and the group/alliance command dispatches that call it.
- Aligned C# invalid target/member behavior for existing non-league `CM_PLAYER_STATUS_INFO` group/alliance commands that dispatch through Java `findMember`: group ban, group set leader, group remove, alliance ban, alliance set captain, alliance set vice captain, and alliance unset vice captain.
- Added `CreateInvalidTeamMemberException` using the existing Java-shaped player formatter so C# throws the Java-derived message instead of silently no-oping when a selected member id is not in the active team.
- Added parsed-command regression coverage for invalid member ids across group command ids `2`, `3`, and `6`, confirming no packets are sent and group membership remains unchanged.
- Added parsed-command regression coverage for invalid member ids across alliance command ids `16`, `17`, `25`, and `26`, confirming no packets are sent and alliance membership remains unchanged.
- Intentional C# difference: Java throws `NullPointerException` via `Objects.requireNonNull`; C# throws `InvalidOperationException` with the same Java-shaped message because the local packet-handler boundary uses C# exception idioms.
- Commit: this handoff is included in `Gate invalid team member commands`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PLAYER_STATUS_INFO` | `Aion.GameServer.Network.Aion.ClientPackets.CmPlayerStatusInfo` / `Aion.GameServer.Network.Aion.GameServerConnection.HandlePlayerStatusInfoAsync` | Client Packet / Handler Boundary | Partial | Regression Tested | Needs Verification | Invalid selected member ids throw for command ids `2`, `3`, `6`, `16`, `17`, `25`, and `26` instead of silently no-oping. Real packet processor exception/log behavior remains unverified. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService.findMember` | `GameServerConnection.CreateInvalidTeamMemberException` plus per-command membership checks | Service Helper | Partial | Regression Tested | Intentional Difference | Java throws `NullPointerException` from `Objects.requireNonNull`; C# throws `InvalidOperationException` with the same message. Selected id `0` still maps to the active player before lookup, matching Java. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamCommandService` | Existing `GameServerConnection.HandlePlayerStatusInfoAsync` branch dispatch | Service Dependency | Partial | Regression Tested | Needs Verification | C# still uses manual branch dispatch, but the currently ported `findMember` commands share the Java invalid-member boundary. Full generic service dispatch remains unported. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_BAN_MEMBER` | Command code `2` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id fails before ban self/leader/auto-group checks, matching Java `findMember` ordering. Functional ban behavior remains covered separately but not runtime-compared. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_SET_LEADER` | Command code `3` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id throws Java-shaped message. Existing leader-change packet behavior remains source-derived. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.GROUP_REMOVE_MEMBER` | Command code `6` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id throws Java-shaped message. Remove/disband behavior remains covered separately but not runtime-compared. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_BAN_MEMBER` | Command code `16` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id fails before ban self/leader/auto-alliance checks, matching Java `findMember` ordering. Warning-log behavior for target outside alliance after lookup remains deferred. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_SET_CAPTAIN` | Command code `17` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id throws Java-shaped message. Real alliance leader-change behavior remains source-derived and not runtime-compared. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_SET_VICECAPTAIN` | Command code `25` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id throws Java-shaped message before vice-captain assignment logic. Full Java assignment event ordering remains unverified. |
| `com.aionemu.gameserver.model.team.common.events.TeamCommand.ALLIANCE_UNSET_VICECAPTAIN` | Command code `26` branch in `GameServerConnection.HandlePlayerStatusInfoAsync` | Enum / Command Mapping | Partial | Regression Tested | Needs Verification | Invalid target id throws Java-shaped message before vice-captain demotion logic. Full Java assignment event ordering remains unverified. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService` | `GameServerConnection` group command branches plus `PlayerGroupRuntime` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Invalid target ids fail before service side effects. Java static group registry, event queue, lock, and exception propagation are not runtime-compared. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` | `GameServerConnection` alliance command branches plus `PlayerAllianceRuntime` | Service / Runtime Bridge | Partial | Regression Tested | Needs Verification | Invalid target ids fail before service side effects. Java static alliance registry, event queue, lock, Vortex cleanup, and league side effects are not runtime-compared. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.toString` | `GameServerConnection.FormatJavaPlayer` | Utility / Diagnostic Formatting | Partial | Regression Tested | Needs Verification | Existing formatter is reused for invalid member exception messages. Other Java `Player.toString` use sites are not audited. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `IGameClientConnectionRegistry` / no-send assertions | Runtime Dependency | Partial | Regression Tested | Needs Verification | Regressions confirm invalid member failures send no packets. Live packet processor exception handling/logging remains unverified. |

## Tests Added

- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_GroupFindMemberCommandsInvalidTargetThrowLikeJava`
  - Validates command ids `2`, `3`, and `6` with selected member id `1999` throw `InvalidOperationException("Player [id=1001, name=Leader] tried to execute team command on non-existent member with ID 1999")`, preserve group membership, and send no packets.
- `GameServerConnectionPlayerStatusInfoTests.HandlePlayerStatusInfoAsync_AllianceFindMemberCommandsInvalidTargetThrowLikeJava`
  - Validates command ids `16`, `17`, `25`, and `26` with selected member id `1999` throw the same Java-shaped message, preserve alliance membership, and send no packets.
- These tests are source-derived. They do not compare against Java runtime execution, Java-generated golden vectors, packet captures, encrypted frames, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 14
- Total artifacts ported or partially modeled in this handoff window: 1 shared invalid-member prerequisite boundary across 7 existing `CM_PLAYER_STATUS_INFO` command branches plus 2 regression test methods covering 7 command ids.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked/not-started artifacts: full generic team-command dispatch, Java packet processor exception/log comparison, Java static team registries, Java event queue/lock comparison, Vortex/league side effects, EventService/InstanceService callbacks, encoded opcode/frame golden validation, packet capture comparison, and real-client validation.
- Estimated overall migration completion: 64%

The percentage stays conservative. Invalid target/member behavior is tighter for ported team commands, but real runtime parity remains source-derived.

## Remaining Risks

- C# still uses manual branch dispatch rather than a full `PlayerTeamCommandService` equivalent.
- Java exception type differs intentionally for invalid member ids; only the message and no-send/no-mutation behavior are mirrored.
- Java packet processor exception handling/logging for these branches has not been runtime-compared.
- Java static group/alliance registries, event queues, locks, object iteration ordering, offline-recipient behavior, Vortex cleanup, league side effects, EventService, and InstanceService callbacks remain source-derived or deferred.
- Java golden byte vectors, encrypted opcode/frame validation, packet capture comparison, and real-client validation remain unavailable.

## Next Recommended Unit of Work

Continue Phase 6 in this order:

1. Continue the non-league `CM_PLAYER_STATUS_INFO` audit by checking invalid-member and no-op boundaries for `ALLIANCE_CHANGE_GROUP` command `27`, whose C# path uses a dedicated group-change service rather than Java `findMember`.
2. Or begin planning a minimal league runtime bridge now that the league command prerequisite gates are bounded.
3. Keep real league mutations out until a focused league model and event fanout plan are in place.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6EU-Completion.md`
   - this handoff
3. Inspect Java source for the selected branch before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
