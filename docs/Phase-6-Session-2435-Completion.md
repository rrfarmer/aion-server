# Phase 6 Session 2435 Completion

## Unit of Work

- UOW-2435: Audit manual/offline-timeout alliance disband league ordering.

## Status

- Documentation-only audit completed.
- No production code changed.
- No runtime tests were added in this UOW.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java` remove-recruitment usage was searched and spot-reviewed through alliance disband call sites.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeavedPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceLeaveWorkflowPlanner.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPlayerStatusInfoTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

## Findings

- Java `PlayerAllianceService.disband(alliance, onBefore)` removes find-group recruitment, captures the current league, emits `LeagueLeftEvent` before `AllianceDisbandEvent` when `onBefore == true`, removes the alliance descriptor, and emits `LeagueLeftEvent` after alliance removal when `onBefore == false`.
- Java manual command disband paths (`LEAVE`/`BAN`) can broadcast ordinary league alliance info to other alliances before `disband(true)`, while timeout-driven removal skips that ordinary broadcast and still uses the `onBefore` league-left ordering if the alliance should disband.
- Java `AllianceDisbandEvent` replays `PlayerAllianceLeavedEvent(..., DISBAND)` for remaining alliance members; the `DISBAND` reason sends the dispersed message and skips ordinary member/alliance info fanout.
- C# already has a modeled logout/no-online `disband(false)` path through `PlayerLeagueRuntime.RemoveAllianceAfterAllianceDisband(...)`, with existing coverage in `PlayerEnterWorldServiceTests`.
- C# already has non-league command-side two-member alliance disband coverage in `GameServerConnectionPlayerStatusInfoTests`.
- C# does not yet model command-side in-league `disband(true)` ordering. `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow(...)` removes the leaver, builds its leave workflow with `isInLeague: false`, and clears remaining members/alliance state before the command handler can emit a Java-equivalent `LeagueLeftEvent` with the leaving alliance still packet-addressable.
- `PlayerLeagueRuntime.BroadcastAllianceInfo(...)` currently supports skipped-player behavior, but not Java `League.broadcast(skippedAlliance)` semantics, where every other alliance receives alliance info with skipped-alliance rows blanked.

## Migration Parity Notes

| Java surface | C# surface | Status | Verification | Notes |
| --- | --- | --- | --- | --- |
| `PlayerAllianceService.disband` | `PlayerAllianceRuntime` and `GameServerConnection` | Partial | Manual audit | Logout/no-online `disband(false)` is modeled; command-side in-league `disband(true)` ordering remains a gap. |
| `PlayerAllianceLeavedEvent` | `PlayerAllianceLeavedPlanner` and `RemoveMemberWithLeaveWorkflow` | Partial | Manual audit | Non-league command disband has tests; in-league leave/ban/timeout disband fanout needs phased mutation support. |
| `AllianceDisbandEvent` | `PlayerAllianceLeavedPlanner` disband packet append and `PlayerAllianceRuntime` cleanup | Partial | Existing regression tests plus manual audit | Existing coverage covers non-league command disband and logout/no-online league disband, not command-side in-league disband. |
| `LeagueLeftEvent` | `PlayerLeagueRuntime.RemoveAlliance` and `RemoveAllianceAfterAllianceDisband` | Partial | Existing regression tests plus manual audit | Java `onBefore=true` command-side league-left ordering is not wired. |
| `League.broadcast(skippedAlliance)` | `PlayerLeagueRuntime.BroadcastAllianceInfo` | Partial | Existing regression tests plus manual audit | Skipped-player overload exists; skipped-alliance broadcast semantics are missing. |
| `FindGroupService.removeRecruitment` | `FindGroupRecruitmentPlanService` | Partial | Existing regression tests plus manual audit | Command disband already removes recruitment in C# cleanup path; next implementation should preserve that timing. |

## Validation

- Changed surface: documentation only.
- Focused C# tests: not run because no runtime code changed.
- Java/Maven: not run because no Java source or fixture changed and no targeted Java parity test was required.
- Broad .NET suite: not run because no broad trigger applied.
- Hygiene check: `git diff --check`.

