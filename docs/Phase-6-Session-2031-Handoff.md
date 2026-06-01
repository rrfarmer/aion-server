# Phase 6 Session 2031 Handoff - Find Group Prepare Window Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2031
Status: Completed

## What Changed

- Extended Java `SM_FIND_GROUP` golden coverage to actions `23` and `24`.
- Extended C# `SmFindGroup` with prepare-window destroy/update writers.
- Added C# byte-for-byte tests for those two action payloads.

## Validation

- Focused Java `SM_FIND_GROUP` golden test passed with 9 test methods.
- Focused C# `SmFindGroup` test passed with 10 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 105 game-server tests.
- Broad C# game-server suite passed with 5142 tests.

## Known Gaps

- This unit proves only `SM_FIND_GROUP` actions `23` and `24` on top of previous slices for actions `1`, `5`, `11`, `14`, `18`, `22`, and `26`.
- Actions `0`, `4`, `10`, and `16` remain unported in C#.
- Action `23` true-flag payload is source-reviewed but not byte-golden tested through a normal Java constructor path.
- Action `24` is covered only for one deterministic offline member with admin name tags disabled.
- C# does not yet perform live find-group service calls, world broadcasts, encrypted frame handling, socket dispatch, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2031-Completion.md`
- `docs/Phase-6-Session-2031-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `SM_FIND_GROUP` action `16` member-info writer with a minimal member snapshot, using parsed/tolerant assertions for the `System.currentTimeMillis() / 1000` header unless a deterministic seam is found.

Safe alternative candidates:

- Inspect action `10` instance-group list with parsed timestamp-header assertions.
- Inspect actions `0`/`4` recruitment/application list writers with parsed timestamp-header assertions.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect a narrow non-live `FindGroupService` planner only if service calls, persistence, and world broadcasts remain explicitly deferred.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim full `SM_FIND_GROUP` parity from UOW-2027 through UOW-2031; only actions `1`, `5`, `11`, `14`, `18`, `22`, `23`, `24`, and `26` have objective byte evidence.
- Remaining `SM_FIND_GROUP` writer gaps are actions `0`, `4`, `10`, and `16`.
- Current-time header branches should not be asserted as exact fixed bytes unless the Java side can be made deterministic.
