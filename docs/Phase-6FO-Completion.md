# Phase 6FO Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FN and covers Session 659.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter PlayerLeagueInvitePlannerTests`
  - Result: Passed, 4 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1139 tests.

## Recent Work Completed

### Session 659 - League Invite Accept Existing-League Branch

- Added `PlayerLeagueInvitePlanner.CreateAcceptExistingLeaguePlan`.
- The planner models the Java `LeagueInviteEvent.acceptRequest` path after `LeagueService.canInvite` succeeds and the requester already has a league.
- Successful existing-league accept reuses `PlayerLeagueRuntime.JoinAlliance`.
- Deferred/no-op statuses are explicit:
  - `RequesterLeagueMissing` for Java's create-league-on-accept branch,
  - `InvitedAlreadyInLeague` for Java's `if (!invited.isInLeague())` guard.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.league.events.LeagueInviteEvent.acceptRequest` | `PlayerLeagueInvitePlanner.CreateAcceptExistingLeaguePlan` | Request / Event Planner | Partial | Unit Tested | Needs Verification | Existing-league accept branch is covered; full request flow remains open. |
| `com.aionemu.gameserver.model.team.league.LeagueService.addAlliance` | `PlayerLeagueRuntime.JoinAlliance` | Service / Runtime Bridge | Partial | Unit Tested | Needs Verification | Successful accepted invite reuses join event fanout. |
| `com.aionemu.gameserver.model.team.league.events.LeagueJoinEvent` | `PlayerLeagueRuntime.JoinAlliance` / `PlayerLeagueJoinPlan` | Event Runtime Dependency | Partial | Unit Tested | Needs Verification | Exercised through the invite planner. |
| `com.aionemu.gameserver.model.team.league.LeagueService.createLeague` | Deferred branch represented by `RequesterLeagueMissing` | Service Dependency | Not Started | Unit Tested | Unknown | Java creates the league here; C# reports the branch only. |
| `com.aionemu.gameserver.model.team.league.LeagueService.canInvite` | Deferred precondition outside `CreateAcceptExistingLeaguePlan` | Service Dependency | Not Started | No Tests | Unknown | Planner assumes validation has already passed. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `PlayerAllianceRuntime` | Team State Dependency | Partial | Unit Tested | Needs Verification | Supplies requester/invited alliance state. |

## Tests Added

- `PlayerLeagueInvitePlannerTests.CreateAcceptExistingLeaguePlan_JoinsInvitedAllianceLikeJavaAcceptRequest`
- `PlayerLeagueInvitePlannerTests.CreateAcceptExistingLeaguePlan_ReportsDeferredOrNoopBranches`

These tests are source-derived from Java accept-request control flow. They do not compare against Java runtime execution, Java golden vectors, encrypted frames, packet captures, request-response runtime, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 accepted existing-league invite planner slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueInviteEvent`, `LeagueService.canInvite`, create-league-on-accept, request-response transport, Java static league registry, and client validation.
- Estimated overall migration completion: 64%

## Remaining Risks

- `LeagueService.canInvite` is not ported; failure ordering and system-message fanout are still open.
- Request-response question-window registration and invocation are not wired.
- Requester-without-league create-on-accept is represented as a deferred status, not implemented.
- Invite-to-leader redirection in `LeagueService.inviteToLeague` remains open.
- Packet-field coverage for reused join fanout remains C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.

## Next Recommended Unit of Work

Start `LeagueService.canInvite` as a source-derived validation planner:

1. Add the first failure branches in Java order:
   - inviter dead -> `STR_UNION_CANT_INVITE_WHEN_DEAD` (`1400570`),
   - invited offline -> `STR_UNION_OFFLINE_MEMBER` (`1400569`),
   - invited without alliance -> `STR_UNION_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(invitedName)` (`1400567`).
2. Return a status plus requester-targeted system-message intent.
3. Keep question-window transport, invite-to-leader redirection, full validation matrix, and create-league-on-accept separate.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FN-Completion.md`
   - this handoff
3. Inspect Java source for `LeagueService.canInvite` and relevant `SM_SYSTEM_MESSAGE` factories before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
