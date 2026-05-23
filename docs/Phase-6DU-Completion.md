# Phase 6DU Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DT and covers Sessions 596-598.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~GamePacketTests"`
  - Result: Passed, 141 tests.
- Latest full validation:
  - `dotnet test dotnetConversion\AionServer.slnx`
  - Result: Passed, 1239 tests.

## Recent Work Completed

### Session 596 - Alliance Vice-Captain Role Plan

- Source-read Java `SM_ALLIANCE_INFO`, `AssignViceCaptainEvent`, `ChangeAllianceLeaderEvent`, `PlayerAllianceService.changeViceCaptain`, `TeamType`, and `SM_SYSTEM_MESSAGE.STR_FORCE_CANNOT_PROMOTE_MANAGER`.
- Added alliance role-info DTOs and `PlayerAllianceViceCaptainAssignmentPlanner`.
- Modeled Java `PROMOTE`, `DEMOTE`, `DEMOTE_CAPTAIN_TO_VICECAPTAIN`, promote-limit, and missing/offline event-player branches as non-sending plans.
- Added `SmSystemMessage.ForceCannotPromoteManager()` with Java message id `1301061`.
- Commit: `3c289813e Add alliance vice captain role plan`

### Session 597 - Non-League Alliance Info Serializer

- Added `SmAllianceInfo` with opcode `245`.
- Serialized the non-league `SM_ALLIANCE_INFO.writeImpl` body from `PlayerAllianceInfoPacketPlan`.
- Added `PlayerAllianceInfoIntent.CreatePacket()`.
- Added tests for promote payload, message-id-zero empty-message behavior, and explicit league-row unsupported handling.
- Commit: `9e0f446fc Add alliance info packet serializer`

### Session 598 - Alliance Leader Change Plan

- Added `PlayerAllianceLeaderChangePlan` and `PlayerAllianceLeaderChangePlanner`.
- Modeled non-league Java `ChangeAllianceLeaderEvent.changeLeaderTo` output intents:
  - updated leader id in `SM_ALLIANCE_INFO`;
  - removal of the new leader from vice-captain ids;
  - `STR_FORCE_HE_IS_NEW_LEADER` (`1300998`) to other members when `eventPlayer != null`;
  - `STR_FORCE_YOU_BECOME_NEW_LEADER` (`1300999`) to the new leader;
  - skip of direct `SM_ALLIANCE_INFO` when league broadcast should handle it.
- Added `SmSystemMessage.ForceHeIsNewLeader(string)` and `SmSystemMessage.ForceYouBecomeNewLeader()`.
- Commit: `4352b2b9c Add alliance leader change plan`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` / `PlayerAllianceViceCaptainAssignmentPlan` | Event Planning Bridge | Partial | Regression Tested | Needs Verification | Branch outputs and post-event vice-captain snapshots are modeled. Live alliance mutation, event dispatch, permissions, socket fanout, and league broadcast execution remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` / `PlayerAllianceInfoPacketPlan` | Server Packet | Partial | Unit Tested | Needs Verification | Non-league packet bodies are serialized and tested. League rows, Java golden bytes, encoded frames, live sends, and client validation remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceLeaderChangePlanner` / `PlayerAllianceLeaderChangePlan` | Event Planning Bridge | Partial | Regression Tested | Needs Verification | Non-league output fanout is modeled. Live `changeLeader`, automatic fallback leader selection, league union messages, and event dispatch remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` leader/vice-captain methods | `SmSystemMessage.ForceCannotPromoteManager`, `ForceHeIsNewLeader`, `ForceYouBecomeNewLeader` | Server Packet Factory | Partial | Unit Tested | Needs Verification | Java ids `1301061`, `1300998`, and `1300999` are modeled and recipient-planned. Java runtime frame comparison is missing. |
| `com.aionemu.gameserver.model.team.TeamType` | `Aion.GameServer.Services.PlayerAllianceTeamType` | Enum / Packet Dependency | Partial | Unit Tested | Needs Verification | Alliance type/subtype values are source-modeled for packet plans. Full team-type lifecycle is not ported. |
| `com.aionemu.gameserver.model.team.league.League` | `PlayerAllianceLeaderChangePlan.WouldBroadcastLeague` and `SmAllianceInfo` league guard | Runtime / Packet Dependency | Not Started | Unit Tested as Metadata/Unsupported | Unknown | League broadcast is metadata only. League row serialization and union leader-change messages remain missing. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | Snapshot inputs across alliance role plans | Team Runtime Dependency | Not Started | No Tests | Unknown | Full live alliance state remains missing: member wrappers, group size semantics, leader mutation, vice-captain collection ownership, league integration, and live iteration. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 7
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live alliance runtime, live vice-captain mutation, live leader mutation, automatic fallback leader selection, league row serialization/broadcast, league union messages, service event invocation/permissions, live socket fanout, Java runtime ordering comparison, encoded opcode/frame validation, and live client validation
- Estimated overall migration completion: 63%

The percentage stays conservative. Alliance role packet planning and non-league `SM_ALLIANCE_INFO` serialization improved, but live alliance runtime behavior and Java/client validation remain incomplete.

## Remaining Risks

- Live `PlayerAlliance` runtime and event dispatch are not implemented.
- Vice-captain and leader mutations are modeled as post-event snapshots, not applied to live state.
- Automatic fallback leader selection from online vice-captains/next available player is not ported.
- League-specific `SM_ALLIANCE_INFO` rows, league broadcast execution, and union leader-change messages are missing.
- `PlayerAllianceService` permission checks, right-missing/not-in-force system messages, and event invocation remain incomplete.
- Live socket fanout and Java iteration/threading behavior are not runtime-compared.
- Java golden byte vectors and encoded-frame validation are unavailable.
- Real-client handling is unverified for these alliance role packets.

## Next Recommended Unit of Work

Continue alliance event parity:

1. Source-read Java `PlayerAllianceEnteredEvent`, `PlayerAllianceLeavedEvent`, `PlayerConnectedEvent`, and `PlayerDisconnectedEvent`.
2. Add a focused non-sending plan for alliance enter/connect/disconnect info fanout using the existing `SmAllianceInfo` and `SmAllianceMemberInfo` packet surfaces.
3. Model packet intent ordering carefully, especially where Java sends both `SM_ALLIANCE_INFO` and `SM_ALLIANCE_MEMBER_INFO`.
4. Keep live membership mutation, disband logic, timeout scheduling, and socket fanout deferred unless a safe runtime surface already exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6DT-Completion.md`
   - this handoff
3. Source-read Java alliance enter/leave/connect/disconnect event classes.
4. Implement one narrow event fanout plan or packet slice.
5. Add tests.
6. Run focused tests, then full `dotnet test dotnetConversion\AionServer.slnx`.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
