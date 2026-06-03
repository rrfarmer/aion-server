# Phase 6 Session 2435 Handoff

## Latest Completed UOW

- UOW-2435: Audit manual/offline-timeout alliance disband league ordering.
- Outcome: documentation-only parity audit; no production code changed.

## Required Reading for Next UOW

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`
- Latest `docs/Phase-6-Session-*-Completion.md`

Do not read or update `docs/PHASE-6-PROGRESS.md` unless explicitly requested or needed for targeted archaeology.

## Key Findings to Carry Forward

- Java `PlayerAllianceService.disband(alliance, true)` emits `LeagueLeftEvent` before `AllianceDisbandEvent` and before alliance descriptor removal.
- Java `PlayerAllianceService.disband(alliance, false)` emits `LeagueLeftEvent` after alliance descriptor removal; this is the logout/no-online shape already modeled in C#.
- Java `PlayerAllianceLeavedEvent`:
  - removes the leaver and sends ordinary alliance leave fanout;
  - for `LEAVE`/`BAN`, broadcasts league alliance info to other alliances before disband when the alliance remains in a league;
  - for `LEAVE_TIMEOUT`, skips the ordinary league broadcast;
  - calls `PlayerAllianceService.disband(team, true)` when the alliance should disband;
  - for `DISBAND`, sends dispersed messages to remaining alliance members and skips ordinary member/alliance info fanout.
- C# `PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow(...)` currently clears remaining members and alliance state before command handlers can emit Java-equivalent command-side `LeagueLeftEvent` fanout.
- C# `PlayerAllianceLeaveWorkflowPlanner` is currently invoked with `isInLeague: false` in this path, so direct leave packets do not use league-row alliance info semantics.
- C# `PlayerLeagueRuntime.BroadcastAllianceInfo(...)` has skipped-player behavior but lacks a skipped-alliance overload equivalent to Java `League.broadcast(skippedAlliance)`.

## Recommended Next UOW

UOW-2436: Implement command-side in-league `disband(true)` ordering in focused phases.

Suggested implementation shape:

- Add or adjust league broadcast support for skipped-alliance semantics, likely in `PlayerLeagueRuntime.BroadcastAllianceInfo(...)`.
- Split command-side alliance leave/disband mutation so the leaver removal, ordinary leave fanout, optional league broadcast, league-left event, remaining-member disband fanout, and final cleanup happen in Java order.
- Capture league id and leaving alliance identity before mutation.
- Preserve existing logout/no-online `disband(false)` behavior through `RemoveAllianceAfterAllianceDisband(...)`.
- Preserve ban ordering so `STR_FORCE_BAN_ME` remains after disband when Java sends it.
- Preserve timeout behavior: no ordinary `League.broadcast(team)`, but still use `disband(true)` if the alliance should disband.

Suggested tests:

- `HandlePlayerStatusInfoAsync_AllianceLeaveTwoMemberInLeagueDisbandsWithLeagueLeftBeforeDisbandLikeJava`
- `HandlePlayerStatusInfoAsync_AllianceBanTwoMemberInLeagueDisbandsWithLeagueLeftBeforeDisbandLikeJava`
- Add timeout coverage only if a narrow command/service path exists for the same runtime branch.

Suggested focused validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven is not expected unless Java source or fixtures change. Start with focused validation; run broader .NET only if the implementation touches shared league/alliance packet behavior enough to trigger it.

## Alternate Safe Slice

If UOW-2436 is too large for one pass, first audit direct in-league `SM_ALLIANCE_INFO` construction outside vice-captain and leader-change paths, then return to command-side `disband(true)` implementation.

