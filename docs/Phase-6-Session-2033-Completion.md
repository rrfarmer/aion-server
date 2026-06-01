# Phase 6 Session 2033 Completion - Find Group Instance List Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2033
Status: Completed

## Work Discovery

- Re-read the latest UOW-2032 handoff before choosing work.
- Confirmed the worktree was clean after UOW-2032.
- Inspected Java `SM_FIND_GROUP.writeImpl` action `10`.
- Compared Java action `10` with action `14` to avoid reusing the wrong unknown-field block.
- Reviewed current C# `SmFindGroup` writer and tests.

## What Changed

- Added Java parsed packet evidence for `SM_FIND_GROUP` action `10`, `showInstanceGroups`.
- Added C# `SmFindGroup.ShowInstanceGroups`.
- Reused `FindGroupInstanceGroupRegistrationSnapshot` for the group row fields shared with action `14`.
- Added a deterministic C# packet test for action `10` with an injected list timestamp.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmFindGroupTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_FIND_GROUP` golden test passed with 11 test methods.
- Focused C# `SmFindGroup` test passed with 12 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 107 game-server tests.
- Broad C# game-server suite passed with 5144 tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/model/gameobjects/findGroup/ServerWideGroup.java`.
- Objective packet evidence now covers `SM_FIND_GROUP` actions `1`, `5`, `10`, `11`, `14`, `16`, `18`, `22`, `23`, `24`, and `26`.
- Action `10` evidence is parsed/timestamp-bounded rather than exact full-payload byte equality because Java writes the current Unix-seconds value.
- No verified live parity is claimed for `FindGroupService` dispatch, world broadcasts, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2033-Completion.md`
- `docs/Phase-6-Session-2033-Handoff.md`

## Remaining Risks

- `SM_FIND_GROUP` actions `0` and `4` remain unported.
- Action `10` multi-group ordering and live team-backed member lookup remain unverified.
- The C# action `10` factory accepts a snapshot timestamp; no live C# caller yet derives the Java-style current-time list header.
- Live find-group service calls and packet fanout remain deferred.

## Next Recommended Unit

- Inspect `SM_FIND_GROUP` action `0` recruitment list with parsed timestamp-header assertions and a deterministic solo-player recruitment.

Safe alternatives:

- Inspect action `4` application list writer with parsed timestamp-header assertions.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect a narrow non-live `FindGroupService` planner only if service calls, persistence, and world broadcasts remain explicitly deferred.
