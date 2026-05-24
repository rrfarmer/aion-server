# Phase 6GM Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GL and covers Session 683.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter GameServerConnectionGroupInviteTests`
  - Result: Passed, 11 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1201 tests.

## Recent Work Completed

### Session 683 - CM_INVITE_TO_GROUP Alliance Invite Type

- Source-read Java `PlayerAllianceService.inviteToAlliance`, `PlayerAllianceInvite`, `PlayerRestrictions.canInviteToAlliance`, and related packet constants.
- Added `SmQuestionWindow.AllianceInvite = 70000`.
- Added alliance invite `SmSystemMessage` factories used by the Java request path.
- Added `PendingAllianceInviteRequest` and `QuestionResponseRequestKind.AllianceInvite`.
- Added `PlayerAllianceInviteRequestService` for represented invite setup, deny, and narrow solo accept.
- Wired `CM_INVITE_TO_GROUP` invite type `12` through `GameServerConnection.HandleInviteToAllianceAsync`.
- Wired `CM_QUESTION_RESPONSE` for alliance invite deny and solo accept.
- Added `PlayerEnterWorldService` deny cleanup for pending alliance invites.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_INVITE_TO_GROUP` | `GameServerConnection.HandleInviteToGroupAsync` invite type `12` branch | Client Packet / Handler | Partial | Regression Tested | Needs Verification | Type `12` is production-routed; live socket/client validation not run. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` | `PlayerAllianceInviteRequestService.SendInvite` / `GameServerConnection.HandleInviteToAllianceAsync` | Service / Request Starter | Partial | Regression Tested | Needs Verification | Setup, group-leader redirection, and question registration are modeled. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceInvite` | `PendingAllianceInviteRequest` / response handling | Request Handler | Partial | Regression Tested | Needs Verification | Deny and solo accept are modeled. Group merge accept remains deferred. |
| `com.aionemu.gameserver.restrictions.PlayerRestrictions.canInviteToAlliance` | `CreateRepresentedRestrictionMessage` | Restriction Utility | Partial | Regression Tested | Needs Verification | Only represented checks are covered. Prison/FFA/auto-instance/race/defence-force checks remain unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_PARTY_ALLIANCE_DO_YOU_ACCEPT_HIS_INVITATION` | `SmQuestionWindow.AllianceInvite` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Question id `70000` is asserted. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.createAlliance/addPlayer` | `PlayerAllianceRuntime.CreateAlliance` / `AddMember` / `CreateEnteredPlan` | Runtime Service | Partial | Regression Tested | Needs Verification | Narrow solo accept creates a represented C# alliance. Full Java event graph remains partial. |
| `com.aionemu.gameserver.model.team.group.PlayerGroup` | `PlayerGroupRuntime` | Runtime Dependency | Partial | Regression Tested | Needs Verification | Used for request redirection only; Java group removal/merge is not implemented. |

## Tests Added Or Updated

- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_AllianceInviteTypeRegistersQuestion`
- `GameServerConnectionGroupInviteTests.HandleInviteToGroupAsync_AllianceInviteTypeRedirectsSelectedGroupMemberToLeader`
- `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_AllianceInviteDenyClearsRequestAndRejectsInviter`
- `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_AllianceInviteAcceptCreatesAllianceForSoloPlayers`

These tests are source-derived from Java; they do not compare against Java runtime execution, golden bytes, encrypted frames, full group-to-alliance merge behavior, full restriction parity, reflection callback behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 13
- Total artifacts ported or partially modeled in this handoff window: 1 alliance invite type `12` production packet/request/response wiring slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 13
- Total blocked/not-started artifacts: group-to-alliance merge accept branch, full `PlayerRestrictions.canInviteToAlliance`, Java static alliance map/offline checker, `FindGroupService` join callbacks, generic anonymous handler callback execution, full alliance event graph comparison, real socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Java `PlayerAllianceInvite.acceptRequest` group merge behavior remains deferred.
- Full `PlayerRestrictions.canInviteToAlliance` parity is not complete.
- `PlayerAllianceService.createAlliance` static registry, offline checker, `FindGroupService`, and complete event fanout are not fully modeled.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Continue alliance invite parity by modeling the Java group-merge accept branch from `PlayerAllianceInvite.acceptRequest`: collect requester group members, collect invited group members, remove both groups through the represented group runtime, then create/add alliance members with ordered packet fanout. If that is too broad, move to another production-reachable `ResponseRequester` handler with a small surface, such as duel request, craft-skill learn confirmation, or experience recovery dialog.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GL-Completion.md`
   - this handoff
3. Inspect the selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
