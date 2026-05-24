# Phase 6FP Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6FO and covers Session 660.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter PlayerLeagueInvitePlannerTests`
  - Result: Passed, 6 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1141 tests.

## Recent Work Completed

### Session 660 - League Invite First Can-Invite Checks

- Added `SmSystemMessage` factories for:
  - `STR_UNION_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION` (`1400567`),
  - `STR_UNION_OFFLINE_MEMBER` (`1400569`),
  - `STR_UNION_CANT_INVITE_WHEN_DEAD` (`1400570`).
- Added `PlayerLeagueInvitePlanner.CreateCanInviteFirstChecksPlan`.
- The planner preserves Java `LeagueService.canInvite` ordering for the first three failure branches:
  - inviter dead,
  - invited offline,
  - invited without alliance.
- The planner returns requester-targeted `PlayerAllianceSystemMessageIntent` failures and `PassedRepresentedChecks` only after the represented checks pass.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.league.LeagueService.canInvite` | `PlayerLeagueInvitePlanner.CreateCanInviteFirstChecksPlan` | Service / Validation Planner | Partial | Unit Tested | Needs Verification | First three failure branches are covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_CANT_INVITE_WHEN_DEAD` | `SmSystemMessage.UnionCantInviteWhenDead` | Server Packet Factory | Complete | Unit Tested | Needs Verification | Message id `1400570` covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_OFFLINE_MEMBER` | `SmSystemMessage.UnionOfflineMember` | Server Packet Factory | Complete | Unit Tested | Needs Verification | Message id `1400569` covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_UNION_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION` | `SmSystemMessage.UnionCantInviteWhenHeIsAskedQuestion` | Server Packet Factory | Complete | Unit Tested | Needs Verification | Message id `1400567` and invited name parameter covered. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model Dependency | Partial | Unit Tested | Needs Verification | Uses C# creature state, online flag, and alliance snapshot. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `PlayerAllianceSystemMessageIntent` | Runtime Dependency / Intent | Partial | Unit Tested | Needs Verification | C# still returns intents instead of sending live packets. |

## Tests Added

- `PlayerLeagueInvitePlannerTests.CreateCanInviteFirstChecksPlan_FollowsJavaFailureOrder`
- `PlayerLeagueInvitePlannerTests.CreateCanInviteFirstChecksPlan_PassesWhenRepresentedChecksPass`

These tests are source-derived. They do not compare against Java runtime execution, Java golden vectors, encrypted frames, packet captures, request-response runtime, or a live client.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 partial `canInvite` validation slice plus 3 system-message factories.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: Java golden byte validation, full `LeagueService.canInvite`, request-response transport, invite-to-leader redirection, create-league-on-accept, live `PacketSendUtility` wiring, and client validation.
- Estimated overall migration completion: 64%

## Remaining Risks

- Remaining `canInvite` branches are not ported: own-alliance invite, invited already in league, league full, same-league/other-union check, and Java null assumptions around inviter alliance.
- Question-window transport, invite-to-leader redirection, accept request, create-league-on-accept, and live `PacketSendUtility` wiring remain open.
- C# alliance membership is inferred through snapshots; Java live `PlayerAlliance` references and object identity are not runtime-compared.
- Packet-field coverage remains C# emitted-object validation only; Java golden bytes, encrypted frames, packet captures, and live-client validation remain unavailable.

## Next Recommended Unit of Work

Continue `LeagueService.canInvite` in Java order:

1. Own-alliance/self invite -> `STR_UNION_CANT_INVITE_SELF` (`1400568`).
2. Invited already in league -> `STR_UNION_ALREADY_MY_UNION` (`1400603`).
3. League full -> `STR_UNION_CANT_ADD_NEW_MEMBER` (`1400565`).
4. Keep same-league/other-union, question-window transport, invite-to-leader redirection, create-league-on-accept, and live packet sending as later units.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6FO-Completion.md`
   - this handoff
3. Inspect Java source for the remaining `LeagueService.canInvite` branches and relevant `SM_SYSTEM_MESSAGE` factories before touching C#.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Commit the unit.
