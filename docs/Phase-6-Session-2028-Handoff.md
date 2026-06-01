# Phase 6 Session 2028 Handoff - Find Group Window Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2028
Status: Completed

## What Changed

- Extended Java `SM_FIND_GROUP` golden coverage to actions `18` and `22`.
- Extended C# `SmFindGroup` with a narrow `FindGroupInstanceGroupWindowSnapshot`.
- Added C# byte-for-byte tests for action `18` and action `22`.

## Validation

- Focused Java `SM_FIND_GROUP` golden test passed with 5 test methods.
- Focused C# `SmFindGroup` test passed with 6 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 101 game-server tests.
- Broad C# game-server suite passed with 5138 tests.

## Known Gaps

- This unit proves only `SM_FIND_GROUP` actions `18` and `22` on top of UOW-2027's actions `1`, `5`, and `26`.
- Actions `0`, `4`, `10`, `11`, `14`, `16`, `23`, and `24` remain unported in C#.
- The C# window snapshot does not model full `ServerWideGroup` behavior.
- C# does not yet perform live find-group service calls, world broadcasts, encrypted frame handling, socket dispatch, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2028-Completion.md`
- `docs/Phase-6-Session-2028-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: build deterministic snapshot records for `SM_FIND_GROUP` action `10` or action `14` if Java setup remains small, or inspect action `11` as a player-only writer vector.

Safe alternative candidates:

- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect another compact registered parser/writer boundary with Java golden evidence.
- Inspect a narrow non-live `FindGroupService` planner only if service calls, persistence, and world broadcasts remain explicitly deferred.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim full `SM_FIND_GROUP` parity from UOW-2027/UOW-2028; only actions `1`, `5`, `18`, `22`, and `26` have objective byte evidence.
- Action `11` may be the next smallest writer slice because it needs only player object ID, class ID, level, and name, but Java `Player` setup must be checked carefully before committing to that scope.
