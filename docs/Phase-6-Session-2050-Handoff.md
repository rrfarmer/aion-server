# Phase 6 Session 2050 Handoff - Alliance Member Info Captain Role Goldens

Date: 2026-06-01
Unit of Work: UOW-2050
Status: Completed

## What Changed

- Added Java golden packet evidence for `SM_ALLIANCE_MEMBER_INFO.writeImpl` online `APPOINT_VICE_CAPTAIN`, `DEMOTE_VICE_CAPTAIN`, and `APPOINT_CAPTAIN`.
- Covered Java's shared wire id distinction: all three role events serialize event id `13`, member name, two zero dwords, full-slots byte `127`, zero effect count, and eight zero timer dwords.
- Added the matching C# exact payload test using explicit role event identities.
- Extended the C# same-wire-id identity test to show the role events exist as explicit identities while legacy numeric conversion for id `13` still maps to `Enter`.
- Kept the scope intentionally narrow to avoid live alliance role assignment, dispatch, recipient, and socket behavior until those systems have objective evidence.

## Validation

- Focused Java `SM_ALLIANCE_MEMBER_INFO_GoldenTest` passed with 7 test methods.
- Focused C# role-event, enter/update, and identity tests passed with 3 tests.
- Focused Java alliance/group/member/group-data goldens passed with 17 test methods.
- Full C# `PlayerAllianceMemberInfoTests` passed with 40 tests.
- Full scoped Maven reactor passed with 1 commons test and 124 game-server tests.
- First broad C# validation hit an unrelated `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` duplicate-broadcast assertion; the exact test passed on rerun, and the repeated broad C# validation passed with 5176 tests.

## Known Gaps

- No live captain/vice-captain assignment fanout, production alliance membership attachment, role mutation, production known-list/team recipient filtering, or socket frame ordering is enabled or proven.
- No real-client behavior or persistent Java known-list parity is proven.
- Java golden evidence for `SM_ALLIANCE_MEMBER_INFO` covers movement, online join, offline-enter name, targeted zero-effect `UPDATE_EFFECTS`, member-group-change name-only, online enter, online update, and captain/vice-captain zero-effect skeletons.
- `RECONNECT` zero-effect skeleton and non-empty effects still need Java-side vectors before broad packet-branch claims.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_ALLIANCE_MEMBER_INFO_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceMemberInfoTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2050-Completion.md`
- `docs/Phase-6-Session-2050-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add Java golden evidence for `SM_ALLIANCE_MEMBER_INFO` `RECONNECT` zero-effect skeleton or non-empty alliance effects if a controlled fixture is practical.

Safe alternative candidates:

- Inspect non-empty alliance effects with a controlled Java fixture.
- Inspect a narrow non-live `FindGroupService` planner.
- Inspect live alliance role assignment planning only if it can remain non-live and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Treat UOW-2040 through UOW-2044 as group-member packet-writer evidence and UOW-2045 through UOW-2050 as alliance-member packet-writer Java golden evidence.
- Do not claim live alliance runtime parity until production dispatch, known-list/team online filtering, socket encryption/frame order, Java online/offline lifecycle, effect-controller mutation, alliance role/group-slot mutation, and real-client behavior have objective coverage.
