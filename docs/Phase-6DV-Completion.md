# Phase 6DV Completion Handoff

Created: May 23, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6DU and covers Sessions 599-601.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceMemberInfoTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~GamePacketTests"`
  - Result: Passed, 147 tests.
- Latest full validation:
  - `dotnet test dotnetConversion\AionServer.slnx`
  - Result: Passed, 1245 tests.

## Recent Work Completed

### Session 599 - Alliance Reconnect Fanout Plan

- Source-read Java `PlayerAllianceEnteredEvent`, `PlayerConnectedEvent`, `PlayerDisconnectedEvent`, and `PlayerAllianceLeavedEvent`.
- Added `PlayerAllianceConnectedPlan`, `PlayerAlliancePacketIntent`, and `PlayerAlliancePacketIntentKind`.
- Added `PlayerAllianceConnectedPlanner`.
- Modeled Java `PlayerConnectedEvent` packet order:
  - `SM_ALLIANCE_INFO` to reconnecting player;
  - `SM_ALLIANCE_MEMBER_INFO(connectedMember, RECONNECT)` to reconnecting player;
  - for each other member, `SM_ALLIANCE_MEMBER_INFO(connectedMember, RECONNECT)`;
  - then `SM_ALLIANCE_MEMBER_INFO(member, RECONNECT)` back to reconnecting player.
- Commit: `aaa901af5 Add alliance reconnect fanout plan`

### Session 600 - Alliance Enter Fanout Plan

- Added `PlayerAllianceEnteredPlan` and `PlayerAllianceEnteredPlanner`.
- Added `SmSystemMessage.ForceEnteredForce()` (`1390263`) and `SmSystemMessage.ForceHeEnteredForce(string)` (`1400013`).
- Modeled Java `PlayerAllianceEnteredEvent` ordered fanout for invited and existing members:
  - invited player receives alliance info, entered-force message, and `JOIN` member-info;
  - existing members receive invited `JOIN`, he-entered-force message, alliance info;
  - invited player receives each existing member as `ENTER`.
- Kept brands, abyss-rank broadcast, add-player mutation, league broadcast, and sockets deferred as metadata.
- Commit: `a869eda87 Add alliance enter fanout plan`

### Session 601 - Alliance Disconnect Fanout Plan

- Added `PlayerAllianceDisconnectedPlan`, `PlayerAllianceDisconnectedPlanStatus`, and `PlayerAllianceDisconnectedPlanner`.
- Added `SmSystemMessage.ForceHeBecomeOffline(string)` (`1301019`).
- Modeled Java non-leader `PlayerDisconnectedEvent` fanout:
  - `STR_FORCE_HE_BECOME_OFFLINE`;
  - `SM_ALLIANCE_MEMBER_INFO(disconnectedMember, DISCONNECTED)`;
  - `SM_ALLIANCE_INFO`.
- Explicitly deferred leader disconnect composition, disband, and league broadcast branches.
- Commit: `861826acc Add alliance disconnect fanout plan`

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.alliance.events.PlayerConnectedEvent` | `Aion.GameServer.Services.PlayerAllianceConnectedPlanner` / `PlayerAllianceConnectedPlan` | Event Planning Bridge | Partial | Regression Tested | Needs Verification | Ordered reconnect packet fanout is modeled. Live member wrapper replacement, `alliance.removeMember/addMember`, sockets, and Java runtime ordering comparison remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceEnteredEvent` | `Aion.GameServer.Services.PlayerAllianceEnteredPlanner` / `PlayerAllianceEnteredPlan` | Event Planning Bridge | Partial | Regression Tested | Needs Verification | Ordered enter packet/system-message fanout is modeled after add-player. Live add-player mutation, brands, abyss-rank broadcast, league broadcast, sockets, and `super.handleEvent` remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` / `PlayerAllianceDisconnectedPlan` | Event Planning Bridge | Partial | Regression Tested | Needs Verification | Non-leader offline fanout is modeled. Leader disconnect, online-member tracking, disband, league broadcast, and sockets remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceInfo` via ordered packet intents | Server Packet | Partial | Regression Tested | Needs Verification | Non-league alliance-info packet bodies are reused across reconnect/enter/disconnect plans. League rows, Java golden bytes, encoded frames, and live sends remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ALLIANCE_MEMBER_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmAllianceMemberInfo` via ordered packet intents | Server Packet | Partial | Regression Tested | Needs Verification | `RECONNECT`, `JOIN`, `ENTER`, and `DISCONNECTED` planned fanout paths are packet-body tested. Live wrapper metadata, Java golden bytes, encoded frames, and client validation remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` enter/disconnect methods | `SmSystemMessage.ForceEnteredForce`, `ForceHeEnteredForce`, `ForceHeBecomeOffline` | Server Packet Factory | Partial | Unit Tested | Needs Verification | Java ids `1390263`, `1400013`, and `1301019` are modeled and recipient-planned. Java runtime frame comparison is missing. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | Source-read dependency; next target | Event Dependency | Not Started | No Tests | Unknown | Java leave/ban/timeout/disband fanout remains the next unit. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | Snapshot member inputs across planners | Team Runtime Dependency | Not Started | No Tests | Unknown | Full live alliance state remains missing: member wrappers, add/remove, online member collection, leader lookup, disband checks, league integration, and live iteration. |

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 7
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live alliance runtime, live member wrapper replacement, live add/remove member mutation, brands, abyss-rank broadcast, online-member tracking, disband, league broadcast/rows, leave fanout, Java runtime ordering comparison, encoded opcode/frame validation, and live client validation
- Estimated overall migration completion: 63%

The percentage stays conservative. Alliance reconnect/enter/non-leader disconnect packet ordering is now modeled, but leave/disband/league/live runtime behavior remains incomplete.

## Remaining Risks

- Live `PlayerAlliance` runtime and event dispatch are not implemented.
- Ordered packet intents are source-derived but not compared against a Java runtime or real client.
- `PlayerAllianceLeavedEvent` remains unported beyond source-reading.
- Brand fanout and `SM_ABYSS_RANK_UPDATE(1, player)` for alliance enter are metadata only.
- Leader disconnect composition with `ChangeAllianceLeaderEvent` remains deferred.
- Disband and league-broadcast side effects remain deferred.
- Java golden byte vectors and encoded-frame validation are unavailable.
- Real-client handling is unverified for these alliance fanout packets.

## Next Recommended Unit of Work

Continue alliance event fanout parity:

1. Source-read `PlayerLeavedEvent` base class and Java `SM_SYSTEM_MESSAGE` factories used by `PlayerAllianceLeavedEvent`.
2. Add a non-sending plan for non-disband `PlayerAllianceLeavedEvent` leave/ban/timeout fanout:
   - vice-captain id removal metadata;
   - reason-specific system message to remaining members;
   - `SM_ALLIANCE_MEMBER_INFO(leavedMember, LEAVE)`;
   - `SM_ALLIANCE_INFO(team)`;
   - ban-to-leaved-player message metadata if reason is `BAN`.
3. Keep actual `team.removeMember`, disband checks, league broadcast, timeout scheduling, and socket sends deferred unless safe runtime surfaces already exist.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6DU-Completion.md`
   - this handoff
3. Source-read `PlayerLeavedEvent` and the leave-related `SM_SYSTEM_MESSAGE` methods.
4. Implement one narrow leave fanout plan or packet slice.
5. Add tests.
6. Run focused tests, then full `dotnet test dotnetConversion\AionServer.slnx`.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
