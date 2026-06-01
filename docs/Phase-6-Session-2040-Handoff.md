# Phase 6 Session 2040 Handoff - Group Member Info Movement Golden

Date: 2026-06-01
Unit of Work: UOW-2040
Status: Completed

## What Changed

- Added Java golden packet evidence for `SM_GROUP_MEMBER_INFO.writeImpl` movement prefix.
- Added the matching C# packet writer test for `SmGroupMemberInfo`.
- Confirmed Java `CM_GROUP_DATA_EXCHANGE.runImpl` live-routing tests would be brittle with the current test stack because `PacketSendUtility` is static and not currently mockable there.

## Validation

- Focused Java `SM_GROUP_MEMBER_INFO_GoldenTest` passed with 1 test method.
- Focused C# movement/branchless `SmGroupMemberInfo` tests passed with 2 tests.
- Full C# `PlayerGroupRuntimeTests` passed with 41 tests.
- Focused Java group/member/group-data goldens passed with 5 test methods.
- Full scoped Maven reactor passed with 1 commons test and 112 game-server tests.
- Broad C# validation passed with 5166 tests after a longer rerun; the first broad attempt timed out before returning a result.

## Known Gaps

- No live `CM_GROUP_DATA_EXCHANGE` production dispatch is enabled or proven.
- No socket encryption/frame ordering, real-client behavior, or persistent Java known-list parity is proven.
- `SM_GROUP_MEMBER_INFO` Java golden evidence covers only one movement prefix vector.
- Join, enter/offline, update, update-effects, non-empty abnormal effects, and slot-timer branches still need Java-side vectors before broad parity claims.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_GROUP_MEMBER_INFO_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2040-Completion.md`
- `docs/Phase-6-Session-2040-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add Java golden evidence for another `SM_GROUP_MEMBER_INFO` branch, preferably `JOIN` or `ENTER_OFFLINE`, then mirror the C# packet payload if useful.

Safe alternative candidates:

- Inspect `SM_ALLIANCE_MEMBER_INFO` movement/member prefix parity.
- Inspect a narrow non-live `FindGroupService` planner with service mutation and broadcast side effects deferred.
- Return to group-data known-list/team online filtering only if it can remain diagnostic and disabled.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Treat UOW-2036 as group-data packet-writer evidence, UOW-2037 as planner evidence, UOW-2038 as opt-in adapter boundary evidence, UOW-2039 as disabled connection-composition evidence, and UOW-2040 as adjacent group-member packet-writer evidence.
- Do not claim live `CM_GROUP_DATA_EXCHANGE` parity until production dispatch, known-list/team online filtering, socket encryption/frame order, Java online filtering, and real-client behavior have objective coverage.
