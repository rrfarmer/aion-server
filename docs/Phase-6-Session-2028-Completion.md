# Phase 6 Session 2028 Completion - Find Group Window Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2028
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 completion and handoff before choosing work.
- Inspected Java `ServerWideGroup`, `GroupRecruitment`, `GroupApplication`, `Player`, and `AionObject`.
- Reviewed existing Java unsafe test setup used for packet golden vectors.
- Re-inspected the C# `SmFindGroup` writer from UOW-2027.

## What Changed

- Added Java golden vectors for `SM_FIND_GROUP` action `18` and action `22`.
- Added a narrow C# `FindGroupInstanceGroupWindowSnapshot` containing only group entry ID and instance mask ID.
- Added C# `SmFindGroup` factories and writer branches for:
  - action `18`: show enter button in prepare-for-entry window.
  - action `22`: show prepare-for-entry window.
- Added C# packet tests matching those Java-emitted payloads.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmFindGroupTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_FIND_GROUP` golden test passed with 5 test methods.
- Focused C# `SmFindGroup` test passed with 6 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 101 game-server tests.
- Broad C# game-server suite passed with 5138 tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/model/gameobjects/findGroup/ServerWideGroup.java`.
- Objective byte evidence now covers `SM_FIND_GROUP` actions `1`, `5`, `18`, `22`, and `26`.
- No verified live parity is claimed for `FindGroupService` dispatch, world broadcasts, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2028-Completion.md`
- `docs/Phase-6-Session-2028-Handoff.md`

## Remaining Risks

- `SM_FIND_GROUP` actions `0`, `4`, `10`, `11`, `14`, `16`, `23`, and `24` remain unported.
- The new C# snapshot intentionally covers only the fields needed by actions `18` and `22`.
- Branches that serialize recruitment/application lists, server-wide group members, readiness, names, levels, messages, and timestamps still need deterministic snapshot work.
- Live find-group service calls and packet fanout remain deferred.

## Next Recommended Unit

- Build deterministic snapshot records for `SM_FIND_GROUP` action `10` or action `14` if Java setup remains small, or inspect action `11` as a player-only writer vector.

Safe alternatives:

- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
- Inspect another compact registered parser/writer boundary with Java golden evidence.
- Inspect a narrow non-live `FindGroupService` planner only if service calls, persistence, and world broadcasts remain explicitly deferred.
