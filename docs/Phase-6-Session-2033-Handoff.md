# Phase 6 Session 2033 Handoff - Find Group Instance List Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2033
Status: Completed

## What Changed

- Extended Java `SM_FIND_GROUP` parsed packet coverage to action `10`.
- Extended C# `SmFindGroup` with the instance-group list writer.
- Added a C# deterministic packet test for the action `10` layout.

## Validation

- Focused Java `SM_FIND_GROUP` golden test passed with 11 test methods.
- Focused C# `SmFindGroup` test passed with 12 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 107 game-server tests.
- Broad C# game-server suite passed with 5144 tests.

## Known Gaps

- This unit proves only `SM_FIND_GROUP` action `10` on top of previous slices for actions `1`, `5`, `11`, `14`, `16`, `18`, `22`, `23`, `24`, and `26`.
- Actions `0` and `4` remain unported in C#.
- Action `10` Java evidence is parsed/timestamp-bounded; it is not a full exact byte vector because Java writes `System.currentTimeMillis() / 1000`.
- Multi-group list ordering, live team-backed member lookup, race filtering, encrypted frame handling, socket dispatch, and real-client behavior remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2033-Completion.md`
- `docs/Phase-6-Session-2033-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `SM_FIND_GROUP` action `0` recruitment list with parsed timestamp-header assertions and a deterministic solo-player recruitment.

Safe alternative candidates:

- Inspect action `4` application list writer with parsed timestamp-header assertions.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect a narrow non-live `FindGroupService` planner only if service calls, persistence, and world broadcasts remain explicitly deferred.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim full `SM_FIND_GROUP` parity from UOW-2027 through UOW-2033; only actions `1`, `5`, `10`, `11`, `14`, `16`, `18`, `22`, `23`, `24`, and `26` have objective packet evidence.
- Remaining `SM_FIND_GROUP` writer gaps are actions `0` and `4`.
- Current-time header branches should use parsed/tolerant Java assertions unless the Java side can be made deterministic.
