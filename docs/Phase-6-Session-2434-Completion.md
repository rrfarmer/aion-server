# Phase 6 Session 2434 Completion

Status: Phase 6 continues; UOW-2434 added in-league alliance vice-captain promote-limit early-return coverage.

## Scope

- UOW: UOW-2434 targeted promote-limit early-return parity coverage.
- Reviewed Java `AssignViceCaptainEvent` and `SM_SYSTEM_MESSAGE`.
- Confirmed the current C# planner already matched Java by returning before alliance-info and league broadcast when four vice captains already exist.
- Added focused in-league command coverage to prove the early return remains true with league metadata present.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
  - `PROMOTE` checks `team.getViceCaptainIds().size() == 4`, sends `STR_FORCE_CANNOT_PROMOTE_MANAGER()` to the alliance leader, and returns before direct `SM_ALLIANCE_INFO` fanout or `League.broadcast()`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_FORCE_CANNOT_PROMOTE_MANAGER()` is the Java warning packet for this early return.

## Implemented

- Added `HandlePlayerStatusInfoAsync_AllianceViceCaptainPromoteLimitInLeagueReturnsBeforeFanoutLikeJava`.
- No production code changed.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandlePlayerStatusInfoAsync_AllianceViceCaptainPromoteLimitInLeagueReturnsBeforeFanoutLikeJava` | Regression | Java `AssignViceCaptainEvent` and `SM_SYSTEM_MESSAGE` source review | In an active league, attempting to promote a fifth vice captain sends only `STR_FORCE_CANNOT_PROMOTE_MANAGER` to the alliance leader, does not mutate vice captains, and does not send direct alliance-info or league broadcast packets. | Focused C# command dispatch assertion over a single sent packet, message id `1301061`, unchanged vice-captain ids, and no other recipients. | No Java runtime fixture exists for this event branch; evidence is source review plus C# regression. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` and `GameServerConnection.DispatchAllianceViceCaptainAssignmentAsync` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Promote-limit early return is now covered in and out of league. Offline event-player behavior remains only planner-covered and broader assignment behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | `STR_FORCE_CANNOT_PROMOTE_MANAGER` id `1301061` is asserted through the in-league command boundary. Broader system-message catalog remains partial. |

## Validation Decision

- Changed surface: test-only command boundary coverage.
- Specific behavior/contract: Java `AssignViceCaptainEvent` with four existing vice captains sends only `STR_FORCE_CANNOT_PROMOTE_MANAGER` to the alliance leader and returns before direct alliance-info or league broadcast, even when the alliance is in a league.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 65 passed, 0 failed, 0 skipped.
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warning.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from direct source review. No targeted Java unit test for this command/event branch was found.
- Broad-validation trigger:
  - None; test-only coverage.
- Broad .NET decision:
  - Full project/solution validation skipped. The filtered command compiled affected dependencies and exercised the edited test class; no production code changed.
- Why this scope is sufficient:
  - The UOW only adds a regression for a single Java early-return branch. The test directly asserts the absence of fanout by requiring exactly one captured packet.

## Known Remaining Gaps

- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Summary Metrics

- Java artifacts reviewed: 2.
- C# artifacts reviewed: 3.
- Production files changed: 0.
- Test files changed: 1.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 2.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
