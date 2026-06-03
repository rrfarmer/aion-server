# Phase 6 Session 2434 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2434 in-league alliance vice-captain promote-limit early-return coverage.

## Last Completed UOW

- UOW-2434 added a focused regression proving Java's `AssignViceCaptainEvent` promote-limit early return in an active league.
- When an alliance already has four vice captains, command code `25` now has explicit coverage that only `STR_FORCE_CANNOT_PROMOTE_MANAGER` is sent to the leader and no direct alliance-info or league broadcast packets are sent.
- No production code changed.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2434] Cover alliance promote-limit league early return`

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `docs/Phase-6-Session-2434-Completion.md`
- `docs/Phase-6-Session-2434-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AssignViceCaptainEvent.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Tests.GameServerConnectionPlayerStatusInfoTests`
- `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` reviewed
- `Aion.GameServer.Network.Aion.GameServerConnection` reviewed
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` reviewed

## What Changed

- Added `HandlePlayerStatusInfoAsync_AllianceViceCaptainPromoteLimitInLeagueReturnsBeforeFanoutLikeJava`.
- The test sets up a two-alliance league, fills four vice-captain slots, attempts to promote a fifth member, and asserts:
  - target is not promoted,
  - vice-captain ids are unchanged,
  - exactly one packet is sent,
  - the only recipient is the alliance leader,
  - the only packet is system message id `1301061`.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests" --no-restore`
- Result:
  - Passed: 65
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warning.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.events.AssignViceCaptainEvent` | `Aion.GameServer.Services.PlayerAllianceViceCaptainAssignmentPlanner` and `GameServerConnection.DispatchAllianceViceCaptainAssignmentAsync` | Event / Role Assignment | Partial | Regression Tested | Partial Parity | Promote-limit early return is now covered in and out of league. Offline event-player behavior remains only planner-covered and broader assignment behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet DTO / Factory | Partial | Regression Tested | Partial Parity | `STR_FORCE_CANNOT_PROMOTE_MANAGER` id `1301061` is asserted through the in-league command boundary. Broader system-message catalog remains partial. |

## Known Gaps

- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence timing.
- Remaining player alliance snapshots are not fully synchronized when league runtime removes/disbands a league.

## Remaining Risks

- No Java runtime fixture exists for this promote-limit branch; evidence is Java source review plus C# boundary regression.
- Full live client behavior for alliance command fanouts remains un-smoked.
- Direct in-league `SM_ALLIANCE_INFO` row construction is now covered for vice-captain-style fanout, but other direct packet constructors may still need review.

## Next Recommended UOW

UOW-2435: Audit manual/offline-timeout alliance disband behavior.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Safe fallback candidate:
- Audit direct in-league `SM_ALLIANCE_INFO` constructors outside vice-captain and leader-change command paths.

## Focused Validation Recipe

- For UOW-2435:
  - Specific behavior/contract:
    - Java alliance disband removes FindGroup recruitment, sends `AllianceDisbandEvent` side effects, removes alliance membership, and sends `LeagueLeftEvent` either before or after disband depending on caller's `onBefore` flag.
  - Focused C# command:
    - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
    - Narrow to the edited class if the scope stays test-only.
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java alliance disband test is found.
  - Broad-validation trigger:
    - live alliance/league command or logout dispatch if production send ordering changes.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2430 made alliance disconnected league broadcast and no-online after-disband league-left behavior live.
- UOW-2431 made logout fallback alliance leader-change league broadcast and timeout behavior live.
- UOW-2432 made manual in-league set-captain command dispatch live and caused `AssignViceCaptain` to carry league metadata.
- UOW-2433 made standalone in-league vice-captain command dispatch live and corrected direct in-league alliance-info packets to include league rows.
- UOW-2434 added in-league promote-limit early-return regression coverage without production changes.
