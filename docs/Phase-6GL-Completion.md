# Phase 6GL Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GK and covers Session 682.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionGroupInviteTests`
  - Result: Passed, 7 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1197 tests.

## Recent Work Completed

### Session 682 - CM_INVITE_TO_GROUP League Invite Type

- Source-read Java `LeagueService.inviteToLeague` and compared it with `PlayerLeagueInvitePlanner`.
- Extended `GameServerConnection.HandleInviteToGroupAsync` for invite type `28`.
- Added `HandleInviteToLeagueAsync`:
  - runs represented league can-invite checks,
  - redirects selected non-leader members to their alliance leader,
  - registers the pending request on the request target,
  - sends requester system messages,
  - sends `SmQuestionWindow.UnionInviteMe` to the request target.
- Invite type `12` for alliance invite remains deferred.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_INVITE_TO_GROUP` | `GameServerConnection.HandleInviteToGroupAsync` invite type `28` branch | Client Packet / Handler | Partial | Regression Tested | Needs Verification | Invite type `28` now reaches league invite setup. Invite type `12` remains deferred. |
| `com.aionemu.gameserver.model.team.league.LeagueService.inviteToLeague` | `GameServerConnection.HandleInviteToLeagueAsync` + `PlayerLeagueInvitePlanner` | Service / Request Starter | Partial | Unit Tested / Regression Tested | Needs Verification | Registers pending invite and sends requester/question packets. Full static league service parity remains broader work. |
| `com.aionemu.gameserver.model.team.league.LeagueService.canInvite` | `PlayerLeagueInvitePlanner.CreateCanInviteFirstChecksPlan` / `CreateCanInviteAllianceChecksPlan` | Restriction Planner | Partial | Regression Tested | Needs Verification | Reuses represented checks; no-alliance failure is covered in this unit. |
| `com.aionemu.gameserver.model.team.league.events.LeagueInviteEvent` | `PendingLeagueInviteRequest` via `PlayerLeagueInvitePlanner.TryPutPendingRequest` | Request Handler / Adapter Payload | Partial | Regression Tested | Needs Verification | Registered on redirected alliance leader; Java anonymous handler identity remains unported. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `QuestionResponseRegistry.PutRequest` via `TryPutPendingRequest` | Request Registry Method | Partial | Regression Tested | Needs Verification | Packet path invokes put-if-absent for league invite type `28`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_MSGBOX_UNION_INVITE_ME` | `SmQuestionWindow.UnionInviteMe` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Question id `902249` is sent to redirected alliance leader. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_INVITE_HIM` | `SmSystemMessage.UnionInviteHim` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Test asserts id `1400558`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_INVITE_HIS_LEADER` | `SmSystemMessage.UnionInviteHisLeader` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Test asserts id `1400559`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION` | `SmSystemMessage.UnionCantInviteWhenHeIsAskedQuestion` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Test asserts id `1400567`. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.PlayerAllianceRuntime` | Runtime Dependency | Partial | Regression Tested | Needs Verification | Used for selected-member to leader redirection. |
| `com.aionemu.gameserver.model.team.league.League` / static league map | `Aion.GameServer.Services.PlayerLeagueRuntime` | Runtime Dependency | Partial | Existing Regression Coverage | Needs Verification | Reused for can-invite and response handling. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` | Deferred from `CmInviteToGroup.InviteType == 12` | Service Dependency | Not Started | No Tests | Unknown | Remaining Java invite branch. |

## Tests Added Or Updated

- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_LeagueInviteTypeTargetsAllianceLeaderAndRegistersQuestion`
- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_LeagueInviteTypeRejectsPlayerWithoutAlliance`

These tests are source-derived from Java; they do not compare against Java runtime execution, golden bytes, encrypted frames, full alliance/league event behavior, invite type `12`, reflection callback behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 12
- Total artifacts ported or partially modeled in this handoff window: 1 league invite type `28` production packet wiring slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked/not-started artifacts: invite type `12` alliance wiring, full `LeagueService.canInvite` parity, Java static league map parity, duplicate-request ordering, generic anonymous handler callback execution, full alliance/league event comparison, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Invite type `12` alliance invite remains deferred.
- Full `LeagueService.canInvite` parity and every failure branch are not exhaustively covered.
- Existing C# league runtime is not a complete Java static league service replacement.
- Duplicate request ordering for non-leader redirect messages remains unverified.
- Generic Java `RequestResponseHandler` callback execution remains partial.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Finish the remaining `CM_INVITE_TO_GROUP` branch by modeling invite type `12` (`PlayerAllianceService.inviteToAlliance`) if the C# alliance runtime can support the request/response flow. Otherwise move to another production-reachable `ResponseRequester` handler with a small surface, such as duel request, craft-skill learn confirmation, or experience recovery dialog.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GK-Completion.md`
   - this handoff
3. Inspect the selected Java `ResponseRequester` user and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
